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

import java.util.Arrays;
import de.cubeisland.engine.module.core.sponge.SpongeCore;
import de.cubeisland.engine.module.core.module.Module;
import de.cubeisland.engine.module.core.util.ChatFormat;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import static org.bukkit.Material.DIAMOND_AXE;
import static org.bukkit.Material.LOG;
import static org.bukkit.enchantments.Enchantment.ARROW_KNOCKBACK;

public class Chopchop extends Module
{
    @Override
    public void onEnable()
    {
        this.getCore().getEventManager().registerListener(this, new ChopListener(this));

        ItemStack axe = new ItemStack(DIAMOND_AXE, 1);
        axe.addUnsafeEnchantment(ARROW_KNOCKBACK, 5);
        ItemMeta itemMeta = axe.getItemMeta();
        itemMeta.setDisplayName(ChatFormat.parseFormats("&6Heavy Diamond Axe"));
        itemMeta.setLore(Arrays.asList(ChatFormat.parseFormats("&eChop Chop!")));
        axe.setItemMeta(itemMeta);

        ShapedRecipe heavyAxe = new ShapedRecipe(axe).shape("aa", "la", "l ").
            setIngredient('a', DIAMOND_AXE).setIngredient('l', LOG);

        Server server = ((SpongeCore)this.getCore()).getServer();
        server.addRecipe(heavyAxe);
    }
}
