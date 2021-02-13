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
import io.prometheus.client.CollectorRegistry;
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
import org.cubeengine.module.observe.health.impl.LastTickHealth;
import org.cubeengine.module.observe.health.impl.SimpleHealthCheckService;
import org.cubeengine.module.observe.health.impl.TickTimeCollector;
import org.cubeengine.module.observe.metrics.MetricsService;
import org.cubeengine.module.observe.metrics.impl.PrometheusMetricsService;
import org.cubeengine.module.observe.metrics.impl.SpongeCollector;
import org.cubeengine.module.observe.tracing.impl.JaegerTracingService;
import org.cubeengine.module.observe.tracing.TracingService;
import org.cubeengine.module.observe.tracing.impl.PrometheusMetricsFactory;
import org.cubeengine.module.observe.web.WebServer;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ProvideServiceEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.scheduler.TaskExecutorService;
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
    private final CollectorRegistry asyncCollectorRegistry = new CollectorRegistry();

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
        provideHealth();
        provideMetrics();
    }

    @Listener
    public void onProvideMetrics(ProvideServiceEvent<MetricsService> event)
    {
        event.suggest(this::provideMetrics);
    }

    @Listener
    public void onProvideHealth(ProvideServiceEvent<HealthCheckService> event)
    {
        event.suggest(this::provideHealth);
    }

    @Listener
    public void onProvideTracing(ProvideServiceEvent<TracingService> event)
    {
        event.suggest(this::provideTracing);
    }

    private MetricsService provideMetrics() {
        final TaskExecutorService syncExecutor = Sponge.getServer().getScheduler().createExecutor(plugin);
        final TaskExecutorService asyncExecutor = Sponge.getAsyncScheduler().createExecutor(plugin);

        final PrometheusMetricsService service = new PrometheusMetricsService(syncExecutor, asyncExecutor, logger);

        final CollectorRegistry asyncRegistry = new CollectorRegistry();
        asyncRegistry.register(new StandardExports());
        asyncRegistry.register(new MemoryPoolsExports());
        asyncRegistry.register(new GarbageCollectorExports());
        asyncRegistry.register(new ThreadExports());
        asyncRegistry.register(new ClassLoadingExports());
        asyncRegistry.register(new VersionInfoExports());
        service.addCollectorRegistry(plugin, asyncRegistry, true);

        final CollectorRegistry syncRegistry = new CollectorRegistry();
        final Server server = Sponge.getServer();
        syncRegistry.register(new SpongeCollector(server, plugin));
        syncRegistry.register(new TickTimeCollector(server, tm, plugin));
        service.addCollectorRegistry(plugin, syncRegistry, false);

        getWebServer().registerHandlerAndStart(config.metricsEndpoint, service);
        return service;
    }

    private HealthCheckService provideHealth() {
        final Scheduler scheduler = Sponge.getServer().getScheduler();
        final TaskExecutorService executor = scheduler.createExecutor(plugin);
        final SimpleHealthCheckService service = new SimpleHealthCheckService(executor);

        service.registerProbe(plugin, "last-tick", new LastTickHealth(plugin, scheduler, 45000L));

        getWebServer().registerHandlerAndStart(config.healthEndpoint, service);
        return service;
    }

    private TracingService provideTracing() {
        final PrometheusMetricsFactory metricsFactory = new PrometheusMetricsFactory(asyncCollectorRegistry);
        return new JaegerTracingService(plugin.getMetadata().getId(), metricsFactory);
    }

    @Listener
    public void onStop(StoppingEngineEvent<Server> e)
    {
        if (webServer != null) {
            webServer.stop();
        }
    }
}
