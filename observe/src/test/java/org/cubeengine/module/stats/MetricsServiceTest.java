package org.cubeengine.module.stats;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.GarbageCollectorExports;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.function.DoubleSupplier;

public class MetricsServiceTest {

    @Test
    public void metrics() throws IOException {
        final CollectorRegistry registry = new CollectorRegistry();
        registry.register(new GarbageCollectorExports());
        registry.register(PullGaugeCollector.<DoubleSupplier>build(Math::random).withGauge(PullGauge.build("test", DoubleSupplier::getAsDouble).help("dummy").build()).build());

        final String host = "localhost";
        final HTTPServer server = new HTTPServer(new InetSocketAddress(host, 0), registry, true);
        final String urlBase = "http://" + host + ":" + server.getPort();

        System.out.println(readUrlData(new URL(urlBase + "/")));
        System.out.println(readUrlData(new URL(urlBase + "/other")));
        System.out.println(readUrlData(new URL(urlBase + "/metrics")));
        System.out.println(readUrlData(new URL(urlBase + "/-/healthy")));

        server.stop();
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