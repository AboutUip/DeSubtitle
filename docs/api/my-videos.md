# `GET /myVideos` — 当前用户视频生命周期列表

| 项目 | 说明 |
|------|------|
| 作用 | 返回当前 JWT 用户（`sub`）在 **`user_videos`** 中所有行的生命周期视图，与 **`GET /getIndicator`** 中 **`videoLifecycles`** 单条元素结构相同（快照时刻为服务端 `System.currentTimeMillis()`）。 |
| 方法与路径 | `GET /myVideos` |
| 请求 | 无参数、无请求体。 |
| 请求头 | 初始化完成后须 **`Authorization: Bearer <JWT>`**。 |
| 成功响应 | **200 OK**，`Content-Type: application/json`，体为 **JSON 数组**；元素字段见 [get-indicator.md](./get-indicator.md) 中 `videoLifecycles` 项说明；按上传 **`created_at` 升序**。 |
| **401** | `{"error":"missing_user_context"}` |
| 实现锚点 | `UserVideoController`、`VideoLifecycleRecorder#videoLifecyclesForUserAtMillis` |

目录索引见 [README.md](./README.md)。
