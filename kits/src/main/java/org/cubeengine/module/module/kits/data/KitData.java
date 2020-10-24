package org.cubeengine.module.module.kits.data;

import com.google.common.reflect.TypeToken;
import org.cubeengine.module.module.kits.PluginKits;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.DataStore;
import org.spongepowered.api.data.value.MapValue;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.lifecycle.RegisterCatalogEvent;

public interface KitData
{
    TypeToken<MapValue<String, Long>> TTMV_SL = new TypeToken<MapValue<String, Long>>() {};
    TypeToken<MapValue<String, Integer>> TTMV_SI = new TypeToken<MapValue<String, Integer>>() {};

    Key<MapValue<String, Long>> TIME = Key.builder().type(TTMV_SL).key(ResourceKey.of(PluginKits.KITS_ID, "item_data")).build();
    Key<MapValue<String, Integer>> TIMES = Key.builder().type(TTMV_SI).key(ResourceKey.of(PluginKits.KITS_ID, "times_data")).build();


    static void register(RegisterCatalogEvent<DataRegistration> event)
    {
        final ResourceKey key = ResourceKey.of(PluginKits.KITS_ID, "kit-usage");
        final DataStore dataStore = DataStore.builder()
                                             .pluginData(key)
                                             .holder(ServerPlayer.class)
                                             .key(TIME,"time")
                                             .key(TIMES, "times")
                                             .build();

        final DataRegistration registration = DataRegistration.builder()
              .key(key).store(dataStore)
              .dataKey(TIME, TIMES).build();
        event.register(registration);
    }
}
