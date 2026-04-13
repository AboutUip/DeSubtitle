-- 只读参考；应用不写回。逐项键注释要求见 docs/restriction/code-conventions.md §4。

return {
  -- 每个匿名用户（JWT 的 sub）允许成功完成的视频上传次数上限；超限则 POST /uploadVideo 返回 409。
  max_videos_per_user = 3,
  -- 已上传视频在 data/videos/ 中保留的分钟数；到期后由 VideoLifecycleRecorder 删除文件与 user_videos 行；指标 videoExpiresInSeconds 显示剩余秒数。
  video_retention_minutes = 5,
  -- 去字幕完成后写入 data/desubtitle/ 的成品在本地保留的分钟数；到期后删文件并清空 user_videos 中输出列。
  desubtitle_output_retention_minutes = 10,
  -- POST /sendToDeSubtitle 对单条任务轮询 GetAsyncJobResult 的最长等待（分钟），超时则记为 TIMEOUT 类失败并可下次重试。
  desubtitle_poll_timeout_minutes = 10,
}
