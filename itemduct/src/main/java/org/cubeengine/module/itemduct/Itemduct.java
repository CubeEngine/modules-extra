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
package org.cubeengine.module.itemduct;

import static java.util.Collections.singletonList;
import static org.spongepowered.api.item.Enchantments.LOOTING;

import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.service.event.ModuleListener;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.module.itemduct.data.DuctData;
import org.cubeengine.module.itemduct.data.DuctDataBuilder;
import org.cubeengine.module.itemduct.data.ImmutableDuctData;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.meta.ItemEnchantment;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe;
import org.spongepowered.api.item.recipe.crafting.Ingredient;
import org.spongepowered.api.plugin.PluginContainer;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Module
public class Itemduct extends CubeEngineModule
{
    @ModuleConfig private ItemductConfig config;
    @ModuleListener private ItemDuctListener listener;
    @Inject private PluginContainer plugin;

    private static ItemStack activatorItem;

    @Listener
    public void onPreInit(GamePreInitializationEvent event)
    {
        DataRegistration.<DuctData, ImmutableDuctData>builder()
                .dataClass(DuctData.class).immutableClass(ImmutableDuctData.class)
                .builder(new DuctDataBuilder()).manipulatorId("duct")
                .dataName("CubeEngine ItemDuct Data")
                .buildAndRegister(plugin);

        Ingredient hopper = Ingredient.of(ItemTypes.HOPPER);
        activatorItem = ItemStack.of(ItemTypes.HOPPER, 1);
        activatorItem.offer(Keys.ITEM_ENCHANTMENTS, singletonList(new ItemEnchantment(LOOTING, 1)));
        Sponge.getRegistry().getCraftingRecipeRegistry().register(
        CraftingRecipe.shapedBuilder().rows()
                .row(hopper, hopper, hopper)
                .row(hopper, Ingredient.of(ItemTypes.DIAMOND), hopper)
                .row(hopper, hopper, hopper)
                .result(activatorItem).build("ItemDuctActivator", plugin));
    }

    public static ItemStack activatorItem()
    {
        return activatorItem;
    }
}
