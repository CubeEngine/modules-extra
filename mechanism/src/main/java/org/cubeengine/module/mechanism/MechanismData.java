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
package org.cubeengine.module.mechanism;

import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.entity.Sign;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.util.TypeTokens;

public interface MechanismData
{
    Key<Value<String>> MECHANISM = Key.builder().key(ResourceKey.of(PluginMechanism.MECHANISM_ID, "mechanism"))
                                    .type(TypeTokens.STRING_VALUE_TOKEN).build();

    static void register(RegisterDataEvent event)
    {
        event.register(DataRegistration.of(MECHANISM, Sign.class, BlockSnapshot.class, ItemStack.class, ItemStackSnapshot.class));
    }

}
