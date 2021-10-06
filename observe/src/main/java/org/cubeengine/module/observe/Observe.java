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
import org.cubeengine.module.observe.health.LastTickHealth;
import org.cubeengine.module.observe.health.SimpleHealthCheckService;
import org.cubeengine.module.observe.metrics.PrometheusMetricSubscriber;
import org.cubeengine.module.observe.metrics.PrometheusMetricsService;
import org.cubeengine.module.observe.web.WebServer;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Game;
import org.spongepowered.api.Server;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.scheduler.TaskExecutorService;
import org.spongepowered.observer.healthcheck.HealthCheck;
import org.spongepowered.observer.metrics.Meter;
import org.spongepowered.plugin.PluginContainer;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadFactory;

@Singleton
@Module
public class Observe
{


    @ModuleConfig private ObserveConfig config;
    private final PluginContainer plugin;
    private final Logger logger;
    private final ThreadFactory tf;

    private WebServer webServer;
    private PrometheusMetricSubscriber prometheusSubscriber;

    @Inject
    public Observe(PluginContainer plugin, Logger logger, ThreadFactory tf) {
        this.plugin = plugin;
        this.logger = logger;
        this.tf = tf;
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
        final Game game = event.game();
        provideHealth(game);
        provideMetrics(game);
    }

    private void provideMetrics(Game game) {
        final TaskExecutorService asyncExecutor = game.asyncScheduler().executor(plugin);

        final CollectorRegistry registry = CollectorRegistry.defaultRegistry;
        registry.register(new StandardExports());
        registry.register(new MemoryPoolsExports());
        registry.register(new GarbageCollectorExports());
        registry.register(new ThreadExports());
        registry.register(new ClassLoadingExports());
        registry.register(new VersionInfoExports());

        final PrometheusMetricsService service = new PrometheusMetricsService(registry, asyncExecutor);
        Meter.DEFAULT.subscribe(service.getSubscriber());

        getWebServer().registerHandlerAndStart(config.metricsEndpoint, service);
    }

    private void provideHealth(Game game) {
        final Scheduler scheduler = game.server().scheduler();
        final TaskExecutorService executor = scheduler.executor(plugin);
        final SimpleHealthCheckService service = new SimpleHealthCheckService(executor, HealthCheck.DEFAULT, logger);

        HealthCheck.registerProbe("last-tick", new LastTickHealth(plugin, scheduler, 45000L));

        getWebServer().registerHandlerAndStart(config.healthEndpoint, service);
    }

    @Listener
    public void onStop(StoppingEngineEvent<Server> e)
    {
        Meter.DEFAULT.unsubscribe(prometheusSubscriber);
        prometheusSubscriber = null;

        if (webServer != null) {
            webServer.stop();
            webServer = null;
        }
    }
}
