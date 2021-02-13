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
package org.cubeengine.module.observe.metrics;

import io.prometheus.client.CollectorRegistry;
import org.spongepowered.plugin.PluginContainer;

/**
 * The service can be used to register additional {@link CollectorRegistry} instances for scraping by prometheus.
 *
 * Implementations can chose to scrape Prometheus' global default registry, however plugins should register their own registry. No name or label rules are enforced, however it is advisable
 * to introduce a plugin specific prefix to plugin specific metrics.
 */
public interface MetricsService {
    /**
     * Registers an additional {@link CollectorRegistry} to be scraped. Time series returned by the {@link io.prometheus.client.Collector} will be namespaced by the plugin to prevent name clashes.
     * The standard Prometheus collectors ({@link io.prometheus.client.Gauge}, {@link io.prometheus.client.Counter}, ...) are thread safe and thus safe for
     * scraping from any thread. If you use custom collector implementations or make use of the provided {@link org.cubeengine.module.observe.metrics.pullgauge.PullGauge} interface, make sure your
     * implementation is safe to be called from any thread or add your registry with the asyncCapable parameter set to false.
     *
     * @param plugin the owning plugin
     * @param registry the registry to be scraped
     * @param asyncCapable whether the collectors in the given registry can by scraped outside of the server main thread
     */
    void addCollectorRegistry(PluginContainer plugin, CollectorRegistry registry, boolean asyncCapable);

    /**
     * Removes the given {@link CollectorRegistry} from scraping.
     *
     * @param plugin the owning plugin
     * @param registry the registry to be removed from scraping
     */
    void removeCollectorRegistry(PluginContainer plugin, CollectorRegistry registry);

    /**
     * Removes all {@link CollectorRegistry} instances owned by the given plugin from scaping.
     *
     * @param plugin the owning plugin
     */
    void removeAllCollectorRegistries(PluginContainer plugin);
}
