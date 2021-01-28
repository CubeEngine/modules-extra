package org.cubeengine.module.stats;

import io.prometheus.client.hotspot.GarbageCollectorExports;
import org.junit.Test;
import org.spongepowered.plugin.PluginContainer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.function.DoubleSupplier;

public class MetricsServiceTest {

    @Test
    public void metrics() throws IOException {
        final MetricsService metricsService = new MetricsService();
        final PluginContainer plugin = (PluginContainer) Proxy.newProxyInstance(PluginContainer.class.getClassLoader(), new Class[]{ PluginContainer.class }, (proxy, method, args) -> null);
        metricsService.register(plugin, new GarbageCollectorExports());
        metricsService.register(plugin, PullGaugeCollector.<DoubleSupplier>build(Math::random).withGauge(PullGauge.build("test", DoubleSupplier::getAsDouble).help("dummy").build()).build());

        final String host = "localhost";
        final InetSocketAddress addr = new InetSocketAddress(host, 0);
        metricsService.startExporter(addr);

        final String urlBase = "http://" + host + ":" + metricsService.getPort();

        System.out.println(readUrlData(new URL(urlBase + "/")));
        System.out.println(readUrlData(new URL(urlBase + "/other")));
        System.out.println(readUrlData(new URL(urlBase + "/metrics")));
        System.out.println(readUrlData(new URL(urlBase + "/-/healthy")));

        metricsService.stopExporter();
    }

    private static String readUrlData(URL url) throws IOException {
        System.out.println("URL: " + url);
        URLConnection connection = url.openConnection();
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

        return agg.toString(StandardCharsets.UTF_8.toString());
    }

}