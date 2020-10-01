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

import com.google.common.reflect.TypeToken;
import org.cubeengine.module.itemduct.PluginItemduct;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.DataStore;
import org.spongepowered.api.data.value.MapValue;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.event.lifecycle.RegisterCatalogEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.TypeTokens;

import java.util.List;

public interface DuctData
{
    TypeToken<MapValue<Direction, List<ItemStack>>> TTV_ItemDirection = new TypeToken<MapValue<Direction, List<ItemStack>>>() {};

    Key<MapValue<Direction, List<ItemStack>>> FILTERS = Key.builder()
            .key(ResourceKey.of(PluginItemduct.ITEMDUCT_ID, "filters"))
            .type(TTV_ItemDirection).build();

    Key<Value<Integer>> USES = Key.builder()
            .key(ResourceKey.of(PluginItemduct.ITEMDUCT_ID, "uses"))
            .type(TypeTokens.INTEGER_VALUE_TOKEN).build();


    static void register(RegisterCatalogEvent<DataRegistration> event)
    {
        registerFilterData(event);
        registerUseData(event);
    }

    static void registerFilterData(RegisterCatalogEvent<DataRegistration> event)
    {
        final DataStore dataStore = DataStore.builder()
                .key(DuctData.FILTERS, "filters")
                .holder(TypeTokens.BLOCK_ENTITY_TOKEN)
                .build();

        final DataRegistration registration = DataRegistration.builder()
                .key(DuctData.FILTERS)
                .store(dataStore)
                .key(ResourceKey.of(PluginItemduct.ITEMDUCT_ID, "filters"))
                .build();
        event.register(registration);
    }

    static void registerUseData(RegisterCatalogEvent<DataRegistration> event) {
        final DataStore dataStore = DataStore.builder()
                .key(DuctData.USES, "uses")
                .holder(TypeTokens.ITEM_STACK_TOKEN)
                .build();

        final DataRegistration registration = DataRegistration.builder()
                .key(DuctData.USES)
                .store(dataStore)
                .key(ResourceKey.of(PluginItemduct.ITEMDUCT_ID, "uses"))
                .build();
        event.register(registration);
    }
}
