# 后端 HTTP 能力（索引）

本目录维护 **由 Java（Spring Boot）进程直接提供的 HTTP 行为**：静态资源与单页回退的**总约定**，以及**按端点拆分**的 API 专文。实现与代码的对应关系见 [../architectuure/java-modules.md](../architectuure/java-modules.md)。

## HTTP API 专文（逐项）

| 方法与路径 | 文档 | 摘要 |
|------------|------|------|
| `GET /life` | [life.md](./life.md) | 存活、JWT 校验/刷新、`videoProcessingLanes`、**`indicators` 指标快照**（须带 `Authorization: Bearer`） |
| `GET /getAgreement` | [get-agreement.md](./get-agreement.md) | 无参，返回 `agreement.json` 中 `text` 的纯文本正文 |
| `POST /uploadVideo` | [upload-video.md](./upload-video.md) | `multipart` 字段 `file`；配额见 `video_upload.lua`；落盘 `data/videos/` + 表 `user_videos` |
| `POST /sendToDeSubtitle` | [send-to-desubtitle.md](./send-to-desubtitle.md) | 未过期本地上传批量提交阿里云擦字幕、轮询并下载成品至 `data/desubtitle/` |
| `POST /sendVideoToDeSubtitle` | [send-video-to-desubtitle.md](./send-video-to-desubtitle.md) | 单条 `videoId` 提交去字幕（与批量接口按用户互斥） |
| `GET /myVideos` | [my-videos.md](./my-videos.md) | 当前用户视频生命周期列表（结构同指标 `videoLifecycles` 元素） |
| `DELETE /userVideo/{videoId}` | [revoke-user-video.md](./revoke-user-video.md) | 用户主动删源视频与去字幕成品及库行 |
| `GET /userVideo/{videoId}/source`、`GET /userVideo/{videoId}/output` | [stream-user-video.md](./stream-user-video.md) | 当前用户源/成品视频流（Bearer + fetch/Blob） |
| `GET /getUserToken` | [get-user-token.md](./get-user-token.md) | 无参，写 `user_tokens` 并签发 JWT；TTL 见 `user_token.lua` |
| （横切）Bearer 匿名 JWT | [bearer-user-auth.md](./bearer-user-auth.md) | 白名单外 MVC 接口须 `Authorization: Bearer …` |
| `GET /init/status` | [init-status.md](./init-status.md) | 初始化是否完成、是否调试、是否已配置 AccessKey |
| `POST /init/credentials` | [init-credentials.md](./init-credentials.md) | 提交 AccessKey（非调试写 SQLite 明文，调试仅内存） |
| `POST /init` | [init.md](./init.md) | 校验密钥已就绪后初始化 `data/` / SQLite；写回 `runtime.json`（非调试） |

新增或变更 Controller 端点时：**新增或更新对应专文**，并在上表登记。

## 横切约定

| 文档 | 内容 |
|------|------|
| [后端 HTTP 与 REST 约定](./backend-http-api.md) | 静态资源路径、SPA 回退、状态码；`/api/**` 规划；新 API 文档应写清的内容 |

外部厂商 SDK、异步任务等**非本仓库 Controller** 的参考见 [../reference/README.md](../reference/README.md)。
