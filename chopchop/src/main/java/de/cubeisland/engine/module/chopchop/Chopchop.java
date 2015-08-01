/**
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
package de.cubeisland.engine.module.chopchop;

import javax.inject.Inject;
import de.cubeisland.engine.modularity.core.marker.Disable;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.module.core.sponge.EventManager;
import de.cubeisland.engine.service.task.TaskManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.GameRegistry;
import org.spongepowered.api.data.manipulator.DisplayNameData;
import org.spongepowered.api.data.manipulator.item.EnchantmentData;
import org.spongepowered.api.data.manipulator.item.LoreData;
import org.spongepowered.api.item.Enchantments;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.recipe.Recipes;
import org.spongepowered.api.item.recipe.ShapedRecipe;
import org.spongepowered.api.text.Texts;

import static org.spongepowered.api.item.ItemTypes.DIAMOND_AXE;
import static org.spongepowered.api.item.ItemTypes.LOG;
import static org.spongepowered.api.text.format.TextColors.GOLD;
import static org.spongepowered.api.text.format.TextColors.YELLOW;

@ModuleInfo(name = "ChopChop", description = "Chop whole trees down")
public class Chopchop extends Module
{
    @Inject private EventManager em;
    @Inject private Game game;
    @Inject private TaskManager tm;

    private ShapedRecipe recipe;

    @Enable
    public void onEnable()
    {
        em.registerListener(this, new ChopListener(this, game, tm));
        tm.runTaskDelayed(this, this::registerRecipe, 1);
    }

    @Disable
    public void onDisable()
    {
        if (recipe != null)
        {
            game.getRegistry().getRecipeRegistry().remove(recipe);
        }
        em.removeListeners(this);
    }

    public void registerRecipe()
    {
        GameRegistry registry = game.getRegistry();
        ItemStack axe = registry.getItemBuilder().itemType(DIAMOND_AXE).quantity(1).build();
        EnchantmentData enchantments = game.getRegistry().getManipulatorRegistry().getBuilder(EnchantmentData.class).get().create();
        enchantments.setUnsafe(Enchantments.PUNCH, 5);
        axe.offer(enchantments);

        DisplayNameData display = axe.getOrCreate(DisplayNameData.class).get();
        display.setDisplayName(Texts.of(GOLD, "Heavy Diamond Axe"));
        axe.offer(display);

        LoreData lore = game.getRegistry().getManipulatorRegistry().getBuilder(LoreData.class).get().create();
        lore.set(Texts.of(YELLOW, "Chop Chop!"));
        axe.offer(lore);

        ItemStack axeHead = registry.getItemBuilder().itemType(DIAMOND_AXE).build();
        ItemStack axeHandle = registry.getItemBuilder().itemType(LOG).build();

        recipe = Recipes.shapedBuilder().height(3).width(2)
                        .row(0, axeHead, axeHead)
                        .row(1, axeHead, axeHandle)
                        .row(2, null, axeHandle).addResult(axe).build();
        registry.getRecipeRegistry().register(recipe);
    }
}
