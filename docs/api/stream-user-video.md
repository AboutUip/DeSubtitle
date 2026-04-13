# `GET /userVideo/{videoId}/source` / `GET /userVideo/{videoId}/output` — 当前用户视频流

| 项目 | 说明 |
|------|------|
| 作用 | 在**校验 JWT 用户为行所有者**后，以流形式返回 **`data/videos/`** 源文件或 **`data/desubtitle/`** 去字幕成品，供前端 **`fetch` + `Authorization` + Blob** 绑定 `<video>`（HTML 无法为 `src` 直接带 Bearer）。路径经 **`VideoStorageFileNames.safeResolve`**，防穿越。 |
| 方法与路径 | `GET /userVideo/{videoId}/source`、`GET /userVideo/{videoId}/output` |
| 请求头 | 初始化完成后须 **`Authorization: Bearer <JWT>`**；与 [bearer-user-auth.md](./bearer-user-auth.md) 一致。 |
| 成功响应 | **200**，`Content-Type` 源侧取自表 `content_type`（无效则 `application/octet-stream`）；成品侧为探测/按扩展名推断（如 `video/mp4`）；`Content-Length`、`Accept-Ranges: bytes`。 |
| **401** | `{"error":"missing_user_context"}` |
| **404** | `{"error":"video_not_found"}`（无行、非本人、成品未生成、盘片缺失或非法名）；与 [revoke-user-video.md](./revoke-user-video.md) 同源异常处理。 |
| 实现锚点 | `UserVideoStreamController`、`UserVideoStreamService`、`VideoStorageFileNames` |

索引见 [README.md](./README.md)。
