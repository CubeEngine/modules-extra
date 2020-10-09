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
package org.cubeengine.module.itemduct.data;

import static java.util.Collections.singletonList;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.module.itemduct.ItemductConfig;
import org.cubeengine.module.itemduct.PluginItemduct;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.event.lifecycle.RegisterCatalogEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.recipe.RecipeRegistration;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe;
import org.spongepowered.api.item.recipe.crafting.Ingredient;
import org.spongepowered.api.item.recipe.crafting.ShapedCraftingRecipe;

import java.util.Collections;

public class DuctRecipes
{
    private static ItemStack activatorItem;
    public static ItemStack singleActivatorItem;
    private static ItemStack superActivatorItem;
    private static RecipeRegistration superRecipe;
    private static RecipeRegistration recipe;

    public static void register(RegisterCatalogEvent<RecipeRegistration> event, ItemductConfig config)
    {
        Ingredient hopper = Ingredient.of(ItemTypes.HOPPER.get());
        activatorItem = ItemStack.of(ItemTypes.HOPPER, 1);
        activatorItem.offer(Keys.APPLIED_ENCHANTMENTS, singletonList(Enchantment.builder().type(EnchantmentTypes.LOOTING).level(1).build()));
        activatorItem.offer(Keys.DISPLAY_NAME, TextComponent.of("ItemDuct Activator", NamedTextColor.GOLD));
        activatorItem.offer(Keys.HIDE_ENCHANTMENTS, true);
        activatorItem.offer(DuctData.USES, config.activatorUses);
        activatorItem.offer(Keys.LORE, Collections.singletonList(TextComponent.of("Uses: ").append(TextComponent.of(config.activatorUses))));

        singleActivatorItem = activatorItem.copy();
        singleActivatorItem.offer(DuctData.USES, 1);
        singleActivatorItem.offer(Keys.LORE, Collections.singletonList(TextComponent.of("Single Use")));

        recipe = CraftingRecipe.shapedBuilder().rows()
                .row(hopper, hopper, hopper)
                .row(hopper, Ingredient.of(ItemTypes.DIAMOND.get()), hopper)
                .row(hopper, hopper, hopper)
                .result(activatorItem.copy())
                .key(ResourceKey.of(PluginItemduct.ITEMDUCT_ID, "itemductactivator"))
                .build();

        superActivatorItem = activatorItem.copy();
        superActivatorItem.offer(DuctData.USES, config.superActivatorUses);
        superActivatorItem.offer(Keys.LORE, Collections.singletonList(TextComponent.of("Uses: ").append(TextComponent.of(config.superActivatorUses == -1 ? "Infinite" : String.valueOf(config.superActivatorUses)))));
        superActivatorItem.offer(Keys.DISPLAY_NAME, TextComponent.of("ItemDuct Super Activator", NamedTextColor.GOLD));

        hopper = Ingredient.of(activatorItem);
        superRecipe = CraftingRecipe.shapedBuilder().rows()
                .row(hopper, hopper, hopper)
                .row(hopper, Ingredient.of(ItemTypes.NETHER_STAR.get()), hopper)
                .row(hopper, hopper, hopper)
                .result(superActivatorItem.copy())
                .key(ResourceKey.of(PluginItemduct.ITEMDUCT_ID, "itemductsuperactivator"))
                .build();

        event.register(recipe);
        event.register(superRecipe);
    }

    public static boolean matchesRecipe(CraftingRecipe craftingRecipe) {
        return recipe.getKey().equals(craftingRecipe.getKey()) || superRecipe.getKey().equals(craftingRecipe.getKey());
    }
}
