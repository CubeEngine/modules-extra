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
package org.cubeengine.module.stats;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.ClassLoadingExports;
import io.prometheus.client.hotspot.GarbageCollectorExports;
import io.prometheus.client.hotspot.MemoryPoolsExports;
import io.prometheus.client.hotspot.StandardExports;
import io.prometheus.client.hotspot.ThreadExports;
import io.prometheus.client.hotspot.VersionInfoExports;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Game;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.plugin.PluginContainer;

@Singleton
@Module
public class Stats
{
    @ModuleConfig private StatsConfig config;
    private MonitoringService monitoring;
    private ScheduledExecutorService scheduler;
    private Game game;
    private HTTPServer exporter;

    private static Gauge players = Gauge.build()
            .name("mc_players_total")
            .help("Total online and max players")
            .labelNames("state")
            .create();
    private static Gauge tps = Gauge.build()
            .name("mc_tps")
            .help("Tickrate")
            .create();
    private static Gauge loadedChunks = Gauge.build()
            .name("mc_loaded_chunks")
            .help("Chunks loaded per world")
            .labelNames("world")
            .create();
    private static Gauge playersOnline = Gauge.build()
            .name("mc_players_online")
            .help("Players currently online per world")
            .labelNames("world")
            .create();
    private static Gauge entities = Gauge.build()
            .name("mc_entities")
            .help("Entities loaded per world")
            .labelNames("world")
            .create();
    private static Gauge tileEntities = Gauge.build()
            .name("mc_tile_entities")
            .help("Entities loaded per world")
            .labelNames("world")
            .create();

    @Inject
    public Stats(PluginContainer plugin, MonitoringService monitoring) {
        this.monitoring = monitoring;
        this.scheduler = Sponge.getAsyncScheduler().createExecutor(plugin);
        this.game = Sponge.getGame();
    }

    @Listener
    public void onPreInit(StartedEngineEvent<Server> event)
    {
        final CollectorRegistry registry = monitoring.getRegistry();
        final InetSocketAddress bindAddr = new InetSocketAddress(config.bindAddress, config.bindPort);
        registerDefaults(registry);

        players.register(registry);
        tps.register(registry);
        loadedChunks.register(registry);
        playersOnline.register(registry);
        entities.register(registry);
        tileEntities.register(registry);

        try
        {
            this.exporter = new HTTPServer(bindAddr, registry, true);
            this.scheduler.scheduleAtFixedRate(this::sampleServer, config.samplingInterval.toMillis(), config.samplingInterval.toMillis(), TimeUnit.MILLISECONDS);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static void registerDefaults(CollectorRegistry registry)
    {
        registry.register(new StandardExports());
        registry.register(new MemoryPoolsExports());
        registry.register(new GarbageCollectorExports());
        registry.register(new ThreadExports());
        registry.register(new ClassLoadingExports());
        registry.register(new VersionInfoExports());
    }

    private void sampleServer()
    {
        final Server server = game.getServer();
        players.labels("online").set(server.getOnlinePlayers().size());
        players.labels("max").set(server.getMaxPlayers());

        for (ServerWorld world : server.getWorldManager().worlds())
        {
            final String worldName = world.getProperties().getKey().asString();
            loadedChunks.labels(worldName).set(Iterables.size(world.getLoadedChunks()));
            playersOnline.labels(worldName).set(world.getPlayers().size());
            entities.labels(worldName).set(world.getEntities().size());
            tileEntities.labels(worldName).set(world.getBlockEntities().size());
        }


        tps.set(server.getTicksPerSecond());
    }

    @Listener
    public void onStop(StoppingEngineEvent<Server> e)
    {
        this.exporter.stop();
    }
}
