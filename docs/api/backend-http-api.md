# 后端 HTTP 与 REST 约定

本文描述当前 Spring Boot 应用**实际提供的 HTTP 行为**（以仓库内源码为准），并约定后续 REST 文档的写法。监听地址与端口由启动配置决定（例如 `server.port`，可由 `config/lua/ports.lua` 经 `StartupLuaPorts` 注入）；下文路径均为**相对服务根路径**的 URI。

**按路径拆分的 API 说明**：每个由 Controller 暴露的 HTTP 端点对应 **`docs/api/` 下独立 Markdown**（如 [life.md](./life.md)、[get-agreement.md](./get-agreement.md)、[get-indicator.md](./get-indicator.md)、[get-user-token.md](./get-user-token.md)、[bearer-user-auth.md](./bearer-user-auth.md)、[init-status.md](./init-status.md)、[init-credentials.md](./init-credentials.md)、[init.md](./init.md)），本文不再重复其字段级说明。

---

## 1. 静态资源与单页应用（`WebDirectoryResourceConfig`）

### 1.1 作用

- 在**同一进程、同一端口**上托管仓库根目录下的 `web/` 文件夹，使浏览器无需另启静态服务器即可加载 `index.html`、脚本、样式与图标等。
- 配合前端路由：对**看起来像「前端路由」的路径**（URL 最后一段不含 `.`）在磁盘上无对应文件时，**返回 `web/index.html`**，由前端脚本解析路径并渲染，避免刷新 404。

### 1.2 资源映射

| 项 | 说明 |
|----|------|
| 磁盘根目录 | 进程**当前工作目录**下的 `web/`（绝对路径规范化后作为 `file:` 资源根）。 |
| URL 映射 | 除下文特殊规则外，`GET`（及 Spring 对静态资源支持的同类请求，如 `HEAD`）`/**` 映射到上述目录内**同名相对路径**的文件。 |
| 示例 | `GET /index.html` → `web/index.html`；`GET /scripts/app.js` → `web/scripts/app.js`（路径以仓库实际文件为准）。 |

### 1.3 根路径 `GET /`

| 项目 | 说明 |
|------|------|
| 作用 | 将访问站点根的客户端引导到单页入口。 |
| 支持参数 | **无**查询参数被本配置单独消费；多余查询串行为以 Spring 默认为准（重定向目标通常为 `/index.html`，是否附带 query 以实现为准）。 |
| 实际处理 | 注册视图控制器：**重定向**至 `/index.html`（HTTP **302 Found**，`Location: /index.html`）。客户端若**跟随重定向**，则再发起 `GET /index.html`，得到 HTML 正文。 |
| 成功响应 | 首次响应：**302**，正文通常较短；跟随重定向后：**200**，`Content-Type` 一般为 `text/html`，正文为 `index.html` 内容。 |
| 失败情形 | 若 `web/index.html` 不存在，跟随重定向后静态解析失败，可能为 **404**（见 1.5）。 |

### 1.4 一般路径 `GET /{path}`（含子路径）

`{path}` 为 0 或多个路径段（如 `icons/logo.svg`、`any/spa/route`）。

**分支一：磁盘存在对应文件**

| 项目 | 说明 |
|------|------|
| 作用 | 返回静态文件内容。 |
| 支持参数 | **无**必填查询参数；是否使用查询参数不影响是否命中文件（由路径决定）。 |
| 成功响应 | **200**；`Content-Type`、`Cache-Control` 等由 Spring 资源处理器根据后缀推断。 |
| 正文 | 文件字节流（文本或二进制）。 |

**分支二：磁盘无文件，且触发 SPA 回退**

由内部 `SpaIndexFallbackResolver` 判断：

- 取路径**最后一段**（最后一个 `/` 之后；无 `/` 则整段为最后一段）。
- 若最后一段中**不包含字符 `.`**，则视为「可能为前端路由」，尝试返回 `web/index.html`。

| 项目 | 说明 |
|------|------|
| 作用 | 避免用户在前端路由上刷新或直接打开深链时，服务端因无实体文件而 404。 |
| 条件小结 | 文件不存在 **且** URL 最后一段不含 `.`。 |
| 典型路径 | `/any/spa/route`、仅一段且无点的 `/dashboard`；空路径在解析器中与「需回退」逻辑另有配合（根路径已由 `/` 重定向处理）。 |
| 成功响应 | **200**，正文为 **`index.html`** 全文（与直接访问 `/index.html` 一致）。 |
| 不支持参数 | 无专门 API 参数；查询串会原样进入请求，但不改变上述分支判定。 |

