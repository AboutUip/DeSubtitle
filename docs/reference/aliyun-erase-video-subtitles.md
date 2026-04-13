# EraseVideoSubtitles API 参考

本文描述阿里云视觉智能开放平台 **视频生产（videoenhan）** 类目下 **EraseVideoSubtitles** 的契约要点，供 DeSubtitle 服务端或脚本直连时对照。字段以 OpenAPI 为准；SDK 会将字段映射为各语言风格（驼峰/蛇形）。

应用内 **「去字幕」** 在阿里云侧的第一步就是调用本接口；第二步必须用 **GetAsyncJobResult** 查询结果（见 [aliyun-async-job-result.md](./aliyun-async-job-result.md)）。

**官方文档**：[调用 EraseVideoSubtitles API 擦除视频字幕](https://help.aliyun.com/zh/viapi/developer-reference/api-t470ol)

## 1. 接口性质

- **异步接口**：本接口成功响应**不**直接返回处理后视频二进制或最终持久 URL 的业务完成态；须再调 **GetAsyncJobResult**（见 [aliyun-async-job-result.md](./aliyun-async-job-result.md)）。
- **Action**：固定为 `EraseVideoSubtitles`。

## 2. Endpoint

- **HTTPS 主机名**：`videoenhan.cn-shanghai.aliyuncs.com`
- **Region**：`cn-shanghai`

与 SDK 中 `Config.endpoint` / `region_id` 配置一致。

## 3. 请求参数

| 名称 | 类型 | 必选 | 说明 |
|------|------|------|------|
| **Action** | String | 是 | 固定 `EraseVideoSubtitles` |
| **VideoUrl** | String | 是 | 输入视频地址；推荐上海 OSS；编码与中文限制见 [constraints-and-storage.md](./constraints-and-storage.md) |
| **BX** | Float | 否 | 归一化左上角 x，见 [subtitle-region.md](./subtitle-region.md) |
| **BY** | Float | 否 | 归一化左上角 y |
| **BW** | Float | 否 | 归一化宽度 |
| **BH** | Float | 否 | 归一化高度 |

### 3.1 HTTP 查询示例（示意）

```
https://videoenhan.cn-shanghai.aliyuncs.com/?Action=EraseVideoSubtitles
  &VideoUrl=<编码后的URL>
  &BX=0.0
  &BY=0.75
  &BW=1.0
  &BH=0.25
  &<公共请求参数>
```

实际调用应使用 **签名后的完整 URL** 或 **SDK**，避免手写签名。

## 4. 响应（提交成功时）

以下为文档给出的典型 JSON 形态；不同 SDK 可能将 body 再包一层。

```json
{
  "RequestId": "CCB082BF-A6B1-4C28-9E49-562EEE7DE639",
  "Message": "该调用为异步调用，任务已提交成功，请以requestId的值作为jobId参数调用同类目下GetAsyncJobResult接口查询任务执行状态和结果。"
}
```

### 4.1 字段语义

| 字段 | 说明 |
|------|------|
| **RequestId** | 本次提交请求 ID；在异步模型中同时作为 **GetAsyncJobResult** 的 **`JobId`** 使用（与官方 EraseVideoSubtitles 文档表述一致） |
| **Message** | 人类可读提示，说明需异步查询 |

**注意**：部分文档表格中列出的 `Data`/`VideoUrl` 描述的是**任务完成后**经 **GetAsyncJobResult** 解析得到的业务数据，**不要**与上述立即响应混淆。

## 5. 与 SDK 方法名的对应关系

| 场景 | 典型 SDK 请求类型 | 典型方法后缀 |
|------|-------------------|----------------|
| 仅传 URL | `EraseVideoSubtitlesRequest` | `eraseVideoSubtitlesWithOptions` / `EraseVideoSubtitlesWithOptions` 等 |
| 传文件流/本地 | `EraseVideoSubtitlesAdvanceRequest` | `eraseVideoSubtitlesAdvance` / `EraseVideoSubtitlesAdvance` 等 |

详见 [sdk-endpoint-auth.md](./sdk-endpoint-auth.md)。

## 6. 错误码

常见与参数、权限、配额相关的错误码以官方 **常见错误码** 列表为准；应用层应对 HTTP 状态、业务 `Code`/`Message` 做结构化日志。

## 7. 参考链接

- [EraseVideoSubtitles 官方 API 文档](https://help.aliyun.com/zh/viapi/developer-reference/api-t470ol)
- [SDK 示例（多语言）](https://help.aliyun.com/zh/viapi/use-cases/video-subtitles-erasure-1)
