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
package org.cubeengine.module.module.kits.data;

import io.leangen.geantyref.TypeToken;
import org.cubeengine.module.module.kits.PluginKits;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.DataStore;
import org.spongepowered.api.data.value.MapValue;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;

public interface KitData
{
    TypeToken<MapValue<String, Long>> TTMV_SL = new TypeToken<MapValue<String, Long>>() {};
    TypeToken<MapValue<String, Integer>> TTMV_SI = new TypeToken<MapValue<String, Integer>>() {};

    Key<MapValue<String, Long>> TIME = Key.builder().type(TTMV_SL).key(ResourceKey.of(PluginKits.KITS_ID, "item_data")).build();
    Key<MapValue<String, Integer>> TIMES = Key.builder().type(TTMV_SI).key(ResourceKey.of(PluginKits.KITS_ID, "times_data")).build();


    static void register(RegisterDataEvent event)
    {
        final ResourceKey key = ResourceKey.of(PluginKits.KITS_ID, "kit-usage");
        final DataStore dataStore = DataStore.builder()
                                             .pluginData(key)
                                             .holder(ServerPlayer.class)
                                             .key(TIME,"time")
                                             .key(TIMES, "times")
                                             .build();

        final DataRegistration registration = DataRegistration.builder()
              .store(dataStore)
              .dataKey(TIME, TIMES).build();
        event.register(registration);
    }
}
