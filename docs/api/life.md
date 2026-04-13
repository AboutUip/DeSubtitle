# `GET /life` — 存活、Token 与指标快照

| 项目 | 说明 |
|------|------|
| 作用 | 一站式：**进程在线**、**校验客户端提交的 JWT**（无效则**立即签发新 token**，与 `GET /getUserToken` 同源逻辑）、以及**指标快照**（与 **`GET /getIndicator`** 相同，由 **`IndicatorSnapshotService#combinedSnapshot()`** 合并内存 counters/gauges 与 **`videoExpiresInSeconds`**）。 |
| 方法与路径 | `GET /life`（**非** `/api` 前缀）。 |
| 请求头 | **必须**携带 `Authorization: Bearer <compact JWT>`。`Bearer` 大小写不敏感；**不经**全局 `AnonymousUserBearerInterceptor` 验库，由本控制器自行解析；缺头或非 Bearer 格式返回 **400**（见下）。 |
| 成功响应 | **200 OK**，`Content-Type: application/json`，体为对象（`LifeStatusPayload`）： |
| 字段 | `alive`、`submittedTokenValid`、`tokenRefreshed`、`token`、`expiresInSeconds`（JWT）、`userId`、`videoProcessingLanes`（整数 **1–8**：前端应据此动态挂载并行「视频去字幕」路数；默认来自 `desubtitle.ui.video-processing-lanes`）、`indicators`（`counters`、`gauges`、`videoExpiresInSeconds`、`videoLifecycles`、`capturedAtEpochMillis`，细则见 [get-indicator.md](./get-indicator.md)）。 |
| 错误响应 | **400** `application/json`：`error` 为 `missing_token` 或 `invalid_authorization_header`。**503** `jwt_secret_not_configured`：需配置 `DESUBTITLE_JWT_SECRET` 才能刷新 token（与 [get-user-token.md](./get-user-token.md) 一致）。 |
| 门禁 | 未完成初始化亦可访问（初始化门禁白名单）。**不**占用全局 Bearer 白名单的「免头」语义：客户端**仍须**带头，只是由 `LifeController` 自行处理而非拦截器。 |
| 副作用 | 当 `submittedTokenValid=false` 且签发成功时，与 `GET /getUserToken` 相同会写入 **`user_tokens`**。 |
| 实现类 | `com.kitepromiss.desubtitle.api.LifeController` |
| 回归测试 | `LifeControllerTest`（Surefire 注入 `DESUBTITLE_JWT_SECRET`） |

总约定与静态资源行为见 [backend-http-api.md](./backend-http-api.md)；目录索引见 [README.md](./README.md)。
