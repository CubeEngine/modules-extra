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
package org.cubeengine.module.itemduct.data;

import io.leangen.geantyref.TypeToken;
import org.cubeengine.module.itemduct.PluginItemduct;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.DataStore;
import org.spongepowered.api.data.value.MapValue;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.TypeTokens;

import java.util.List;

public interface ItemductData
{
    TypeToken<MapValue<Direction, List<ItemStack>>> TTV_ItemDirection = new TypeToken<MapValue<Direction, List<ItemStack>>>() {};

    Key<MapValue<Direction, List<ItemStack>>> FILTERS = Key.builder()
            .key(ResourceKey.of(PluginItemduct.ITEMDUCT_ID, "filters"))
            .type(TTV_ItemDirection).build();

    Key<Value<Integer>> USES = Key.builder()
            .key(ResourceKey.of(PluginItemduct.ITEMDUCT_ID, "uses"))
            .type(TypeTokens.INTEGER_VALUE_TOKEN).build();


    static void register(RegisterDataEvent event)
    {
        registerFilterData(event);
        registerUseData(event);
    }

    static void registerFilterData(RegisterDataEvent event)
    {
        final ResourceKey rkey = ResourceKey.of(PluginItemduct.ITEMDUCT_ID, "filters");
        final DataStore dataStore = DataStore.builder().pluginData(rkey)
                                             .holder(BlockEntity.class)
                                             .key(ItemductData.FILTERS, "filters")
                                             .build();

        final DataRegistration registration = DataRegistration.builder()
                                                              .dataKey(ItemductData.FILTERS)
                                                              .store(dataStore)
                                                              .build();
        event.register(registration);
    }

    static void registerUseData(RegisterDataEvent event) {
        final ResourceKey rkey = ResourceKey.of(PluginItemduct.ITEMDUCT_ID, "uses");
        final DataStore dataStore = DataStore.builder()
                .pluginData(rkey)
                .holder(ItemStack.class)
                .key(ItemductData.USES, "uses")
                .build();

        final DataRegistration registration = DataRegistration.builder()
                                                              .dataKey(ItemductData.USES)
                                                              .store(dataStore)
                                                              .build();
        event.register(registration);
    }
}
