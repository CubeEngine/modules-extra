package org.cubeengine.module.observe.metrics;

import org.spongepowered.observer.metrics.Meter;
import org.spongepowered.observer.metrics.meter.Gauge;

public class PerformanceObserver {
    private static final Gauge TPS = Meter.newGauge()
            .labelNames("sponge_server_tps")
            .help("Server tick rate")
            .build();

    private static final Gauge AVERAGE_TICK_TIME = Meter.newGauge()
            .name("sponge_server_avg_tick_time_millis")
            .help("Tick time averaged over the last 100 ticks")
            .build();
}
