package com.kitepromiss.desubtitle.indicator;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

import org.springframework.stereotype.Component;

/**
 * 进程内线程安全的计数器与量表；仅堆内存，进程退出即丢失。
 */
@Component
public class InMemoryIndicatorRegistry implements IndicatorRecorder {

    private final ConcurrentHashMap<String, LongAdder> counters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicReference<Double>> gauges = new ConcurrentHashMap<>();

    private static boolean usableName(String name) {
        return name != null && !name.isBlank();
    }

    @Override
    public void incrementCounter(String name) {
        incrementCounter(name, 1L);
    }

    @Override
    public void incrementCounter(String name, long delta) {
        if (!usableName(name)) {
            return;
        }
        counters.computeIfAbsent(name, k -> new LongAdder()).add(delta);
    }

    @Override
    public void setGauge(String name, double value) {
        if (!usableName(name)) {
            return;
        }
        gauges.computeIfAbsent(name, k -> new AtomicReference<>(0.0)).set(value);
    }

    @Override
    public IndicatorSnapshot snapshot() {
        Map<String, Long> c = new TreeMap<>();
        counters.forEach((k, adder) -> c.put(k, adder.sum()));
        Map<String, Double> g = new TreeMap<>();
        gauges.forEach((k, ref) -> g.put(k, ref.get()));
        return new IndicatorSnapshot(
                Map.copyOf(c), Map.copyOf(g), Map.of(), List.of(), System.currentTimeMillis());
    }
}
