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

import java.io.IOException;
import java.net.InetSocketAddress;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.prometheus.client.hotspot.ClassLoadingExports;
import io.prometheus.client.hotspot.GarbageCollectorExports;
import io.prometheus.client.hotspot.MemoryPoolsExports;
import io.prometheus.client.hotspot.StandardExports;
import io.prometheus.client.hotspot.ThreadExports;
import io.prometheus.client.hotspot.VersionInfoExports;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
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
    private final MetricsService metricsService;

    @Inject
    public Observe(PluginContainer plugin, MetricsService metricsService) {
        this.plugin = plugin;
        this.metricsService = metricsService;
    }

    @Listener
    public void onPreInit(StartedEngineEvent<Server> event)
    {

        metricsService.register(this.plugin, new StandardExports());
        metricsService.register(this.plugin, new MemoryPoolsExports());
        metricsService.register(this.plugin, new GarbageCollectorExports());
        metricsService.register(this.plugin, new ThreadExports());
        metricsService.register(this.plugin, new ClassLoadingExports());
        metricsService.register(this.plugin, new VersionInfoExports());

        metricsService.register(plugin, new SpongeCollector(event.getEngine()));

        try
        {
            metricsService.startExporter(new InetSocketAddress(config.bindAddress, config.bindPort));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Listener
    public void onStop(StoppingEngineEvent<Server> e)
    {
        metricsService.stopExporter();
    }
}
