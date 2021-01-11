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
package org.cubeengine.module.terra.data;

import org.cubeengine.module.terra.PluginTerra;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.util.TypeTokens;
import java.util.UUID;

public class TerraData
{
    public static final Key<Value<String>> WORLD_KEY = Key.builder()
                                                          .key(ResourceKey.of(PluginTerra.TERRA_ID, "worldkey"))
                                                          .type(TypeTokens.STRING_VALUE_TOKEN).build();

    public static final Key<Value<UUID>> WORLD_UUID = Key.builder()
                                                         .key(ResourceKey.of(PluginTerra.TERRA_ID, "worlduuid"))
                                                         .type(TypeTokens.UUID_VALUE_TOKEN).build();

    public static void register(RegisterDataEvent event)
    {
        event.register(DataRegistration.of(WORLD_KEY, ItemStack.class, ItemStackSnapshot.class));
        event.register(DataRegistration.of(WORLD_UUID, ItemStack.class, ItemStackSnapshot.class));
    }
}
