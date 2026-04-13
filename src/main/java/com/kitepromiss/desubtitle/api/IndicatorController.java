package com.kitepromiss.desubtitle.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kitepromiss.desubtitle.indicator.IndicatorSnapshot;
import com.kitepromiss.desubtitle.indicator.IndicatorSnapshotService;

/**
 * 对外返回纯内存指标的只读快照（JSON）。
 */
@RestController
public class IndicatorController {

    private final IndicatorSnapshotService indicatorSnapshotService;

    public IndicatorController(IndicatorSnapshotService indicatorSnapshotService) {
        this.indicatorSnapshotService = indicatorSnapshotService;
    }

    /** 无查询参数、无请求体；正文为 {@link IndicatorSnapshot} 的 JSON。 */
    @GetMapping("/getIndicator")
    public IndicatorSnapshot getIndicator() {
        return indicatorSnapshotService.combinedSnapshot();
    }
}
