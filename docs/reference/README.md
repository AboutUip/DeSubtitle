# DeSubtitle 参考文档索引

本目录为 **DeSubtitle**（基于阿里云视觉智能「视频生产」类目进行视频字幕擦除）的技术参考与接口说明，按主题拆分，便于实现与评审时对照。

## 「去字幕」指什么

应用界面、前端与 **`POST /sendVideoToDeSubtitle`** 等接口中的 **「去字幕」**，在本仓库与下列参考文档里，与阿里云能力 **EraseVideoSubtitles（视频字幕擦除）** 及随后的 **GetAsyncJobResult** 轮询一一对应：先异步提交擦除任务，再按 `JobId`（即提交返回的 `RequestId`）查状态与 `Result` 中的 **`VideoUrl`**。服务端编排见 `SendToDeSubtitleService`（包 `com.kitepromiss.desubtitle.video`），阿里云封装见 `VideoenhanSubtitleEraseOperations` / `DefaultVideoenhanSubtitleEraseOperations`。

仓库结构与前后端边界见 [../architectuure/README.md](../architectuure/README.md)。Agent 限制与告知见 [../restriction/README.md](../restriction/README.md)。

## 阅读顺序建议

| 顺序 | 文档 | 内容 |
|------|------|------|
| 1 | [系统概述与处理流水线](./overview.md) | 目标边界、异步两步模型、与存储的关系 |
| 2 | [字幕区域参数（BX/BY/BW/BH）](./subtitle-region.md) | 归一化坐标、默认值、与检测策略的关系 |
| 3 | [输入输出、限制与合规](./constraints-and-storage.md) | 格式/大小/分辨率、URL 规则、结果链接时效 |
| 4 | [EraseVideoSubtitles API](./aliyun-erase-video-subtitles.md) | **去字幕**第一步：提交擦除任务，拿 `RequestId` |
| 5 | [GetAsyncJobResult 与轮询](./aliyun-async-job-result.md) | **去字幕**第二步：轮询状态机、解析 `Result`（含 `VideoUrl`） |
| 6 | [SDK、Endpoint 与凭证](./sdk-endpoint-auth.md) | 环境变量、RAM、多语言包、`Advance` 上传路径 |
| 7 | [引导页 AccessKey 存储（明文与责任边界）](./aliyun-credentials-storage.md) | SQLite / 调试内存、与 `POST /init` 清目录行为 |
| 8 | [错误与运维要点](./errors-and-operations.md) | 状态失败分支、重试、计费与调试入口 |
| 9 | [用户数据管理器（内存分区与当前用户）](./user-data-manager.md) | `UserDataManager`、与 Bearer / `jti` 的关系 |

## 外部权威链接（实现时以最新版为准）

- [EraseVideoSubtitles（视频字幕擦除）](https://help.aliyun.com/zh/viapi/developer-reference/api-t470ol)
- [GetAsyncJobResult（异步任务查询）](https://help.aliyun.com/zh/viapi/developer-reference/api-querying-asynchronous-task-results)
- [通过 SDK 调用视频字幕擦除示例](https://help.aliyun.com/zh/viapi/use-cases/video-subtitles-erasure-1)
- [文件 URL 处理](https://help.aliyun.com/zh/viapi/getting-started/the-file-url-processing)
