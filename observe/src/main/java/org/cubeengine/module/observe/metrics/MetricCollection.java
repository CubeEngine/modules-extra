package org.cubeengine.module.observe.metrics;

import org.cubeengine.module.observe.metrics.meter.Counter;
import org.cubeengine.module.observe.metrics.meter.Gauge;
import org.cubeengine.module.observe.metrics.meter.Histogram;
import org.cubeengine.module.observe.metrics.meter.Timer;

public interface MetricCollection {

    Counter newCounter(String[] name);
    Gauge newGauge(String[] name);
    Timer newTimer(String[] name);
    Histogram newHistogram(String[] name, double[] buckets);

    void subscribe(MetricSubscriber subscriber);
    void unsubscribe(MetricSubscriber subscriber);

    static String[] name(String... segments) {
        return segments;
    }

    static MetricCollection newCollection() {
        return new SimpleMetricCollection();
    }
}
