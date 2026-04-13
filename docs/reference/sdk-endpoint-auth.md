# SDK、Endpoint 与凭证

## 1. 凭证与环境变量

| 变量名 | 用途 |
|--------|------|
| **ALIBABA_CLOUD_ACCESS_KEY_ID** | AccessKey ID |
| **ALIBABA_CLOUD_ACCESS_KEY_SECRET** | AccessKey Secret |

要求：

- **禁止**将密钥写入仓库、镜像层或前端包。
- 生产环境优先 **RAM 子账号** + 最小权限；视觉智能场景常见策略名为 **`AliyunVIAPIFullAccess`**（以控制台实际策略为准）。

## 2. OpenAPI Endpoint 与 Region

| 项 | 值 |
|----|-----|
| **Endpoint** | `videoenhan.cn-shanghai.aliyuncs.com` |
| **Region** | `cn-shanghai` |

SDK 初始化时显式设置 `endpoint` 与 `region`（或语言等价字段），避免依赖错误默认值。

## 3. 各语言 SDK 包（videoenhan / 2020-03-20）

以下为官方示例中出现的包名，版本号以发布仓库为准。

| 语言 | 安装方式（示例） |
|------|------------------|
| Python | `pip install alibabacloud_videoenhan20200320` |
| Java | Maven `com.aliyun:videoenhan20200320` |
| Node.js | `npm install @alicloud/videoenhan20200320`（及 `@alicloud/openapi-client`、`@alicloud/tea-util` 等传递依赖） |
| Go | `github.com/alibabacloud-go/videoenhan-20200320/v3` |
| PHP | `composer require alibabacloud/videoenhan-20200320` |
| C# | NuGet `AlibabaCloud.SDK.Videoenhan20200320` |

完整列表见官方 **SDK 总览**。

## 4. 两种调用形态

### 4.1 URL 模式（`EraseVideoSubtitlesRequest`）

- 入参核心字段：**`VideoUrl`**。
- 方法名常见形式：`eraseVideoSubtitlesWithOptions`（Python）、`EraseVideoSubtitlesWithOptions`（C#/Java）等。
- 适用于输入已是**合规、可公网访问**的 MP4 地址（推荐上海 OSS）。

### 4.2 Advance 模式（`EraseVideoSubtitlesAdvanceRequest`）

- 入参核心字段：**`VideoUrlObject`**（或语言特定的 Stream / Readable / InputStream）。
- 方法名常见形式：`eraseVideoSubtitles_advance`（Python）、`eraseVideoSubtitlesAdvance`（Java）等。
- 适用于**本地路径**或**任意可下载 URL**：由应用在客户端读出字节流或流对象后交给 SDK 上传，减少「先自建 URL」的环节。

两种模式在**异步语义上一致**：返回仍须配合 **GetAsyncJobResult** 解析最终结果。

## 5. RuntimeOptions

各语言均传入 `RuntimeOptions`（或等价结构），用于超时、重试、代理等。DeSubtitle 建议：

- 为**提交**与**查询**分别设置合理 `readTimeout`；
- 长视频任务以**异步轮询**为主，勿将 HTTP 读超时与「任务总时长」混为一谈。

## 6. 调试与代码生成

- **OpenAPI Explorer**：可在线调试并生成带签名的示例代码（仍须自行保管密钥）。
- **钉钉群**：官方文档提供的视觉智能咨询群号 **23109592**（以官网最新说明为准）。

## 7. 与本应用引导流程的关系

若通过 **`POST /init/credentials`** 在应用内保存 AccessKey（SQLite 明文或调试模式下仅内存），存储形态与责任边界见 [aliyun-credentials-storage.md](./aliyun-credentials-storage.md)。

## 8. 参考

- [视频字幕擦除 SDK 示例](https://help.aliyun.com/zh/viapi/use-cases/video-subtitles-erasure-1)
- [EraseVideoSubtitles API](https://help.aliyun.com/zh/viapi/developer-reference/api-t470ol)
- [GetAsyncJobResult API](https://help.aliyun.com/zh/viapi/developer-reference/api-querying-asynchronous-task-results)
