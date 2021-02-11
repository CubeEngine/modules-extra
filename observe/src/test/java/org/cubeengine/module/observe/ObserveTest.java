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

import io.prometheus.client.hotspot.GarbageCollectorExports;
import org.apache.logging.log4j.LogManager;
import org.cubeengine.libcube.util.Pair;
import org.cubeengine.module.observe.health.HealthState;
import org.cubeengine.module.observe.health.impl.SimpleHealthCheckService;
import org.cubeengine.module.observe.metrics.impl.PrometheusMetricsService;
import org.cubeengine.module.observe.metrics.pullgauge.PullGauge;
import org.cubeengine.module.observe.metrics.pullgauge.PullGaugeCollector;
import org.cubeengine.module.observe.web.WebServer;
import org.junit.Test;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.metadata.PluginMetadata;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.DoubleSupplier;

import static org.junit.Assert.*;

public class ObserveTest {

    @Test
    public void themAll() throws IOException {
        final PluginContainer plugin = (PluginContainer) Proxy.newProxyInstance(PluginContainer.class.getClassLoader(), new Class[]{ PluginContainer.class }, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getLogger":
                    return LogManager.getLogger();
                case "getMetadata":
                    return PluginMetadata.builder()
                            .setLoader("observe")
                            .setId("observe")
                            .setVersion("1.0.0")
                            .setMainClass(Observe.class.getName())
                            .build();
                default:
                    return null;
            }
        });

        final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

        final InetSocketAddress addr = new InetSocketAddress("0.0.0.0", 0);
        final WebServer webServer = new WebServer(addr, Thread::new, plugin.getLogger());
        final PrometheusMetricsService metricsService = new PrometheusMetricsService(executorService, executorService, plugin.getLogger());
        metricsService.registerCollector(plugin, new GarbageCollectorExports());
        metricsService.registerCollector(plugin, PullGaugeCollector.<DoubleSupplier>build(Math::random).withGauge(PullGauge.build("test", DoubleSupplier::getAsDouble).help("dummy").build()).build());
        assertTrue(webServer.registerHandlerAndStart("/metrics", metricsService));

        final SimpleHealthCheckService healthyService = new SimpleHealthCheckService(executorService);
        healthyService.registerProbe(plugin, "test", () -> HealthState.HEALTHY);
        assertTrue(webServer.registerHandlerAndStart("/healthy", healthyService));

        final SimpleHealthCheckService brokenService = new SimpleHealthCheckService(executorService);
        brokenService.registerProbe(plugin, "test", () -> HealthState.BROKEN);
        assertTrue(webServer.registerHandlerAndStart("/broken", brokenService));

        final String urlBase = "http://localhost:" + webServer.getBoundAddress().getPort();

        assertThrows(FileNotFoundException.class, () -> {
            System.out.println(readUrlData(new URL(urlBase + "/other")));
        });

        System.out.println(readUrlData(new URL(urlBase + "/metrics")));


        assertEquals(new Pair<>(200, "{\"state\":\"HEALTHY\",\"details\":{\"observe:test\":\"HEALTHY\"}}"), readUrlData(new URL(urlBase + "/healthy")));

        assertThrows(IOException.class, () -> {
            readUrlData(new URL(urlBase + "/broken"));
        });

        webServer.stop();
    }

    private static Pair<Integer, String> readUrlData(URL url) throws IOException {
        System.out.println("URL: " + url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();
        final InputStream data = connection.getInputStream();
        byte[] buf = new byte[1000];
        ByteArrayOutputStream agg = new ByteArrayOutputStream();

        while (true) {
            int bytesRead = data.read(buf);
            if (bytesRead == -1) {
                break;
            }
            agg.write(buf, 0, bytesRead);
        }

        return new Pair<>(connection.getResponseCode(), agg.toString(StandardCharsets.UTF_8.toString()));
    }

}