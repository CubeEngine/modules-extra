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
package org.cubeengine.module.vigil.data;

import java.util.UUID;
import org.cubeengine.module.vigil.PluginVigil;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.DataStore;
import org.spongepowered.api.data.value.ListValue;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.util.TypeTokens;

public interface VigilData
{
    Key<Value<UUID>> CREATOR = Key.builder().type(TypeTokens.UUID_VALUE_TOKEN).key(ResourceKey.of(PluginVigil.VIGIL_ID, "creator")).build();
    Key<Value<Boolean>> FULL_DATE = Key.builder().type(TypeTokens.BOOLEAN_VALUE_TOKEN).key(ResourceKey.of(PluginVigil.VIGIL_ID, "full-date")).build();
    Key<Value<Boolean>> SHOW_LOC = Key.builder().type(TypeTokens.BOOLEAN_VALUE_TOKEN).key(ResourceKey.of(PluginVigil.VIGIL_ID, "show-loc")).build();
    Key<Value<Boolean>> NO_DATE = Key.builder().type(TypeTokens.BOOLEAN_VALUE_TOKEN).key(ResourceKey.of(PluginVigil.VIGIL_ID, "no-date")).build();
    Key<Value<Boolean>> FULL_LOC = Key.builder().type(TypeTokens.BOOLEAN_VALUE_TOKEN).key(ResourceKey.of(PluginVigil.VIGIL_ID, "full-loc")).build();
    Key<Value<Boolean>> DETAIL_INV = Key.builder().type(TypeTokens.BOOLEAN_VALUE_TOKEN).key(ResourceKey.of(PluginVigil.VIGIL_ID, "detail-inv")).build();
    Key<ListValue<String>> REPORTS = Key.builder().type(TypeTokens.LIST_STRING_VALUE_TOKEN).key(ResourceKey.of(PluginVigil.VIGIL_ID, "reports")).build();

    static void register(RegisterDataEvent event)
    {
        final ResourceKey rkey = ResourceKey.of(PluginVigil.VIGIL_ID, "lookup-data");
        final DataStore dataStore = DataStore.builder()
                                             .pluginData(rkey)
                                             .holder(ItemStack.class, ItemStackSnapshot.class)
                                             .keys(CREATOR, FULL_DATE, SHOW_LOC, NO_DATE, FULL_LOC, DETAIL_INV, REPORTS)
                                             .build();

        final DataRegistration registration = DataRegistration.builder()
                                                              .dataKey(CREATOR, FULL_DATE, SHOW_LOC, NO_DATE, FULL_LOC, DETAIL_INV, REPORTS)
                                                              .store(dataStore)
                                                              .build();
        event.register(registration);
    }

    static void syncToStack(ItemStack itemStack, LookupData data)
    {
        itemStack.offer(CREATOR, data.getCreator());
        itemStack.offer(FULL_DATE, data.isFullDate());
        itemStack.offer(SHOW_LOC, data.isShowLocation());
        itemStack.offer(NO_DATE, data.isNoDate());
        itemStack.offer(FULL_LOC, data.isFullLocation());
        itemStack.offer(DETAIL_INV, data.showDetailedInventory());
        itemStack.offer(REPORTS, data.getReports());
    }

    static void syncFromStack(ItemStack itemStack, LookupData data)
    {
        itemStack.get(CREATOR).ifPresent(data::setCreator);
        itemStack.get(FULL_DATE).ifPresent(data::setFullDate);
        itemStack.get(SHOW_LOC).ifPresent(data::setShowLocation);
        itemStack.get(NO_DATE).ifPresent(data::setNoDate);
        itemStack.get(FULL_LOC).ifPresent(data::setFullLocation);
        itemStack.get(DETAIL_INV).ifPresent(data::setDetailedInventory);
        itemStack.get(REPORTS).ifPresent(data::setReports);

    }
}
