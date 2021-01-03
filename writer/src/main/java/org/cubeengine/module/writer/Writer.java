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
package org.cubeengine.module.writer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.util.ChatFormat;
import org.cubeengine.processor.Module;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.event.lifecycle.RegisterDataPackValueEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.recipe.RecipeRegistration;
import org.spongepowered.api.world.Location;
import org.spongepowered.math.vector.Vector3i;

/**
 * A module to edit signs and signed books
 */
@Singleton
@Module
public class Writer
{
    @Inject private WriterPermissions perms;

    @Listener
    public void onRegisterData(RegisterDataEvent event)
    {
        WriterData.register(event);
    }

    @Listener
    public void onRecipeRegister(RegisterDataPackValueEvent<RecipeRegistration>event)
    {
        WriterItems.registerRecipes(event);
    }

    @Listener
    public void onRightClickBlock(InteractBlockEvent.Secondary event, @First ServerPlayer player)
    {
        if (event.getContext().get(EventContextKeys.USED_HAND).orElse(null) != HandTypes.MAIN_HAND.get()
            || !event.getBlock().getLocation().flatMap(Location::getBlockEntity).map(l -> l.supports(Keys.SIGN_LINES)).orElse(false)
            || !player.getItemInHand(HandTypes.MAIN_HAND).get(WriterData.WRITER).isPresent()
            || !player.getItemInHand(HandTypes.MAIN_HAND).getType().isAnyOf(ItemTypes.WRITABLE_BOOK)
            || !perms.EDIT_SIGN.check(player))
        {
            return;
        }
        List<String> pages = player.getItemInHand(HandTypes.MAIN_HAND).get(Keys.PLAIN_PAGES).orElse(Collections.emptyList());
        final String firstPage = pages.get(0);
        final List<String> lines = Arrays.asList(firstPage.split("\n"));
        final List<Component> signLines = lines.subList(0, Math.min(4, lines.size())).stream()
               .map(line -> ChatFormat.fromLegacy(line, '&')).collect(Collectors.toList());
        final Vector3i pos = event.getBlock().getPosition();
        player.getWorld().offer(pos, Keys.SIGN_LINES, signLines);

    }

}
