package org.cubeengine.module.observe.metrics.meter;

public interface Counter {
    void inc(double by);

    default void inc() {
        inc(1.0);
    }
}
