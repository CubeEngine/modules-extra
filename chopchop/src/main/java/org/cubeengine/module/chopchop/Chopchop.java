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
package org.cubeengine.module.chopchop;

import static java.util.Collections.singletonList;
import static org.spongepowered.api.item.ItemTypes.DIAMOND_AXE;
import static org.spongepowered.api.item.ItemTypes.LOG;
import static org.spongepowered.api.item.ItemTypes.LOG2;
import static org.spongepowered.api.text.format.TextColors.GOLD;
import static org.spongepowered.api.text.format.TextColors.YELLOW;

import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.service.event.ModuleListener;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe;
import org.spongepowered.api.item.recipe.crafting.Ingredient;
import org.spongepowered.api.item.recipe.crafting.ShapedCraftingRecipe;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Module
public class Chopchop extends CubeEngineModule
{
    @Inject private PluginContainer plugin;
    @ModuleListener private ChopListener listener;
    private ShapedCraftingRecipe recipe;

    @Listener
    public void onPreInit(GamePreInitializationEvent event)
    {
        this.registerRecipe();
    }

    public void registerRecipe()
    {
        ItemStack axe = ItemStack.of(DIAMOND_AXE, 1);
        axe.offer(Keys.ITEM_ENCHANTMENTS, singletonList(Enchantment.builder().type(EnchantmentTypes.PUNCH).level(5).build()));
        axe.offer(Keys.DISPLAY_NAME, Text.of(GOLD, "Heavy Diamond Axe"));
        axe.offer(Keys.ITEM_LORE, singletonList(Text.of(YELLOW, "Chop Chop!")));

        this.recipe = CraftingRecipe.shapedBuilder()
                .aisle("aa", "as", " s")
                .where('a', Ingredient.of(DIAMOND_AXE))
                .where('s', Ingredient.of(LOG, LOG2))
                .result(axe)
                .build("chopchop", plugin);
        Sponge.getRegistry().getCraftingRecipeRegistry().register(recipe);

    }
}
