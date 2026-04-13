# 系统概述与处理流水线

## 1. DeSubtitle 在工程上的含义

**DeSubtitle** 在本仓库语境下指：将**用户侧视频**送入阿里云视觉智能开放平台 **视频生产（videoenhan）** 能力 **EraseVideoSubtitles**，在厂商约束内擦除指定归一化矩形内的字幕，并产出**可下载的处理后视频**（通常为临时 URL）。

产品与界面文案中的 **「去字幕」** 即上述整条流水线（提交 EraseVideoSubtitles → 轮询 GetAsyncJobResult → 取 `VideoUrl` 并转存），与「仅检测字幕轨」或第三方剪辑语义不同。

本系统不负责训练模型；算法行为、配额与计费以阿里云侧为准。

## 2. 核心依赖（云服务）

| 依赖 | 作用 |
|------|------|
| **EraseVideoSubtitles** | 提交异步擦除任务 |
| **GetAsyncJobResult** | 用任务标识查询状态与真实结果 |
| **（可选）对象存储 OSS** | 输入侧推荐使用上海地域可访问 URL；输出侧需在临时链接过期前转存 |

凭证使用 **AccessKey**（强烈建议 RAM 子账号），并授予视觉智能相关权限（如 `AliyunVIAPIFullAccess`，以控制台/RAM 策略为准）。

## 3. 异步两步模型（必须实现）

```
客户端/服务  --(1) EraseVideoSubtitles-->  阿里云（入队/处理）
                    <-- RequestId + Message

客户端/服务  --(2) GetAsyncJobResult(JobId=RequestId)-->  阿里云
                    <-- Data.Status, Data.Result ...
```

要点：

1. **第一步**成功仅表示任务已受理，响应体中的 **`RequestId`** 即后续查询用的 **`JobId`**（与 EraseVideoSubtitles 文档表述一致）。
2. **第二步**在 `Status` 为终态前可能需**轮询**；同一任务未完成时不应重复提交擦除任务。
3. **`Data.Result`** 为 **JSON 字符串**，需二次解析；EraseVideoSubtitles 成功时解析结果中含输出 **`VideoUrl`**（字段名以实际 `Result` 为准）。

## 4. 视频来源两条路径

| 路径 | 适用 | 说明 |
|------|------|------|
| **URL 模式** | 已有公网可访问地址，且符合 URL 与地域建议 | 请求体传 `VideoUrl`；厂商推荐 **上海 OSS** 链接 |
| **流式 Advance 模式** | 本地文件或需 SDK 直传二进制 | 各语言 SDK 提供 `EraseVideoSubtitlesAdvance`（或同名）系列，将视频作为流/文件对象上传；具体类型名见 [sdk-endpoint-auth.md](./sdk-endpoint-auth.md) |

非上海 OSS、本地文件、URL 含特殊字符等场景，须对照 [文件 URL 处理](https://help.aliyun.com/zh/viapi/getting-started/the-file-url-processing) 做预处理。

## 5. DeSubtitle 应用层建议流水线

以下为典型服务端编排，可按产品形态裁剪。

1. **接入校验**：格式 MP4、大小 ≤ 1GB、分辨率 ≤ 1080P；URL 不含中文（见 [constraints-and-storage.md](./constraints-and-storage.md)）。
2. **（可选）归一化区域**：未指定时使用默认底部带；自定义时传 `BX/BY/BW/BH`（见 [subtitle-region.md](./subtitle-region.md)）。
3. **提交任务**：调用 EraseVideoSubtitles，持久化 `RequestId` 与业务单号映射。
4. **轮询查询**：调用 GetAsyncJobResult，直到 `PROCESS_SUCCESS` / `PROCESS_FAILED` / `TIMEOUT_FAILED` / `LIMIT_RETRY_FAILED` 等终态（见 [aliyun-async-job-result.md](./aliyun-async-job-result.md)）。
5. **结果处理**：解析 `Result` 得到 `VideoUrl`；在**时效内**拉取并写入自有存储（OSS/S3/本地），再把长期地址返回给前端。

## 6. 实现约束摘要

- 业务层状态机（已提交 / 处理中 / 成功 / 失败）以 **GetAsyncJobResult 的 `Status`** 为准，不能仅以提交接口 HTTP 200 作为完成依据。
- 对用户分发的播放或下载地址，默认以**自有存储转存后**的 URL 为准；若直接使用厂商 `VideoUrl`，须明确告知临时性与过期时间。
