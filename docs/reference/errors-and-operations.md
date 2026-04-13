# 错误处理与运维要点

## 1. 分层错误模型

| 层次 | 典型表现 | DeSubtitle 处理 |
|------|-----------|-----------------|
| **传输/HTTP** | 4xx/5xx、TLS、DNS | 记录 trace id；可重试的限流/5xx 使用退避重试 |
| **OpenAPI 业务错误** | 响应体含错误码/消息（各 SDK 封装形式不同） | 映射为用户可读文案 + 内部错误码入库 |
| **异步任务失败** | `GetAsyncJobResult.Data.Status` 为 `PROCESS_FAILED` 等 | 见第 2 节 |
| **应用逻辑** | 输入未过校验、转存失败 | 在提交阿里云前拦截；转存失败单独告警 |

## 2. 异步状态与业务映射

| `Status` | 对用户 | 对运维 |
|----------|--------|--------|
| `QUEUING` / `PROCESSING` | 处理中 | 正常轮询 |
| `PROCESS_SUCCESS` | 成功（待转存完成后可下载） | 解析 `Result`，触发转存流水线 |
| `PROCESS_FAILED` | 失败 | 记录 `ErrorCode`/`ErrorMessage` |
| `TIMEOUT_FAILED` | 超时 | 可考虑人工重试或缩短视频 |
| `LIMIT_RETRY_FAILED` | 稍后重试 | 按官方说明重新提交擦除任务并限制总重试次数 |

## 3. 幂等与重复提交

- **同一输入**在任务未完成时**不应**重复调用 EraseVideoSubtitles。
- 业务层可用「业务单号 → `JobId`」唯一索引；用户重复点击时返回已有任务状态。

## 4. 日志与审计字段

建议每条阿里云调用至少记录：

- `RequestId`（提交、查询各自独立）
- `JobId`（擦除任务标识）
- 业务 `task_id` / `user_id`
- 视频源类型（URL / Advance）、是否自定义 `BX/BY/BW/BH`
- 终态 `Status` 与解析后的输出存储路径（脱敏）

## 5. 监控指标（示例）

- 提交成功率、查询成功率
- 从提交到 `PROCESS_SUCCESS` 的 **P50/P95 耗时**
- `PROCESS_FAILED` / `TIMEOUT_FAILED` 占比
- 转存失败率、临时 `VideoUrl` 下载 4xx/5xx 次数

## 6. 计费与配额

- EraseVideoSubtitles 为付费接口；控制台可查用量与账单。
- 异常重试会增加调用次数，需在成本模型中计入。

## 7. 错误码权威来源

具体 HTTP/`Code` 枚举以官方 **常见错误码** 为准；实现时不要在文档中硬编码未经验证的码表，宜在运行时记录原始响应以便对照更新。

## 8. 参考

- [EraseVideoSubtitles](https://help.aliyun.com/zh/viapi/developer-reference/api-t470ol)
- [GetAsyncJobResult](https://help.aliyun.com/zh/viapi/developer-reference/api-querying-asynchronous-task-results)
