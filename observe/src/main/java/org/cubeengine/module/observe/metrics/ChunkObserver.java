/*
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
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
        updateLoadedChunkCount(e.worldKey());
    }

    @Listener
    public void onChunkLoaded(ChunkEvent.Unload e) {
        updateLoadedChunkCount(e.worldKey());
    }

    private void updateLoadedChunkCount(ResourceKey worldKey) {
        Sponge.server().worldManager().world(worldKey).ifPresent(world -> {
            LOADED_CHUNKS.set(StreamSupport.stream(world.loadedChunks().spliterator(), false).count(), worldKey);
        });
    }
}
