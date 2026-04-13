# Java 主源码模块一览（`src/main/java`）

## 约定

- **一个 `.java` 文件 = 一个模块 = 唯一职责**；若无法用一句话说清该文件「只负责什么」，或同一文件并列承担多条无关任务，视为**架构问题**，应**重构**（拆分或调整包结构）。
- 本文**仅**覆盖 `src/main/java`，**不**说明 `src/test/java`。
- 增删改 main 源码时须**同步更新**本表（直接覆盖，保持与仓库一致）。

## 模块表

| 类（包路径） | 唯一职责 |
|--------------|----------|
| `DeSubtitleApplication`（`com.kitepromiss.desubtitle`） | Spring Boot 入口：合并 `StartupLuaPorts` 产出的默认属性后启动应用。 |
| `LuaConfigLoader`（`…config`） | 从磁盘执行 `config/lua` 下 Lua 脚本并返回根表（`LuaTable`）。 |
| `JsonConfigLoader`（`…config`） | 读写 `config/json` 下 JSON 文件为 `JsonNode`（UTF-8、缩进输出）。 |
| `StartupLuaPorts`（`…config`） | 读取 `config/lua/ports.lua`，将 `backend_port` / `frontend_port` 转为 Spring 默认属性键。 |
| `WebDirectoryResourceConfig`（`…web`） | 将仓库根目录 `web/` 注册为静态资源；根路径重定向至 `index.html`；无扩展名路径回退 SPA 入口。 |
| `InitializationGateInterceptor`（`…web`） | 未完成初始化或正在执行 `InitService` 时拦截非 `MvcPublicEndpointRules` 白名单内的控制器，返回 503 JSON。 |
| `MvcPublicEndpointRules`（`…web`） | `allowsWithoutInitialization`（宽）与 `allowsWithoutBearer`（窄：仅 getUserToken + init 三接口）分两套白名单。 |
| `AnonymousUserBearerInterceptor`（`…web`） | 白名单外要求 `Authorization: Bearer` JWT，并核对 `user_tokens`；写入 `AnonymousUserRequestAttributes`。 |
| `InitializationWebConfiguration`（`…web`） | 注册 `InitializationGateInterceptor`（最高优先级）与 `AnonymousUserBearerInterceptor`（次优先级）。 |
| `InitializationAccessGate`（`…init`） | 缓存 `runtime.json` 完成标记；标记 `InitService` 执行区间供拦截器使用。 |
| `InitExecutionMutex`（`…init`） | `ReentrantLock.tryLock`：`POST /init` 业务段全进程互斥，失败由控制器返回 409。 |
| `ConcurrentInitInProgressException`（`…init`） | 互斥抢锁失败时抛出，由 `InitController` 映射为 HTTP 409。 |
| `LifeController`（`…api`） | 提供 `GET /life`：须带头 {@code Authorization: Bearer …}（不经全局 Bearer 拦截器）；返回存活、token 校验/按需刷新及内存指标快照 JSON（{@code LifeStatusPayload}）。 |
| `AgreementController`（`…api`） | `GET /getAgreement`：无参，返回 {@code agreement.json} 中 {@code text} 的纯文本。 |
| `IndicatorController`（`…api`） | `GET /getIndicator`：无参，返回纯内存指标快照 JSON（`IndicatorSnapshot`）。 |
| `IndicatorRecorder`（`…indicator`，接口） | 内部记录计数器、量表；默认 Bean `InMemoryIndicatorRegistry`。 |
| `InMemoryIndicatorRegistry`（`…indicator`） | 线程安全纯内存指标；`snapshot()` 供 HTTP 与内部共用。 |
| `LifeOnlineUserTracker`（`…indicator`） | 在每次成功 `GET /life` 后按 `userId` 更新心跳并写入量表 `online_users`（与提交 JWT 是否有效无关）。 |
| `IndicatorSnapshot`（`…indicator`） | `counters`、`gauges`、`videoExpiresInSeconds`、`videoLifecycles`、`capturedAtEpochMillis` 快照 DTO。 |
| `VideoLifecycleDetail`（`…indicator`） | 单条 `user_videos` 在快照时刻的生命周期 JSON 视图。 |
| `AgreementService`（`…agreement`） | 读取 {@code config/json/agreement.json} 的 {@code text} 字段为字符串。 |
| `UserTokenController`（`…api`） | `GET /getUserToken`：门闩内写 `user_tokens` 并签发 HS256 JWT（`sub`、`jti`）。 |
| `UserJwtIssuerService`（`…user`） | 签发 JWT；经 `SqliteConcurrencyController` 持久化 `UserTokenEntity`。 |
| `UserJwtAuthenticationService`（`…user`） | 验签并在门闩内核对 `user_tokens`；实现 `AnonymousUserJwtGate`。 |
| `AnonymousUserJwtGate`（`…user`，接口） | 由拦截器调用，校验紧凑 JWT 是否对应有效未撤销行；返回 `AnonymousUserPrincipal`。 |
| `AnonymousUserPrincipal`（`…user`） | 已校验会话：`userId`（`sub`）、`tokenId`（`jti`）。 |
| `UserJwtSignatureSupport`（`…user`） | 自 `JwtSigningSecretSource` 构造 HS256 `SecretKey`。 |
| `UserTokenEntity` / `UserTokenRepository`（`…user`） | JPA 表 `user_tokens`；供签发、校验、撤销。 |
| `UserTokenManagementService`（`…user`） | `revokeByJti` 等；门闩内更新 `user_tokens`。 |
| `UserDataManager`（`…userdata`） | 内存按用户分区状态；从请求上下文解析当前用户；`revokeCurrentUserToken` 委托撤销。 |
| `NoCurrentUserContextException`（`…userdata`） | 无匿名用户上下文时抛出。 |
| `AnonymousUserRequestAttributes`（`…user`） | 请求属性键：匿名用户 id（`sub`）。 |
| `UserTokenLuaSettings`（`…user`） | 读取 `config/lua/user_token.lua` 的 `token_ttl_minutes`。 |
| `JwtSigningSecretSource`（`…user`，接口） | 提供 JWT 签名用原始密钥字符串。 |
| `EnvJwtSigningSecretSource`（`…user`） | 自环境变量 `DESUBTITLE_JWT_SECRET` 读取密钥。 |
| `JwtSecretNotConfiguredException`（`…user`） | 密钥缺失或过短时抛出，映射为 HTTP 503。 |
| `SqliteConcurrencyController`（`…sqlite`） | 进程内互斥门闩；所有 `desubtitle.db` 业务访问须在其临界区内。 |
| `InitStatusController`（`…api`） | `GET /init/status`：返回已初始化、调试模式、凭证是否就绪。 |
| `InitCredentialController`（`…api`） | `POST /init/credentials`：接收 AccessKey，按 `debug_mode` 写 SQLite 或仅内存。 |
| `InitController`（`…api`） | `POST /init`：委托 `InitService`；200 / 409 / 428 等见 `InitConflictBody`。 |
| `InitService`（`…init`） | 校验 `CredentialInitPrecondition` 后初始化 `data/`（清空时保留 `desubtitle.db`）、经门闩触库、写回 `runtime.json`；互斥与门禁协同。 |
| `CredentialInitPrecondition`（`…credential`，接口） | 在 `InitService` 进入执行区间前断言 AccessKey 已就绪并返回 `debug_mode` 快照。 |
| `AliyunCredentialInitBridge`（`…credential`） | 上述接口的默认实现，委托 `AliyunCredentialSetupService`。 |
| `AliyunCredentialSetupService`（`…credential`） | 读 `debug_mode`；非调试时对仓库的读写经 `SqliteConcurrencyController`。 |
| `AliyunCredentialsEntity`（`…credential`） | JPA 实体：单行 AccessKey 明文（表 `aliyun_credentials`）。 |
| `AliyunCredentialsRepository`（`…credential`） | 上述实体 Spring Data 仓库。 |
| `InMemoryAliyunCredentialHolder`（`…credential`） | 调试模式下进程内 AccessKey 存放。 |
| `MissingAliyunCredentialsException`（`…credential`） | `InitService` 前置条件失败，映射为 HTTP 428。 |
| `AliyunCredentialsSource`（`…credential`，接口） | 提供当前可用的阿里云 AccessKey 对（供去字幕等出站调用）。 |
| `ResolvedAliyunKeys`（`…credential`） | AccessKeyId + Secret 值对象。 |
| `AliyunAccessKeyResolver`（`…credential`） | 实现 `AliyunCredentialsSource`：调试读内存、非调试经门闩读库，缺省再读 `ALIBABA_CLOUD_*` 环境变量。 |
| `WorkspacePaths`（`…workspace`） | 记录工作目录下 `runtime.json`、`runtime_mode.lua`、`data/`、`agreement.json`、`user_token.lua`、`video_upload.lua` 的绝对路径。 |
| `VideoUploadController`（`…api`） | `POST /uploadVideo`：`multipart` 字段 `file`；委托 `VideoUploadService`。 |
| `UploadVideoResponse`（`…api`） | 上传成功 JSON：`id`、`storedFileName`、`originalFileName`、`sizeBytes`、`contentType`。 |
| `VideoUploadService`（`…video`） | 校验每用户配额、随机文件名写入 `data/videos/`、JPA 持久化（含 `expires_at`）、`IndicatorRecorder` 按用户递增计数器。 |
| `VideoLifecycleRecorder`（`…video`） | 定时删除过期上传（`data/videos/` 与行）；删除过期去字幕产物（`data/desubtitle/` 与列）；为指标提供 `videoExpiresInSeconds` 与全量/按用户的 `VideoLifecycleDetail` 列表。 |
| `UserVideoExpiresAtSqliteMigration`（`…video`） | 应用就绪时对旧 SQLite 库执行 `ALTER TABLE user_videos ADD expires_at` 并回填。 |
| `UserVideoDesubtitleSqliteMigration`（`…video`） | 应用就绪时为 `user_videos` 补齐去字幕相关列（job 状态、本地成品名与过期时刻等）。 |
| `VideoUploadLuaSettings`（`…video`） | 读取 `video_upload.lua`：上传上限、源视频保留、去字幕成品保留与轮询超时等。 |
| `UserVideoEntity` / `UserVideoRepository`（`…video`） | JPA 表 `user_videos`：用户、源盘片名、元数据、过期时刻及阿里云任务与 `data/desubtitle/` 成品字段。 |
| `VideoQuotaExceededException`（`…video`） | 超配额时由控制器映射为 HTTP 409。 |
| `VideoenhanSubtitleEraseOperations`（`…aliyun`，接口） | 封装 EraseVideoSubtitlesAdvance 提交与 GetAsyncJobResult 查询。 |
| `DefaultVideoenhanSubtitleEraseOperations`（`…aliyun`） | 基于官方 `videoenhan20200320` SDK 的默认实现（上海 endpoint）。 |
| `SubtitleAsyncJobState`（`…aliyun`） | 异步查询结果视图（Status、Result JSON 字符串等）。 |
| `SendToDeSubtitleService`（`…video`） | 按用户 stripe 串行：批量或单条提交擦除、轮询、下载成品；记录去字幕相关计数器。 |
| `SendToDeSubtitleController`（`…api`） | `POST /sendToDeSubtitle`、`POST /sendVideoToDeSubtitle`；须 Bearer；缺凭证时 428。 |
| `SendVideoToDeSubtitleRequest`（`…api`） | 单条去字幕请求 JSON：`videoId`、`subtitlePosition`（可选）。 |
| `SubtitleVerticalPosition`（`…api`） | 字幕竖直档位 → 阿里云 BY/BH（BX=0、BW=1）。 |
| `SubtitleEraseBand`（`…aliyun`） | EraseVideoSubtitles 归一化矩形 BX/BY/BW/BH。 |
| `SendToDeSubtitleBatchResponse` / `SendToDeSubtitleItemResult`（`…api`） | 去字幕 HTTP 响应 DTO（批量体或单条体）。 |
| `UserVideoController`（`…api`） | `GET /myVideos`、`DELETE /userVideo/{videoId}`。 |
| `UserVideoStreamService`（`…video`） | 校验所有者后解析 `data/videos/` 或 `data/desubtitle/` 下安全路径，供 HTTP 流式返回。 |
| `UserVideoStreamController`（`…api`） | `GET /userVideo/{id}/source`、`GET /userVideo/{id}/output`：Bearer 下当前用户源/成品文件流。 |
| `UserIdStripeLock`（`…video`） | 按 userId 分条的进程内锁；{@code SendToDeSubtitleService} 与 {@code UserVideoRevocationService} 共用，使用户维度的去字幕与撤销互斥。 |
| `UserVideoRevocationService`（`…video`） | 校验所有者后删源/成品文件与库行；递增撤销计数器；与去字幕同锁互斥。 |
| `UserVideoNotFoundException`（`…video`） | id 不存在或非本用户时抛出。 |
| `UserVideoNotFoundHandler`（`…api`） | 将上述异常映射为 HTTP 404 JSON。 |
| `IndicatorSnapshotService`（`…indicator`） | 合并 `IndicatorRecorder.snapshot()` 与 `VideoLifecycleRecorder` 的 `videoExpiresInSeconds` 与 `videoLifecycles`。 |
| `WorkspaceConfiguration`（`…workspace`） | 注册 `WorkspacePaths` Bean。 |