**分支三：磁盘无文件，且不满足 SPA 回退**

- 最后一段**包含 `.`**（如 `missing.js`、`a.png`），推断为「请求具体静态资源扩展名」，**不回退**到 `index.html`。

| 项目 | 说明 |
|------|------|
| 实际处理 | 资源解析返回无资源 → Spring 按未映射静态资源处理。 |
| 响应 | 通常为 **404 Not Found**（无 JSON 业务体，除非全局异常处理另行包装）。 |

### 1.5 启动期失败（无 `web/` 目录）

| 情形 | 说明 |
|------|------|
| 条件 | 构造 `WebDirectoryResourceConfig` 时，当前工作目录下**不存在**名为 `web` 的目录。 |
| 结果 | 抛出 **`IllegalStateException`**（消息指明静态资源目录不存在），应用**无法完成正常启动**，因而**无任何 HTTP 端点可用**。 |
| 处理建议 | 在**模块/仓库根目录**启动进程，或保证工作目录下存在 `web/` 且含至少 `index.html`（若需 SPA 回退）。 |

---

## 2. JSON REST API（前缀 `/api/**`）

### 2.1 当前状态

- 仓库 **尚未** 注册映射到 **`/api/**`** 的业务接口；存活探测 **`GET /life`** 有独立文档 [life.md](./life.md)。
- 除 [bearer-user-auth.md](./bearer-user-auth.md) 所列**全局拦截器不要求 Bearer** 的路径（**`GET /getUserToken`**、**init 三接口**、**`GET /life`**、错误控制器）外，其它 **MVC 控制器**在通过初始化门禁后均须由拦截器校验 **`Authorization: Bearer <JWT>`**（JWT 来自 [get-user-token.md](./get-user-token.md)）。**`GET /life`** 虽免拦截器，但**仍须**在头中附带 Bearer，由 [life.md](./life.md) 约定。**`GET /getAgreement`**、**`GET /getIndicator`** 须走拦截器校验。
- 架构约定：未来业务接口应放在 **`/api/**`** 下，避免与前端路由及静态文件名冲突（见 `WebDirectoryResourceConfig` 类注释与 [../architectuure/runtime-boundaries.md](../architectuure/runtime-boundaries.md)）。

### 2.2 新增 REST 端点时文档应写清的内容

每增加一个后端 API，须在 `docs/api/` 下**新增独立文件**（路径与文件名建议在 [README.md](./README.md) 索引中登记），正文至少包含：

| 章节 | 内容 |
|------|------|
| 作用 | 该接口解决的业务问题；与前端或其它服务的协作关系。 |
| 方法与路径 | 如 `POST /api/...`；是否要求认证 / 会话（若将来引入）。 |
| 请求 | 支持的 **Content-Type**（如 `application/json`）；**查询参数**、**路径变量**、**请求体**字段：名称、类型、是否必填、默认值、合法取值。 |
| 不同情况的处理 | 校验失败、资源不存在、冲突、外部服务超时等分别返回的 **HTTP 状态码**与**响应体结构**（建议统一错误 DTO 时注明字段含义）。 |
| 成功响应 | **状态码**；响应体 JSON 各字段含义；分页时 `total`、`page` 等约定。 |
| 副作用 | 是否读写 `config/json`、`data/`、调用外部 API 等。 |

有集成测试或契约测试时，可在文档中引用测试类名便于核对行为。

---

## 3. 与本文同步的源码锚点

