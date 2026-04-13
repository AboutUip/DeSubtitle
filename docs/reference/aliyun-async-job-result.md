# GetAsyncJobResult 与轮询策略

本文说明异步任务查询接口 **GetAsyncJobResult** 的输入输出及 DeSubtitle 建议的轮询语义。

**官方文档**：[调用异步接口后使用 GetAsyncJobResult 查询异步任务结果](https://help.aliyun.com/zh/viapi/developer-reference/api-querying-asynchronous-task-results)

## 0. 与「去字幕」的关系

用户在前端点击 **去字幕** 或调用 **`POST /sendVideoToDeSubtitle`** 后，服务端已用 **EraseVideoSubtitles** 提交过任务时，后续**唯一**依赖 **GetAsyncJobResult**（本文）推进状态：用提交接口返回的 **`RequestId`** 作为 **`JobId`** 轮询，直到 `PROCESS_SUCCESS` 等终态，再从 **`Data.Result`** 解析出 **`VideoUrl`** 拉取成品。缺少这一步则只有「已提交」而没有可播放/可下载的去字幕结果。

## 1. 与 EraseVideoSubtitles 的衔接

1. 调用 **EraseVideoSubtitles** 成功后，保存响应中的 **`RequestId`**。
2. 将该值作为 **GetAsyncJobResult** 的 **`JobId`** 参数传入（EraseVideoSubtitles 官方文档明确此对应关系）。
3. 在 **`Data.Status`** 进入终态前，按策略重复查询；**勿**对同一输入重复提交擦除任务。

## 2. Endpoint 选择

- **视频生产**类目 SDK 示例中，擦除与查询通常共用 **`videoenhan.cn-shanghai.aliyuncs.com`**。
- 若使用多产品统一客户端，以当前所用 SDK 包与 [Endpoint 列表](https://help.aliyun.com/document_detail/143103.html) 为准，保证 **提交与查询在同一套接入配置** 下可工作。

## 3. 请求参数

| 名称 | 类型 | 必选 | 说明 |
|------|------|------|------|
| **Action** | String | 是 | 固定 `GetAsyncJobResult` |
| **JobId** | String | 是 | 异步提交接口返回的 **`RequestId`** |

## 4. 响应结构（`Data`）

| 字段 | 类型 | 说明 |
|------|------|------|
| **Status** | String | 任务状态，见下节 |
| **JobId** | String | 任务侧 ID（与入参可能成对出现，以响应为准） |
| **Result** | String | **JSON 字符串**；成功时反序列化得到具体业务字段（EraseVideoSubtitles 场景下含 **`VideoUrl`**） |
| **ErrorMessage** | String | 失败时人类可读信息（存在时） |
| **ErrorCode** | String | 失败时错误码（存在时） |

### 4.1 `Status` 枚举（官方列出）

| Status | 含义 |
|--------|------|
| **QUEUING** | 排队中 |
| **PROCESSING** | 处理中 |
| **PROCESS_SUCCESS** | 成功 |
| **PROCESS_FAILED** | 失败 |
| **TIMEOUT_FAILED** | 超时未完成 |
| **LIMIT_RETRY_FAILED** | 超过最大重试次数；文档建议稍后重新调用算法接口再查 |

### 4.2 `Result` 反序列化（EraseVideoSubtitles）

成功且 `Status === PROCESS_SUCCESS` 时，将 **`Data.Result`** 按 UTF-8 字符串解析为 JSON 对象，典型包含：

```json
{
  "VideoUrl": "https://...signed-url..."
}
```

字段以实际返回为准；若厂商扩展字段，应前向兼容解析。

## 5. 轮询与退避建议

| 项目 | 建议 |
|------|------|
| 初始间隔 | 1～2 秒 |
| 退避 | 指数或线性增加，上限如 10～15 秒 |
| 总超时 | 结合产品 SLA 与视频时长设定；超时后标记任务失败并记录 `JobId` |
| 并发 | 多任务轮询时限制并发，避免触发流控 |

## 6. 失败分支处理

- **`PROCESS_FAILED` / `TIMEOUT_FAILED`**：向用户暴露通用失败信息；内部记录 `ErrorCode`/`ErrorMessage`。
- **`LIMIT_RETRY_FAILED`**：按文档建议，**重新提交** EraseVideoSubtitles（新 `RequestId`），再轮询；避免无限循环，设全局重试上限。

## 7. 与存储流水线的关系

一旦 `Result` 中取得 **`VideoUrl`**，须在**临时链接有效期内**完成下载与转存（见 [constraints-and-storage.md](./constraints-and-storage.md)）。

## 8. 参考

- [GetAsyncJobResult 官方文档](https://help.aliyun.com/zh/viapi/developer-reference/api-querying-asynchronous-task-results)
- [EraseVideoSubtitles 与异步说明](https://help.aliyun.com/zh/viapi/developer-reference/api-t470ol)
