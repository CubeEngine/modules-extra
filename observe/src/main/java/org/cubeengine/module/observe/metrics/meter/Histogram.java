package org.cubeengine.module.observe.metrics.meter;

public interface Histogram {
    void observe(double seconds);
}
