# `POST /sendToDeSubtitle` — 批量提交去字幕并落盘成品

| 项目 | 说明 |
|------|------|
| 作用 | 将当前 JWT 用户（`sub`）在 **`user_videos` 中仍有效**（`expires_at` 未到）且磁盘上 **`data/videos/` 源文件存在** 的记录，按创建时间顺序依次：以 **EraseVideoSubtitlesAdvance** 提交本地文件、**GetAsyncJobResult** 轮询至终态或超时、成功时从返回的 **VideoUrl** 下载至 **`data/desubtitle/`**（UUID 文件名）；在表中维护 **jobId、异步状态、错误摘要、成品文件名与成品过期时刻**。已 **PROCESS_SUCCESS** 且成品文件仍在、且成品未过期的记录会 **skipped**。 |
| 方法与路径 | `POST /sendToDeSubtitle` |
| 请求体 | 无 |
| 请求头 | 初始化完成后须 **`Authorization: Bearer <JWT>`**；须已配置阿里云 AccessKey（`POST /init/credentials` 或环境变量 `ALIBABA_CLOUD_ACCESS_KEY_ID` / `ALIBABA_CLOUD_ACCESS_KEY_SECRET`）。 |
| 成功响应 | **200 OK**，`Content-Type: application/json`，体为 `SendToDeSubtitleBatchResponse`：`results` 为数组，每项含 `videoId`、`outcome`（`success` / `skipped` / `failed` / `timeout`）、`aliyunStatus`、`storedOutputFileName`（成功时）、`outputExpiresAtEpochMillis`（成功或跳过时若有）、`error`（失败/跳过原因码或说明）。 |
| **428** | 无任何可用 AccessKey 时 **`{"error":"need_credentials"}`**（与 `POST /init` 缺密钥时错误码一致）。 |
| **401** | `missing_user_context`（无 Bearer 用户）。 |
| 外部依赖 | 阿里云视觉智能 videoenhan（上海）；契约见 [../reference/aliyun-erase-video-subtitles.md](../reference/aliyun-erase-video-subtitles.md)、[../reference/aliyun-async-job-result.md](../reference/aliyun-async-job-result.md)。 |
| 配置 | **`desubtitle_output_retention_minutes`**（默认 10）：成品在 `data/desubtitle/` 保留分钟数；**`desubtitle_poll_timeout_minutes`**（默认 10）：单条任务在本请求内轮询最久分钟数。见 [../config/lua-video_upload.md](../config/lua-video_upload.md)。 |
| 副作用 | 读写 `user_videos`；写 `data/desubtitle/`；调用阿里云与厂商临时 URL。 |
| 并发 | 同一 JWT `sub` 下，本接口、**`POST /sendVideoToDeSubtitle`** 与 **`DELETE /userVideo/{id}`** 在 **`UserIdStripeLock`** 上互斥（与 **`POST /uploadVideo`** 配额 stripe 独立），避免撤销与去字幕交错、并行重复提交。 |
| 指标 | 每次调用递增 **`desubtitle_batch_requests_user_<suffix>`**；每条 **`outcome=success`** 递增 **`desubtitle_job_success_user_<suffix>`**（见 [life.md](./life.md) **`indicators.counters`**）。 |
| 实现锚点 | `SendToDeSubtitleController`、`SendToDeSubtitleService`、`VideoenhanSubtitleEraseOperations`、`DefaultVideoenhanSubtitleEraseOperations`、`AliyunAccessKeyResolver`、`UserVideoEntity`、`UserVideoRepository`、`VideoLifecycleRecorder`、`UserVideoDesubtitleSqliteMigration`、`VideoUploadLuaSettings` |

目录索引见 [README.md](./README.md)。
