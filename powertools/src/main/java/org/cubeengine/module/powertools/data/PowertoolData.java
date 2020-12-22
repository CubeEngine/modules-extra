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
package org.cubeengine.module.powertools.data;

import io.leangen.geantyref.TypeToken;
import org.cubeengine.module.powertools.PluginPowertools;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.DataStore;
import org.spongepowered.api.data.value.ListValue;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.item.inventory.ItemStack;

public interface PowertoolData
{
    TypeToken<ListValue<String>> TTLV_String = new TypeToken<ListValue<String>>() {};

    Key<ListValue<String>> POWERS = Key.builder().type(TTLV_String).key(ResourceKey.of(PluginPowertools.POWERTOOLS_ID, "powers")).build();

    static void register(RegisterDataEvent event)
    {
        final ResourceKey rkey = ResourceKey.of(PluginPowertools.POWERTOOLS_ID, "powers");
        final DataStore dataStore = DataStore.builder().pluginData(rkey)
                                             .holder(ItemStack.class)
                                             .key(POWERS, "powers")
                                             .build();

        final DataRegistration registration = DataRegistration.builder()
                                                              .dataKey(POWERS)
                                                              .store(dataStore)
                                                              .key(rkey)
                                                              .build();
        event.register(registration);
    }

}
