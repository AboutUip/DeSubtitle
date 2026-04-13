package com.kitepromiss.desubtitle.indicator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

class InMemoryIndicatorRegistryTest {

    @Test
    void aggregatesConcurrentIncrements() throws Exception {
        InMemoryIndicatorRegistry reg = new InMemoryIndicatorRegistry();
        int threads = 8;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    start.await();
                    for (int j = 0; j < 1000; j++) {
                        reg.incrementCounter("hits");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        done.await();
        IndicatorSnapshot s = reg.snapshot();
        assertEquals(8000L, s.counters().get("hits").longValue());
        assertTrue(s.capturedAtEpochMillis() > 0);
        assertTrue(s.videoLifecycles().isEmpty());
    }

    @Test
    void gaugeAndSnapshotOrder() {
        InMemoryIndicatorRegistry reg = new InMemoryIndicatorRegistry();
        reg.setGauge("qps", 1.5);
        reg.incrementCounter("b", 2);
        reg.incrementCounter("a", 1);
        IndicatorSnapshot s = reg.snapshot();
        assertEquals(1L, s.counters().get("a").longValue());
        assertEquals(2L, s.counters().get("b").longValue());
        assertEquals(1.5, s.gauges().get("qps"), 0.001);
        assertTrue(s.videoExpiresInSeconds().isEmpty());
        assertTrue(s.videoLifecycles().isEmpty());
    }

    @Test
    void ignoresBlankNames() {
        InMemoryIndicatorRegistry reg = new InMemoryIndicatorRegistry();
        reg.incrementCounter(null);
        reg.incrementCounter("   ");
        reg.setGauge("", 1.0);
        assertTrue(reg.snapshot().counters().isEmpty());
        assertTrue(reg.snapshot().gauges().isEmpty());
    }
}
