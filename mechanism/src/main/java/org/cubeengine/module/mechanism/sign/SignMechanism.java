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
package org.cubeengine.module.mechanism.sign;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.module.mechanism.MechanismData;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.world.server.ServerLocation;
import java.util.List;

public interface SignMechanism
{
    ItemStack makeSign(ItemStack signStack);

    boolean interact(InteractBlockEvent event, ServerPlayer player, ServerLocation loc, boolean hidden);

    String getName();

    default void initSign(BlockEntity sign) {
        sign.transform(Keys.SIGN_LINES, lines -> {
            lines.set(0, Component.text("[Mechanism]", NamedTextColor.DARK_GRAY));
            lines.set(1, Component.text(this.getName(), NamedTextColor.DARK_GRAY));
            return this.initLines(lines);
        });
        sign.offer(MechanismData.MECHANISM, this.getName());
    }

    default List<Component> initLines(List<Component> lines) {
        return lines;
    }
}
