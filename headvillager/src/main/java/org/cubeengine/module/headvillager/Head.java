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
package org.cubeengine.module.headvillager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.property.ProfileProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class Head
{
    public final String name;
    public final UUID uuid;
    public final String value;
    public final String tags;

    public transient ItemStack stack;

    public void init(Category category)
    {
        category.heads.computeIfAbsent(this.name, n -> new ArrayList<>()).add(this);

        final GameProfile gameProfile = GameProfile.of(uuid, name).withProperty(ProfileProperty.of(ProfileProperty.TEXTURES, value));
        stack = ItemStack.of(ItemTypes.PLAYER_HEAD);
        stack.offer(Keys.GAME_PROFILE, gameProfile);
        stack.offer(Keys.CUSTOM_NAME, Component.text(name));
        stack.offer(Keys.LORE, Arrays.asList(Component.text(tags, NamedTextColor.GRAY)));
    }

    public Head(String name, UUID uuid, String value, String tags)
    {
        this.name = name;
        this.uuid = uuid;
        this.value = value;
        this.tags = tags;
    }
}
