package com.kitepromiss.desubtitle.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.kitepromiss.desubtitle.indicator.InMemoryIndicatorRegistry;
import com.kitepromiss.desubtitle.indicator.IndicatorSnapshot;
import com.kitepromiss.desubtitle.indicator.IndicatorSnapshotService;
import com.kitepromiss.desubtitle.video.UserVideoRepository;
import com.kitepromiss.desubtitle.video.VideoLifecycleRecorder;
import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

class IndicatorControllerTest {

    @Test
    void returnsSnapshotJsonShape() {
        InMemoryIndicatorRegistry reg = new InMemoryIndicatorRegistry();
        reg.incrementCounter("x", 3);
        reg.setGauge("g", 2.0);
        WorkspacePaths paths =
                new WorkspacePaths(
                        Path.of("r.json"),
                        Path.of("m.lua"),
                        Path.of("data"),
                        Path.of("a.json"),
                        Path.of("ut.lua"),
                        Path.of("vu.lua"));
        VideoLifecycleRecorder lifecycle = new VideoLifecycleRecorder(emptyUserVideoRepository(), paths);
        IndicatorSnapshotService snapshotService = new IndicatorSnapshotService(reg, lifecycle);
        IndicatorSnapshot s = new IndicatorController(snapshotService).getIndicator();
        assertEquals(3L, s.counters().get("x").longValue());
        assertEquals(2.0, s.gauges().get("g"), 0.001);
        assertTrue(s.videoExpiresInSeconds().isEmpty());
        assertTrue(s.videoLifecycles().isEmpty());
    }

    @SuppressWarnings("unchecked")
    private static UserVideoRepository emptyUserVideoRepository() {
        return (UserVideoRepository)
                Proxy.newProxyInstance(
                        UserVideoRepository.class.getClassLoader(),
                        new Class<?>[] {UserVideoRepository.class},
                        (p, m, a) -> {
                            if ("findAll".equals(m.getName())) {
                                return List.of();
                            }
                            if (m.getReturnType() == long.class || m.getReturnType() == Long.class) {
                                return 0L;
                            }
                            if (m.getReturnType() == boolean.class) {
                                return false;
                            }
                            return null;
                        });
    }
}
