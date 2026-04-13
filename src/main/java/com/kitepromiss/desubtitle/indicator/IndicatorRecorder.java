package com.kitepromiss.desubtitle.indicator;

/**
 * 纯内存指标记录入口，供业务代码注入后调用；无磁盘、无 SQLite。
 */
public interface IndicatorRecorder {

    /** 计数器 +1；{@code name} 为空或空白时忽略。 */
    void incrementCounter(String name);

    /**
     * 计数器增加 {@code delta}（可为负，用于「减少」语义）；{@code name} 为空或空白时忽略。
     */
    void incrementCounter(String name, long delta);

    /** 覆盖型量表；{@code name} 为空或空白时忽略。 */
    void setGauge(String name, double value);

    /** 当前内存中的指标快照（调用时刻）。 */
    IndicatorSnapshot snapshot();
}
