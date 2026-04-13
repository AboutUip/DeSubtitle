# 输入输出、限制与合规

## 1. 输入视频限制（EraseVideoSubtitles）

| 项 | 约束 |
|------|------|
| 容器/格式 | **MP4** |
| 文件大小 | **≤ 1 GB** |
| 分辨率 | **≤ 1080P** |
| `VideoUrl` | **URL 中不得包含中文字符**（路径、查询串均需注意编码） |

## 2. 字幕类型与效果预期

- **较适合**：常规中文/英文、对比度较高的**纯色类**字幕（如常见影视对白字幕）。
- **较弱**：复杂花体、极细笔画、与艺术字混排等。

厂商说明：若字幕字号过大，擦除效果可能下降。具体以试用与线上表现为准。

## 3. URL 与地域

- **推荐**：使用 **华东 2（上海）** 地域 **OSS** 生成的可访问链接作为 `VideoUrl`，降低跨域与访问策略问题概率。
- **本地或非上海 OSS**：需按 [文件 URL 处理](https://help.aliyun.com/zh/viapi/getting-started/the-file-url-processing) 处理可达性与格式要求；或使用 SDK **Advance** 流式上传，避免先暴露不稳定 URL。

## 4. 输出结果形态与时效

- 任务成功后，在 **GetAsyncJobResult** 的 **`Data.Result`**（JSON 字符串）中解析得到 **`VideoUrl`**（EraseVideoSubtitles 场景）。
- 该 **`VideoUrl` 为临时地址**，官方说明有效期约 **30 分钟**；过期后不可再下载。
- **工程要求**：在有效期内由 DeSubtitle 服务端或任务 Worker **下载并转存**到自有 OSS、对象存储或文件系统，再对业务用户发放长期或受控短期链接。

## 5. 异步任务相关文件时效（查询侧说明）

GetAsyncJobResult 文档指出：异步任务关联的产出文件存在**过期时间（文档示例为 30 分钟量级）**，长期保存须及时下载或写入 OSS。与上条合并理解：**轮询间隔与总超时**应小于业务可接受的数据丢失风险窗口。

## 6. 合规与安全

- 上传与处理的视频须来源合法、符合适用法律法规与服务条款。
- **AccessKey** 不得写入仓库；使用环境变量或密钥管理服务。
- 体验/调试上传的临时文件有自动清理策略（官方说明含 1 小时/24 小时等表述），**生产环境勿依赖**临时调试存储。

## 7. 计费

EraseVideoSubtitles 为**付费能力**（具体计价见官方计费文档）。应用层应记录 `RequestId`/`JobId` 便于对账与排障。

## 8. 参考

- [EraseVideoSubtitles 输入限制与返回说明](https://help.aliyun.com/zh/viapi/developer-reference/api-t470ol)
- [GetAsyncJobResult 功能描述](https://help.aliyun.com/zh/viapi/developer-reference/api-querying-asynchronous-task-results)
