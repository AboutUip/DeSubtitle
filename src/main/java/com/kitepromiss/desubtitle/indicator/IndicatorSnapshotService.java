package com.kitepromiss.desubtitle.indicator;

import org.springframework.stereotype.Service;

import com.kitepromiss.desubtitle.video.VideoLifecycleRecorder;

/**
 * 合并 {@link IndicatorRecorder} 与上传视频生命周期剩余时间，供 {@code GET /getIndicator} 与 {@code GET /life} 使用。
 */
@Service
public class IndicatorSnapshotService {

    private final IndicatorRecorder indicatorRecorder;
    private final VideoLifecycleRecorder videoLifecycleRecorder;

    public IndicatorSnapshotService(IndicatorRecorder indicatorRecorder, VideoLifecycleRecorder videoLifecycleRecorder) {
        this.indicatorRecorder = indicatorRecorder;
        this.videoLifecycleRecorder = videoLifecycleRecorder;
    }

    public IndicatorSnapshot combinedSnapshot() {
        long capturedAt = System.currentTimeMillis();
        IndicatorSnapshot base = indicatorRecorder.snapshot();
        return new IndicatorSnapshot(
                base.counters(),
                base.gauges(),
                videoLifecycleRecorder.secondsUntilExpiryAtMillis(capturedAt),
                videoLifecycleRecorder.videoLifecyclesAtMillis(capturedAt),
                capturedAt);
    }
}
