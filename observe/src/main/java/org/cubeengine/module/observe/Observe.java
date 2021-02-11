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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.prometheus.client.hotspot.ClassLoadingExports;
import io.prometheus.client.hotspot.GarbageCollectorExports;
import io.prometheus.client.hotspot.MemoryPoolsExports;
import io.prometheus.client.hotspot.StandardExports;
import io.prometheus.client.hotspot.ThreadExports;
import io.prometheus.client.hotspot.VersionInfoExports;
import org.apache.logging.log4j.Logger;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.module.observe.health.HealthCheckService;
import org.cubeengine.module.observe.health.impl.SimpleHealthCheckService;
import org.cubeengine.module.observe.metrics.MetricsService;
import org.cubeengine.module.observe.metrics.impl.PrometheusMetricsService;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ProvideServiceEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.plugin.PluginContainer;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadFactory;

@Singleton
@Module
public class Observe
{
    @ModuleConfig private ObserveConfig config;
    private final PluginContainer plugin;
    private WebServer webServer;
    private final Logger logger;
    private final ThreadFactory tf;
    private final TaskManager tm;

    @Inject
    public Observe(PluginContainer plugin, Logger logger, ThreadFactory tf, TaskManager tm) {
        this.plugin = plugin;
        this.logger = logger;
        this.tf = tf;
        this.tm = tm;
    }

    private synchronized WebServer getWebServer() {
        if (webServer == null) {
            this.webServer = new WebServer(new InetSocketAddress(config.bindAddress, config.bindPort), tf, logger);
        }
        return webServer;
    }

    @Listener
    public void onStarted(StartedEngineEvent<Server> event)
    {
        // TODO remove this once interfaces are part of the API
        providerHealth();
        providerMetrics();
    }

    @Listener
    public void onProvideMetrics(ProvideServiceEvent<MetricsService> event)
    {
        event.suggest(this::providerMetrics);
    }

    @Listener
    public void onProvideHealth(ProvideServiceEvent<HealthCheckService> event)
    {
        event.suggest(this::providerHealth);
    }

    private MetricsService providerMetrics() {
        final PrometheusMetricsService service = new PrometheusMetricsService(tm, logger);

        service.registerCollector(plugin, new StandardExports());
        service.registerCollector(plugin, new MemoryPoolsExports());
        service.registerCollector(plugin, new GarbageCollectorExports());
        service.registerCollector(plugin, new ThreadExports());
        service.registerCollector(plugin, new ClassLoadingExports());
        service.registerCollector(plugin, new VersionInfoExports());

        final Server server = Sponge.getServer();
        service.registerCollector(plugin, new SpongeCollector(server, plugin));
        service.registerCollector(plugin, new TickTimeCollector(server, tm, plugin));

        getWebServer().registerHandlerAndStart(config.metricsEndpoint, service);
        return service;
    }

    private HealthCheckService providerHealth() {
        final SimpleHealthCheckService service = new SimpleHealthCheckService(tm);

        //service.registerProbe(plugin, new StandardExports());

        getWebServer().registerHandlerAndStart(config.metricsEndpoint, service);
        return service;
    }

    @Listener
    public void onStop(StoppingEngineEvent<Server> e)
    {
        if (webServer != null) {
            webServer.stop();
        }
    }
}
