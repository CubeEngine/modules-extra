package org.cubeengine.module.observe.metrics;

import org.spongepowered.observer.metrics.Meter;
import org.spongepowered.observer.metrics.meter.Gauge;

public class EntityObserver {

    private static final Gauge ENTITIES = Meter.newGauge()
            .name("sponge_world_entity_count")
            .help("Entities loaded per world")
            .build();

    private static final Gauge BLOCK_ENTITIES = Meter.newGauge()
            .name("sponge_world_block_entity_count")
            .help("Block entities loaded per world")
            .build();
}