| 行为 | 主要源码 |
|------|-----------|
| 静态目录、`/**`、SPA 回退 | `com.kitepromiss.desubtitle.web.WebDirectoryResourceConfig` |
| 根路径重定向 | 同上，`addViewControllers` |
| `GET /life`（细则见 [life.md](./life.md)） | `com.kitepromiss.desubtitle.api.LifeController` |
| `GET /getAgreement`（细则见 [get-agreement.md](./get-agreement.md)） | `com.kitepromiss.desubtitle.api.AgreementController`、`AgreementService` |
| `GET /getIndicator`（细则见 [get-indicator.md](./get-indicator.md)） | `IndicatorController`、`IndicatorSnapshotService`、`IndicatorRecorder`、`VideoLifecycleRecorder`、`InMemoryIndicatorRegistry` |
| `GET /getUserToken`（细则见 [get-user-token.md](./get-user-token.md)） | `UserTokenController`、`UserJwtIssuerService`、`UserTokenRepository`、`UserJwtSignatureSupport` |
| `POST /uploadVideo`（细则见 [upload-video.md](./upload-video.md)） | `VideoUploadController`、`VideoUploadService`、`UserVideoRepository`、`VideoUploadLuaSettings` |
| `POST /sendToDeSubtitle`（细则见 [send-to-desubtitle.md](./send-to-desubtitle.md)） | `SendToDeSubtitleController`、`SendToDeSubtitleService`、`UserIdStripeLock`、`VideoenhanSubtitleEraseOperations`、`DefaultVideoenhanSubtitleEraseOperations`、`AliyunCredentialsSource`、`AliyunAccessKeyResolver`、`UserVideoRepository`、`VideoUploadLuaSettings`、`VideoLifecycleRecorder`、`IndicatorRecorder` |
| `POST /sendVideoToDeSubtitle`（细则见 [send-video-to-desubtitle.md](./send-video-to-desubtitle.md)） | 同上 |
| `GET /myVideos`（细则见 [my-videos.md](./my-videos.md)） | `UserVideoController`、`VideoLifecycleRecorder` |
| `DELETE /userVideo/{videoId}`（细则见 [revoke-user-video.md](./revoke-user-video.md)） | `UserVideoController`、`UserVideoRevocationService`、`UserIdStripeLock`、`UserVideoNotFoundHandler` |
| `GET /userVideo/{videoId}/source` / `output`（细则见 [stream-user-video.md](./stream-user-video.md)） | `UserVideoStreamController`、`UserVideoStreamService`、`VideoStorageFileNames`、`WorkspacePaths` |
| Bearer 匿名 JWT（细则见 [bearer-user-auth.md](./bearer-user-auth.md)） | `MvcPublicEndpointRules`、`AnonymousUserBearerInterceptor`、`UserJwtAuthenticationService`、`AnonymousUserJwtGate` |
| SQLite 并发门闩（细则见 [../architectuure/sqlite-access.md](../architectuure/sqlite-access.md)） | `SqliteConcurrencyController` |
| `GET /init/status`（细则见 [init-status.md](./init-status.md)） | `com.kitepromiss.desubtitle.api.InitStatusController` |
| `POST /init/credentials`（细则见 [init-credentials.md](./init-credentials.md)） | `com.kitepromiss.desubtitle.api.InitCredentialController` |
| `POST /init`（细则见 [init.md](./init.md)） | `com.kitepromiss.desubtitle.api.InitController`、`com.kitepromiss.desubtitle.init.InitService` |
| AccessKey 引导存储 | `credential` 包内实体、仓库、`AliyunCredentialSetupService`、`AliyunCredentialInitBridge`、`CredentialInitPrecondition`、`MissingAliyunCredentialsException` |
| 初始化完成前/执行中的 API 门禁（细则见 [init.md](./init.md)） | `InitializationGateInterceptor`、`InitializationAccessGate`、`InitializationWebConfiguration`（并注册 `AnonymousUserBearerInterceptor`） |
| `POST /init` 全进程互斥 | `InitExecutionMutex`、`ConcurrentInitInProgressException` |
| 行为回归测试 | `WebDirectoryResourceConfigTest`、`LifeControllerTest`、`InitControllerTest`、`InitServiceTest`、`InitializationGateInterceptorTest`、`MvcPublicEndpointRulesTest`、`IndicatorControllerTest`、`InMemoryIndicatorRegistryTest`、`VideoLifecycleRecorderTest`、`VideoUploadControllerTest`、`VideoUploadControllerQuotaTest`、`VideoUploadServiceTest`、`VideoUploadLuaSettingsTest`、`SendToDeSubtitleServiceTest`、`UserVideoStreamServiceTest`、`UserDataManagerTest`、`UserTokenControllerTest`、`UserJwtIssuerServiceTest`、`UserTokenLuaSettingsTest`、`SqliteConcurrencyControllerTest`、`AnonymousUserBearerInterceptorTest`、`UserJwtAuthenticationServiceTest` |
| 工作目录路径 Bean | `WorkspacePaths`、`WorkspaceConfiguration`（`com.kitepromiss.desubtitle.workspace`） |

修改上述类时，**须同步更新**对应端点专文与本文相关小节，避免文档与实现脱节。
