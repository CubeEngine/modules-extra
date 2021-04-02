package org.cubeengine.module.observe.metrics;

import org.cubeengine.module.observe.metrics.meter.Counter;
import org.cubeengine.module.observe.metrics.meter.Gauge;
import org.cubeengine.module.observe.metrics.meter.Histogram;
import org.cubeengine.module.observe.metrics.meter.Timer;

public final class Metric {

    public static final SimpleMetricCollection DEFAULT = new SimpleMetricCollection();

    private Metric() {

    }


    Counter newCounter(String[] name, String help, String... labelNames) {
        return DEFAULT.newCounter(name, help, labelNames);
    }
    Gauge newGauge(String[] name, String help, String... labelNames) {
        return DEFAULT.newGauge(name, help, labelNames);
    }
    Timer newTimer(String[] name, String help, String... labelNames) {
        return DEFAULT.newTimer(name, help, labelNames);
    }
    Histogram newHistogram(String[] name, String help, double[] buckets, String... labelNames) {
        return DEFAULT.newHistogram(name, help, buckets, labelNames);
    }
}
