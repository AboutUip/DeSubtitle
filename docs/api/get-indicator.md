# `GET /getIndicator` — 内存指标快照（JSON）

| 项目 | 说明 |
|------|------|
| 作用 | 返回**调用时刻**指标快照：`counters`/`gauges` 为进程内纯内存（重启清空）；**`videoExpiresInSeconds`** 与 **`videoLifecycles`** 来自 SQLite **`user_videos`** 与快照时刻对齐（后者为每条视频的结构化生命周期）。 |
| 方法与路径 | `GET /getIndicator` |
| 请求 | 无参数、无请求体。 |
| 成功响应 | **200 OK**，`Content-Type: application/json` |
| 响应体 | 对象字段：`counters`（`string → long`）、`gauges`（`string → number`）、`videoExpiresInSeconds`（`string → long`，**上传视频 id** → 距离该条 `expires_at` 的**剩余整秒数**，已过期待清理为 0）、`videoLifecycles`（**数组**：每项含 `videoId`、`userId`、源文件元数据、上传/成品过期时刻与剩余秒、`desubtitleLastStatus`、`desubtitleJobId`、`desubtitleError` 等，与 [my-videos.md](./my-videos.md) 单条结构一致；按 `videoId` 字典序）、`capturedAtEpochMillis`（`long`）。其中 `counters`/`gauges` 键名按字典序。 |
| 内置量表 | **`online_users`**（`double`）：由 **`LifeOnlineUserTracker`** 维护，统计**最近 120 秒内成功调用过 `GET /life`** 且已确定 `userId` 的去重用户数；依据 **life 调用** 刷新心跳，**不**根据其它接口上的「Bearer 有效 token」推断在线。 |
| 内置计数器 | **`video_uploads_user_<userId>`**（`long`）：每成功一次 **`POST /uploadVideo`** 对应当前 JWT `sub` 递增 1；**`desubtitle_batch_requests_user_<suffix>`**：每调用一次 **`POST /sendToDeSubtitle`**；**`desubtitle_single_requests_user_<suffix>`**：每调用一次 **`POST /sendVideoToDeSubtitle`**；**`desubtitle_job_success_user_<suffix>`**：每有一条视频去字幕**成功落盘**（`outcome=success`）；**`video_revokes_user_<suffix>`**：每成功一次 **`DELETE /userVideo/{id}`**。`userId` 中非 `[a-zA-Z0-9_-]` 字符在键名中替换为 `_`（`suffix` 同 [upload-video.md](./upload-video.md) 中计数器规则）。 |
| 记录方式 | 业务代码注入 **`IndicatorRecorder`**（默认实现 **`InMemoryIndicatorRegistry`**），调用 `incrementCounter` / `setGauge` 等；见下文。 |
| 门禁 | 未完成初始化亦可访问（初始化门禁白名单）。**初始化完成后**须 **`Authorization: Bearer <JWT>`**（见 [bearer-user-auth.md](./bearer-user-auth.md)）。同一快照也可在带 Bearer 调用 **`GET /life`** 时随响应字段 `indicators` 返回（见 [life.md](./life.md)）。 |

## 内部 API（`IndicatorRecorder`）

| 方法 | 说明 |
|------|------|
| `incrementCounter(String name)` | 计数器 +1 |
| `incrementCounter(String name, long delta)` | 计数器增加 `delta`（可为负） |
| `setGauge(String name, double value)` | 量表设为当前值（覆盖） |
| `snapshot()` | 与 HTTP 快照语义相同，可在无 Web 场景使用 |

`name` 为 `null` 或空白字符串时，记录调用会被忽略。

## 实现锚点

`InMemoryIndicatorRegistry`、`IndicatorRecorder`、`IndicatorSnapshot`、`VideoLifecycleDetail`、`IndicatorSnapshotService`、`VideoLifecycleRecorder`、`IndicatorController`、`LifeOnlineUserTracker`。
