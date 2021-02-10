package org.cubeengine.module.observe.metrics;

import io.prometheus.client.Collector;
import org.spongepowered.plugin.PluginContainer;

public interface MetricsService {
    void registerCollector(PluginContainer plugin, SyncCollector collector);
    void registerCollector(PluginContainer plugin, Collector collector);
    void unregisterCollector(PluginContainer plugin, SyncCollector collector);
    void unregisterCollector(PluginContainer plugin, Collector collector);
    void unregisterCollectors(PluginContainer plugin);
}
