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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import org.spongepowered.plugin.PluginContainer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class MetricsService
{
    private final CollectorRegistry registry;
    private final AtomicReference<HTTPServer> exporter;

    @Inject
    public MetricsService()
    {
        this.registry = new CollectorRegistry();
        this.exporter = new AtomicReference<>(null);
    }

    public void register(PluginContainer plugin, Collector collector)
    {
        this.registry.register(collector);
    }

    void startExporter(InetSocketAddress bindAddr) throws IOException {
        this.exporter.compareAndSet(null, new HTTPServer(bindAddr, registry, true));
    }

    void stopExporter() {
        this.exporter.get().stop();
        this.exporter.set(null);
    }
}
