package com.kitepromiss.desubtitle.api;

import com.kitepromiss.desubtitle.indicator.IndicatorSnapshot;

/**
 * {@code GET /life} 的 JSON 体：存活标记、提交 token 校验结果、当前有效 token（可能已刷新）、内存指标快照，
 * 以及前端应渲染的并行「视频去字幕」路数（由 {@code desubtitle.ui.video-processing-lanes} 配置，范围 1–8）。
 */
public record LifeStatusPayload(
        boolean alive,
        boolean submittedTokenValid,
        boolean tokenRefreshed,
        String token,
        long expiresInSeconds,
        String userId,
        IndicatorSnapshot indicators,
        int videoProcessingLanes) {}
