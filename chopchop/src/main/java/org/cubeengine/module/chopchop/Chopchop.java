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
import static org.spongepowered.api.text.format.TextColors.GOLD;
import static org.spongepowered.api.text.format.TextColors.YELLOW;

import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Disable;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.libcube.hack.RecipeHack;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.task.TaskManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.GameRegistry;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.meta.ItemEnchantment;
import org.spongepowered.api.item.Enchantments;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.recipe.ShapedRecipe;
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

    private ShapedRecipe recipe;

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
            game.getRegistry().getRecipeRegistry().remove(recipe);
        }
    }

    public void registerRecipe()
    {
        GameRegistry registry = game.getRegistry();
        ItemStack axe = ItemStack.builder().itemType(DIAMOND_AXE).quantity(1).build();
        axe.offer(Keys.ITEM_ENCHANTMENTS, singletonList(new ItemEnchantment(Enchantments.PUNCH, 5)));
        axe.offer(Keys.DISPLAY_NAME, Text.of(GOLD, "Heavy Diamond Axe"));
        axe.offer(Keys.ITEM_LORE, singletonList(Text.of(YELLOW, "Chop Chop!")));

        ItemStack axeHead = ItemStack.builder().itemType(DIAMOND_AXE).build();
        ItemStack axeHandle = ItemStack.builder().itemType(LOG).build();

        HashMap<Character, ItemStack> map = new HashMap<>();
        map.put('a', axeHead);
        map.put('s', axeHandle);
        Object recipe = RecipeHack.addRecipe(axe, new String[]{"aa", "as", " s"}, map);

        /*
        recipe = Sponge.getRegistry().createBuilder(ShapedRecipe.Builder.class).height(3).width(2)
                                             .row(0, axeHead, axeHead)
                                             .row(1, axeHead, axeHandle)
                                             .row(2, null, axeHandle).addResult(axe).build();
        registry.getRecipeRegistry().register(recipe);
        */
    }
}
