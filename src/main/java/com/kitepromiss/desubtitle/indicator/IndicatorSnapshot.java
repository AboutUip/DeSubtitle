package com.kitepromiss.desubtitle.indicator;

import java.util.List;
import java.util.Map;

/**
 * {@link #capturedAtEpochMillis()} 为服务端生成快照时的毫秒时间戳（{@link System#currentTimeMillis()}）。
 *
 * <p>{@link #videoExpiresInSeconds()}：当前仍存在于库中的上传视频 id → 距离该视频过期时刻的剩余整秒数（已到期待清理的为 0）。
 *
 * <p>{@link #videoLifecycles()}：与快照时刻对齐的每条视频结构化生命周期（按 {@code videoId} 字典序）。
 */
public record IndicatorSnapshot(
        Map<String, Long> counters,
        Map<String, Double> gauges,
        Map<String, Long> videoExpiresInSeconds,
        List<VideoLifecycleDetail> videoLifecycles,
        long capturedAtEpochMillis) {}
