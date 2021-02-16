package org.cubeengine.module.observe.metrics;

public interface MetricSubscriber {
    void onCounterIncrement(String[] name, double incrementedBy);
    void onGaugeSet(String[] name, double value);
    void onTimerObserved(String[] name, double seconds);
    void onHistogramObserved(String[] name, double[] buckets, double value);
}
