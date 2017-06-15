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

import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Disable;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.task.TaskManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.GameRegistry;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.meta.ItemEnchantment;
import org.spongepowered.api.item.Enchantments;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe;
import org.spongepowered.api.item.recipe.crafting.Ingredient;
import org.spongepowered.api.item.recipe.crafting.ShapedCraftingRecipe;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;

import java.util.HashMap;

import javax.inject.Inject;

@ModuleInfo(name = "ChopChop", description = "Chop whole trees down")
public class Chopchop extends Module
{
    @Inject private EventManager em;
    @Inject private Game game;
    @Inject private TaskManager tm;
    @Inject private PluginContainer plugin;

    private ShapedCraftingRecipe recipe;

    @Enable
    public void onEnable()
    {
        em.registerListener(Chopchop.class, new ChopListener(plugin));
        tm.runTaskDelayed(Chopchop.class, this::registerRecipe, 1);
    }

    @Disable
    public void onDisable()
    {
        if (recipe != null)
        {
            game.getRegistry().getCraftingRecipeRegistry().remove(recipe);
        }
    }

    public void registerRecipe()
    {
        ItemStack axe = ItemStack.of(DIAMOND_AXE, 1);
        axe.offer(Keys.ITEM_ENCHANTMENTS, singletonList(new ItemEnchantment(Enchantments.PUNCH, 5)));
        axe.offer(Keys.DISPLAY_NAME, Text.of(GOLD, "Heavy Diamond Axe"));
        axe.offer(Keys.ITEM_LORE, singletonList(Text.of(YELLOW, "Chop Chop!")));

        Ingredient axeHandle = Ingredient.builder().with(LOG, LOG2).build();

        Sponge.getRegistry().getCraftingRecipeRegistry().register(plugin, "chopchop",
              CraftingRecipe.shapedBuilder()
                      .aisle("aa", "as", " s")
                      .where('a', ItemTypes.DIAMOND_AXE)
                      .where('s', axeHandle)
                      .result(axe)
                      .build());

    }
}
