package org.cubeengine.module.observe.metrics;

import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.world.chunk.ChunkEvent;
import org.spongepowered.observer.metrics.Meter;
import org.spongepowered.observer.metrics.meter.Gauge;

import java.util.stream.StreamSupport;

public class ChunkObserver {
    private static final Gauge LOADED_CHUNKS = Meter.newGauge()
            .name("sponge_world_loaded_chunk_count")
            .help("Chunks loaded per world")
            .labelNames("world")
            .build();

    @Listener
    public void onChunkLoaded(ChunkEvent.Load e) {
        updateLoadedChunkCount(e.chunkWorld());
    }

    @Listener
    public void onChunkLoaded(ChunkEvent.Unload e) {
        updateLoadedChunkCount(e.chunkWorld());
    }

    private void updateLoadedChunkCount(ResourceKey worldKey) {
        Sponge.server().worldManager().world(worldKey).ifPresent(world -> {
            LOADED_CHUNKS.set(StreamSupport.stream(world.loadedChunks().spliterator(), false).count(), worldKey);
        });
    }
}
