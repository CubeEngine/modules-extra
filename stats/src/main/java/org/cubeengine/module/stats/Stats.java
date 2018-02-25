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

import com.google.common.collect.Iterables;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.*;
import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.service.MonitoringService;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Game;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.living.humanoid.player.KickPlayerEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppedEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.world.World;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

@Singleton
@Module
public class Stats extends CubeEngineModule
{
    @ModuleConfig private StatsConfig config;
    @Inject private PluginContainer plugin;
    @Inject private MonitoringService monitoring;
    @Inject private Scheduler scheduler;
    @Inject private Game game;
    private HTTPServer exporter;

    private Gauge players;
    private Gauge tps;
    private Gauge loadedChunks;
    private Gauge playersOnline;
    private Gauge entities;
    private Gauge tileEntities;


    @Listener
    public void onPreInit(GamePreInitializationEvent event)
    {
        final CollectorRegistry registry = monitoring.getRegistry();
        final InetSocketAddress bindAddr = new InetSocketAddress(config.bindAddress, config.bindPort);
        registerDefaults(registry);

        this.players = Gauge.build()
                .name("mc_players_total")
                .help("Total online and max players")
                .labelNames("state")
                .create()
                .register(registry);

        this.tps = Gauge.build()
                .name("mc_tps")
                .help("Tickrate")
                .labelNames("state")
                .create()
                .register(registry);

        this.loadedChunks = Gauge.build()
                .name("mc_loaded_chunks")
                .help("Chunks loaded per world")
                .labelNames("world")
                .create()
                .register(registry);

        this.playersOnline = Gauge.build()
                .name("mc_players_online")
                .help("Players currently online per world")
                .labelNames("world")
                .create()
                .register(registry);

        this.entities = Gauge.build()
                .name("mc_entities")
                .help("Entities loaded per world")
                .labelNames("world")
                .create()
                .register(registry);

        this.tileEntities = Gauge.build()
                .name("mc_tile_entities")
                .help("Entities loaded per world")
                .labelNames("world")
                .create()
                .register(registry);

        try
        {
            this.exporter = new HTTPServer(bindAddr, registry, true);
            this.scheduler.createAsyncExecutor(this.plugin).scheduleAtFixedRate(this::sampleServer, config.samplingInterval.toMillis(), config.samplingInterval.toMillis(), TimeUnit.MILLISECONDS);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static void registerDefaults(CollectorRegistry registry)
    {
        new StandardExports().register(registry);
        new MemoryPoolsExports().register(registry);
        new GarbageCollectorExports().register(registry);
        new ThreadExports().register(registry);
        new ClassLoadingExports().register(registry);
        new VersionInfoExports().register(registry);
    }

    private void sampleServer()
    {
        final Server server = game.getServer();
        players.labels("online").set(server.getOnlinePlayers().size());
        players.labels("max").set(server.getMaxPlayers());

        for (World world : server.getWorlds())
        {
            loadedChunks.labels(world.getName()).set(Iterables.size(world.getLoadedChunks()));
            playersOnline.labels(world.getName()).set(world.getPlayers().size());
            entities.labels(world.getName()).set(world.getEntities().size());
            tileEntities.labels(world.getName()).set(world.getTileEntities().size());
        }


        tps.labels("tps").set(server.getTicksPerSecond());
    }

    @Listener
    public void onStop(GameStoppedEvent e)
    {
        this.exporter.stop();
    }
}
