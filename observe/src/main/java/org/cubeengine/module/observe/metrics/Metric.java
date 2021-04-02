package org.cubeengine.module.observe.metrics;

import org.cubeengine.module.observe.metrics.meter.Counter;
import org.cubeengine.module.observe.metrics.meter.Gauge;
import org.cubeengine.module.observe.metrics.meter.Histogram;
import org.cubeengine.module.observe.metrics.meter.Timer;

public final class Metric {

    public static final SimpleMetricCollection DEFAULT = new SimpleMetricCollection();

    private Metric() {
    }

    static Counter.Builder newCounter() {
        return DEFAULT.newCounter();
    }

    static Gauge.Builder newGauge() {
        return DEFAULT.newGauge();
    }

    static Timer.Builder newTimer() {
        return DEFAULT.newTimer();
    }

    static Histogram.Builder newHistogram() {
        return DEFAULT.newHistogram();
    }

}
