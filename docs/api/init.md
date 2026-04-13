# `POST /init` — 数据区与运行态初始化

## 推荐引导顺序

1. **`GET /init/status`**：若 `initialized` 为 `false` 且 `credentialsConfigured` 为 `false`，请用户填写 AccessKey。  
2. **`POST /init/credentials`**：提交密钥（非调试写入 SQLite，调试仅内存）。详见 [init-credentials.md](./init-credentials.md)。  
3. **`POST /init`**（本文）：准备 `data/`、SQLite，并在非调试模式下将 `initialization_completed` 置为 `true`。

若在未配置有效 AccessKey 时调用 **`POST /init`**，返回 **428**，体 `{"error":"need_credentials"}`。

---

| 项目 | 说明 |
|------|------|
| 作用 | 在首次部署或需要重置时，准备 **`data/`** 与 SQLite 库文件；**前提**为已通过 **`POST /init/credentials`**（或调试模式下内存）配置密钥。非调试时在成功后写回 **`config/json/runtime.json`** 的 `initialization_completed`。 |
| 方法与路径 | `POST /init`（**非** `/api` 前缀）。 |
| 请求 | **无**路径变量、**无**请求体；附带查询串**不参与**语义。 |
| 前置条件 | 由 `AliyunCredentialInitBridge` 校验：非调试须 SQLite 中存在有效 AccessKey；调试须内存中已提交非空密钥。不满足则 **428** `need_credentials`（**不**进入「初始化中」门禁）。 |
| 已初始化分支 | 若 `runtime.json` 中 `initialization_completed` 已为 **`true`**：**不再**操作 `data/` 与库文件，也**不**改 JSON；响应 `skipped_already_initialized`。 |
| 未初始化分支 | 取得与本次 init 一致的 **`debug_mode` 快照**后进入执行区间。处理 **`data/`**（与 `spring.datasource.url` 中 `jdbc:sqlite:data/desubtitle.db` 一致）：若目录**不存在** → 创建 → **标准初始化**；若目录**已存在** → Hikari **soft evict** → **删除目录内除 `desubtitle.db` 以外的所有条目**（保留 `data/` 目录与主库文件，以免清空已存 AccessKey）→ 再 evict → **标准初始化**。 |
| 标准初始化 | 通过当前 **`DataSource`** 执行 `SELECT 1`；随后 **`runPostInitPlaceholder()`**（当前为空）。 |
| 写回 `runtime.json` | 非 **`debug_mode`**：将 `initialization_completed` 写为 **`true`**；调试模式：**不修改** `runtime.json`。 |
| 成功响应 | **200 OK**，体为 `InitResponse`（见下表）。 |
| 互斥 | 全进程**至多一个** `POST /init` 独占段；并发失败 **409** `init_in_progress`（见下）。 |
| 失败响应 | **428** 缺密钥；**409** 互斥；I/O **500**；其它未捕获异常由 Spring 处理为 5xx。 |
| 实现类 | `InitController`、`InitService`、`AliyunCredentialInitBridge`、`CredentialInitPrecondition` |
| 回归测试 | `InitControllerTest`、`InitServiceTest`、`InitExecutionMutexTest`、`InitializationGateInterceptorTest` |
| 访问门禁 | 未完成初始化时，除下文路径外，其它 Spring MVC 控制器 **503**（`not_initialized` / `initializing`）。静态资源不受影响。白名单与 **`MvcPublicEndpointRules#allowsWithoutInitialization`** 一致：`GET /life`、`GET /getAgreement`、`GET /getUserToken`、`GET /init/status`、`POST /init/credentials`、`POST /init`。 |
| Bearer JWT | 初始化完成后，**全局拦截器**对 **`GET /getUserToken`**、**init 三接口**、**`GET /life`** 不要求 Bearer；其中 **`GET /life`** 仍须在头中附带 **`Authorization: Bearer <JWT>`**（由控制器自处理，见 [life.md](./life.md)）。**`GET /getAgreement`** 等须由拦截器校验 Bearer（见 [bearer-user-auth.md](./bearer-user-auth.md)）。 |

## 响应体字段（`InitResponse`，HTTP 200）

| 字段 | 类型 | 说明 |
|------|------|------|
| `status` | string | `skipped_already_initialized` / `completed`。 |
| `executed` | boolean | 是否实际跑过初始化逻辑（跳过时为 `false`）。 |
| `debugMode` | boolean | 本次 init 使用的 `debug_mode` 快照。 |
| `initializationFlagWritten` | boolean | 非调试下是否写入了 `initialization_completed=true`。 |

## 并发冲突（HTTP 409）

| 字段 | 说明 |
|------|------|
| `error` | `init_in_progress` |

## 缺少密钥（HTTP 428）

| 字段 | 说明 |
|------|------|
| `error` | `need_credentials` |

总约定见 [backend-http-api.md](./backend-http-api.md)；索引见 [README.md](./README.md)；密钥存储见 [../reference/aliyun-credentials-storage.md](../reference/aliyun-credentials-storage.md)。
