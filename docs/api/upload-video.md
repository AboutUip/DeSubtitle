# `POST /uploadVideo` — 上传视频（配额 + 落盘 + SQLite）

| 项目 | 说明 |
|------|------|
| 作用 | 将当前匿名用户（JWT `sub`）提交的视频保存到 **`data/videos/`**（随机文件名）；元数据写入 **`user_videos`**（含 **`expires_at`**，由 **`video_retention_minutes`** 决定）；递增计数器 **`video_uploads_user_<userId>`**；到期后 **`VideoLifecycleRecorder`** 自动删盘与删行（见 [get-indicator.md](./get-indicator.md) 的 **`videoExpiresInSeconds`**）。 |
| 方法与路径 | `POST /uploadVideo` |
| 请求 | `multipart/form-data`，字段名 **`file`**（单文件）。 |
| 请求头 | 初始化完成后须 **`Authorization: Bearer <JWT>`**（见 [bearer-user-auth.md](./bearer-user-auth.md)）；未完成初始化时先被门禁 **503**。 |
| 成功响应 | **200 OK**，`Content-Type: application/json`，体为 `UploadVideoResponse`：`id`（行主键 UUID）、`storedFileName`（磁盘名）、`originalFileName`、`sizeBytes`、`contentType`。 |
| 配额与保留 | **`max_videos_per_user`**（默认 3）超限返回 **409** `video_quota_exceeded`。**`video_retention_minutes`**（默认 5）决定每条记录的过期时刻。配置说明见 [../config/lua-video_upload.md](../config/lua-video_upload.md)。 |
| 其它错误 | **400** `empty_file`；**401** `missing_user_context`；**500** `upload_failed`（落盘或持久化异常）。 |
| 后缀规则 | 仅当**最后一个点之前的主名中不含其它点**、且后缀为纯字母数字（如 `.mp4`、`.mov`）且总长合理时保留；否则落盘为无后缀 UUID 文件名（实现见 `VideoUploadService#safeExtension`）。 |
| 配置 | 单文件与总请求体上限见 `config/yml/application.yml` 中 **`spring.servlet.multipart.*`**（默认 512MB）。 |
| 实现锚点 | `VideoUploadController`、`VideoUploadService`、`VideoLifecycleRecorder`、`UserVideoExpiresAtSqliteMigration`、`UserVideoEntity`、`UserVideoRepository`、`VideoUploadLuaSettings` |

目录索引见 [README.md](./README.md)。
