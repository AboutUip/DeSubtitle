# `config/lua/video_upload.lua`

仓库内路径：`config/lua/video_upload.lua`。属 **`config/lua/`**（只读参考、应用不写回；逐项 `--` 注释要求见 [../restriction/code-conventions.md](../restriction/code-conventions.md) §4）。

## 文件作用

控制 **`POST /uploadVideo`** 的**每用户上传条数上限**与**落盘保留时长**（到期后 **`VideoLifecycleRecorder`** 删文件与库行）。详见 [../api/upload-video.md](../api/upload-video.md)。

## 当前加载情况

由 **`VideoUploadLuaSettings`** 读取；缺失或解析失败时：`max_videos_per_user` 默认 **3**，`video_retention_minutes` 默认 **5**，`desubtitle_output_retention_minutes` 默认 **10**，`desubtitle_poll_timeout_minutes` 默认 **10**；各键均有上下限夹取（见源码常量）。

## 配置项

| 键 | 作用 | 影响 | 支持的值 | 默认值（仓库） |
|----|------|------|-----------|----------------|
| `max_videos_per_user` | 单用户成功上传次数上限 | `UserVideoRepository#countByUserId` 达该值后拒绝新上传 | Lua **数字**（解析为整数后夹取） | **3** |
| `video_retention_minutes` | 每条上传在 `data/videos/` 与表中的保留时长 | 写入 `user_videos.expires_at`；到期后定时任务删盘与删行；指标 `videoExpiresInSeconds` 据此计算剩余秒数 | Lua **数字**（分钟，整数夹取） | **5** |
| `desubtitle_output_retention_minutes` | 去字幕成品在 `data/desubtitle/` 的保留时长 | 写入 `user_videos.desubtitle_output_expires_at`；到期后删成品文件并清空输出列 | Lua **数字**（分钟，整数夹取） | **10** |
| `desubtitle_poll_timeout_minutes` | 单次 `POST /sendToDeSubtitle` 内对**单条**任务轮询 GetAsyncJobResult 的最长等待 | 超时则记为本请求内失败（`LOCAL_POLL_TIMEOUT`），可下次再调 | Lua **数字**（分钟，整数夹取） | **10** |

## 与实现的对齐

`LuaConfigLoader`；`VideoUploadLuaSettings`、`VideoUploadService`、`SendToDeSubtitleService`、`VideoLifecycleRecorder`、`UserVideoExpiresAtSqliteMigration`、`UserVideoDesubtitleSqliteMigration`（旧库补列）。清理调度间隔见 `config/yml/application.yml` 中 **`desubtitle.video.lifecycle-purge-interval-ms`**。单文件与请求体大小另见 `spring.servlet.multipart.*`（同文件）。
