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
package org.cubeengine.module.observe;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.prometheus.client.hotspot.ClassLoadingExports;
import io.prometheus.client.hotspot.GarbageCollectorExports;
import io.prometheus.client.hotspot.MemoryPoolsExports;
import io.prometheus.client.hotspot.StandardExports;
import io.prometheus.client.hotspot.ThreadExports;
import io.prometheus.client.hotspot.VersionInfoExports;
import org.apache.logging.log4j.Logger;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Server;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.plugin.PluginContainer;

@Singleton
@Module
public class Observe
{
    @ModuleConfig private ObserveConfig config;
    private final PluginContainer plugin;
    private PrometheusMetricsService metricsService;
    private final Logger logger;
    private final ThreadFactory tf;
    private final TaskManager tm;
    private ServerBootstrap bootstrap = null;
    private NioEventLoopGroup eventLoopGroup = null;
    private Channel channel = null;

    @Inject
    public Observe(PluginContainer plugin, Logger logger, ThreadFactory tf, TaskManager tm) {
        this.plugin = plugin;
        this.logger = logger;
        this.tf = tf;
        this.tm = tm;
    }

    @Listener
    public void onPreInit(StartedEngineEvent<Server> event)
    {
        this.metricsService = new PrometheusMetricsService(tf, tm, new InetSocketAddress(config.bindAddress, config.bindPort), logger);
        metricsService.registerAsync(plugin, new StandardExports());
        metricsService.registerAsync(plugin, new MemoryPoolsExports());
        metricsService.registerAsync(plugin, new GarbageCollectorExports());
        metricsService.registerAsync(plugin, new ThreadExports());
        metricsService.registerAsync(plugin, new ClassLoadingExports());
        metricsService.registerAsync(plugin, new VersionInfoExports());

        metricsService.registerSync(plugin, new SpongeCollector(event.getEngine(), plugin));

        metricsService.startExporter();
    }

    @Listener
    public void onStop(StoppingEngineEvent<Server> e)
    {
        metricsService.stopExporter();
    }
}
