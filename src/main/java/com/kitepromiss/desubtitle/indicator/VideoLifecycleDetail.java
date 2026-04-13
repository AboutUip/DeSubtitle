package com.kitepromiss.desubtitle.indicator;

/**
 * 单条 {@code user_videos} 在快照时刻的生命周期视图（供 {@code GET /life} 的 {@code indicators}、{@code GET /myVideos} 等 JSON 序列化）。
 */
public record VideoLifecycleDetail(
        String videoId,
        String userId,
        String storedFileName,
        String originalFileName,
        String contentType,
        long sizeBytes,
        long createdAtEpochMillis,
        long uploadExpiresAtEpochMillis,
        long uploadExpiresInSeconds,
        String desubtitleJobId,
        String desubtitleLastStatus,
        String desubtitleError,
        String desubtitleOutputFileName,
        Long desubtitleOutputExpiresAtEpochMillis,
        Long desubtitleOutputExpiresInSeconds) {}
