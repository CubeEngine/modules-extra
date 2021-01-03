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
import static org.spongepowered.api.item.ItemTypes.OAK_LOG;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.libcube.service.event.ModuleListener;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.processor.Module;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterDataPackValueEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.recipe.RecipeRegistration;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe;
import org.spongepowered.api.item.recipe.crafting.Ingredient;
import org.spongepowered.plugin.PluginContainer;

@Singleton
@Module
public class Chopchop
{
    @ModuleConfig ChopchopConfig config;
    @Inject private PluginContainer plugin;
    @Inject private PermissionManager pm;
    @ModuleListener private ChopListener listener;
    private RecipeRegistration recipe;
    protected Permission usePerm;
    protected Permission autoplantPerm;

    @Listener
    public void onConstruct(StartedEngineEvent<Server> event)
    {
        usePerm = pm.register(Chopchop.class, "use", "Allows using the Chop Chop Axe", null);
        autoplantPerm = pm.register(Chopchop.class, "auto-plant", "Replants saplings automatically", null);
    }

    @Listener
    public void onRegistry(RegisterDataPackValueEvent<RecipeRegistration>event)
    {
        ItemStack axe = ItemStack.of(DIAMOND_AXE, 1);
        axe.offer(Keys.APPLIED_ENCHANTMENTS, singletonList(Enchantment.builder().type(EnchantmentTypes.PUNCH).level(5).build()));
        axe.offer(Keys.CUSTOM_NAME, Component.text("Heavy Diamond Axe", NamedTextColor.GOLD));
        axe.offer(Keys.LORE, singletonList(Component.text("Chop Chop!", NamedTextColor.YELLOW)));
        axe.offer(Keys.HIDE_ENCHANTMENTS, true);

        this.recipe = CraftingRecipe.shapedBuilder()
                .aisle("aa", "as", " s")
                .where('a', Ingredient.of(DIAMOND_AXE))
                .where('s', Ingredient.of(OAK_LOG))
                .result(axe)
                .key(ResourceKey.of(plugin, "chopchop"))
                .build();
        event.register(recipe);
    }

    public ChopchopConfig getConfig()
    {
        return config;
    }
}
