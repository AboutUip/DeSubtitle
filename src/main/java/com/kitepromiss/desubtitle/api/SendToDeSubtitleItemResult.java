package com.kitepromiss.desubtitle.api;

/**
 * {@code POST /sendToDeSubtitle} 中单个源视频的处理结果。
 *
 * @param outcome {@code success}、{@code skipped}、{@code failed}、{@code timeout}
 */
public record SendToDeSubtitleItemResult(
        String videoId,
        String outcome,
        String aliyunStatus,
        String storedOutputFileName,
        Long outputExpiresAtEpochMillis,
        String error) {}
