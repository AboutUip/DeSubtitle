# `DELETE /userVideo/{videoId}` — 用户主动撤销视频

| 项目 | 说明 |
|------|------|
| 作用 | 删除 **`data/videos/`** 下源文件、**`data/desubtitle/`** 下去字幕成品（若存在）、并删除 **`user_videos`** 对应行；仅当 **`videoId` 属于当前 JWT 用户** 时成功。 |
| 方法与路径 | `DELETE /userVideo/{videoId}` |
| 路径变量 | **`videoId`**：`user_videos.id`（与 **`POST /uploadVideo`** 返回的 `id` 一致）。 |
| 请求体 | 无 |
| 请求头 | 初始化完成后须 **`Authorization: Bearer <JWT>`**。 |
| 成功响应 | **204 No Content**，无正文。 |
| **401** | `{"error":"missing_user_context"}` |
| **404** | `{"error":"video_not_found"}`（id 不存在或不属于当前用户，统一 404）。 |
| 副作用 | 删盘、删库行；递增指标 **`video_revokes_user_<suffix>`**（见 [get-indicator.md](./get-indicator.md)）。与 **`POST /sendToDeSubtitle`** / **`POST /sendVideoToDeSubtitle`** 共用 **`UserIdStripeLock`**，同一用户不会并发撤销与去字幕。 |
| 实现锚点 | `UserVideoController`、`UserVideoRevocationService`、`UserVideoNotFoundHandler` |

目录索引见 [README.md](./README.md)。
