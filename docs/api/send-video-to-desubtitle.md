# `POST /sendVideoToDeSubtitle` — 单条视频提交去字幕

| 项目 | 说明 |
|------|------|
| 作用 | 仅处理 **`videoId` 指定**且属于当前用户的一条记录，流程与 **`POST /sendToDeSubtitle`** 中单条处理相同（Advance 提交、轮询、下载至 **`data/desubtitle/`**）。与批量接口、**`DELETE /userVideo/{id}`** 共享 **`UserIdStripeLock`**，同一 `sub` 下互斥。 |
| 方法与路径 | `POST /sendVideoToDeSubtitle` |
| 请求体 | `application/json`：`{"videoId":"<uuid>","subtitlePosition":"bottom"}`（`SendVideoToDeSubtitleRequest`）。`subtitlePosition` 可选（小写英文）：`full`（全画面）、`upper_half`（上半屏）、`lower_half`（下半屏）、`top`、`upper`、`middle`、`lower`、`bottom`（与阿里云默认底部通栏一致）；缺省或空串视为 `bottom`。单条请求**不会**因已有成品而跳过，可改区域后重复提交。 |
| 请求头 | 初始化完成后须 **`Authorization: Bearer <JWT>`**；须已配置阿里云 AccessKey（同 [send-to-desubtitle.md](./send-to-desubtitle.md)）。 |
| 成功响应 | **200 OK**，体为 **`SendToDeSubtitleItemResult`**（与批量接口 `results[]` 元素相同）：`videoId`、`outcome`、`aliyunStatus`、`storedOutputFileName`、`outputExpiresAtEpochMillis`、`error`。 |
| **400** | `missing_video_id`（缺 `videoId` 或空白）。 |
| **401** | `missing_user_context` |
| **404** | `video_not_found`（见 [revoke-user-video.md](./revoke-user-video.md)）。 |
| **428** | `need_credentials` |
| 副作用与配置 | 同 [send-to-desubtitle.md](./send-to-desubtitle.md)；并递增 **`desubtitle_single_requests_user_<suffix>`**；成功落盘时递增 **`desubtitle_job_success_user_<suffix>`**。 |
| 实现锚点 | `SendToDeSubtitleController`、`SendVideoToDeSubtitleRequest`、`SubtitleVerticalPosition`、`SendToDeSubtitleService#sendSingleForUser`、`SubtitleEraseBand`、`DefaultVideoenhanSubtitleEraseOperations` |

目录索引见 [README.md](./README.md)。
