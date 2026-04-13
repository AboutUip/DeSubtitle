# `POST /init/credentials` — 提交阿里云 AccessKey

| 项目 | 说明 |
|------|------|
| 作用 | 用户在引导页填入 **AccessKey ID** 与 **AccessKey Secret** 后提交；非调试模式写入 SQLite（经 **`SqliteConcurrencyController`** 临界区），调试模式仅写入进程内存。详见 [../architectuure/sqlite-access.md](../architectuure/sqlite-access.md)。 |
| 方法与路径 | `POST /init/credentials` |
| 请求体 | JSON：`accessKeyId`、`accessKeySecret`（字符串，去首尾空白后均不得为空）。 |
| 成功响应 | **200 OK**，体见下表。 |
| 校验失败 | **400**，由 Spring 默认错误体或 `ResponseStatusException` 呈现（`access_key_empty`）。 |
| 门禁 | 未完成初始化时仍允许访问（白名单）。 |

## 成功响应字段（`CredentialStoreResponse`）

| 字段 | 类型 | 说明 |
|------|------|------|
| `stored` | boolean | 固定 `true`：已接受本次提交。 |
| `persisted` | boolean | `true` 表示已写入 SQLite；**调试模式**下为 `false`（仅内存）。 |

## 安全与合规

明文存储与部署责任见 [../reference/aliyun-credentials-storage.md](../reference/aliyun-credentials-storage.md)。生产环境务必 **HTTPS**。

提交后须再调用 **`POST /init`** 完成数据区与完成标记（调试模式下仍不写 `initialization_completed`），见 [init.md](./init.md)。
