package org.cubeengine.module.observe.health;

import org.spongepowered.plugin.PluginContainer;

public interface HealthCheckService {
    void registerProbe(PluginContainer plugin, String id, SyncHealthProbe probe);
    void registerProbe(PluginContainer plugin, String id, AsyncHealthProbe probe);
    void unregisterProbe(PluginContainer plugin, String id);
    void unregisterProbes(PluginContainer plugin);
}
