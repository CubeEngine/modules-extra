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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.module.itemduct.ItemductConfig;
import org.cubeengine.module.itemduct.ItemductManager;
import org.cubeengine.module.itemduct.PluginItemduct;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.lifecycle.RegisterCatalogEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.recipe.RecipeRegistration;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe;
import org.spongepowered.api.item.recipe.crafting.Ingredient;

import java.util.Collections;

public class ItemductItems
{
    private static ItemStack activatorItem;
    public static ItemStack singleActivatorItem;
    private static ItemStack superActivatorItem;
    private static RecipeRegistration superRecipe;
    private static RecipeRegistration recipe;

    public static void registerRecipes(RegisterCatalogEvent<RecipeRegistration> event, ItemductConfig config)
    {
        Ingredient hopper = Ingredient.of(ItemTypes.HOPPER.get());
        activatorItem = ItemStack.of(ItemTypes.HOPPER, 1);
        activatorItem.offer(Keys.APPLIED_ENCHANTMENTS, singletonList(Enchantment.builder().type(EnchantmentTypes.LOOTING).level(1).build()));
        activatorItem.offer(Keys.DISPLAY_NAME, Component.text("ItemDuct Activator", NamedTextColor.GOLD));
        activatorItem.offer(Keys.HIDE_ENCHANTMENTS, true);
        activatorItem.offer(ItemductData.USES, config.activatorUses);
        activatorItem.offer(Keys.LORE, Collections.singletonList(Component.text("Uses: ").append(Component.text(config.activatorUses))));

        singleActivatorItem = activatorItem.copy();
        singleActivatorItem.offer(ItemductData.USES, 1);
        singleActivatorItem.offer(Keys.LORE, Collections.singletonList(Component.text("Single Use")));

        recipe = CraftingRecipe.shapedBuilder().rows()
                .row(hopper, hopper, hopper)
                .row(hopper, Ingredient.of(ItemTypes.DIAMOND.get()), hopper)
                .row(hopper, hopper, hopper)
                .result(activatorItem.copy())
                .key(ResourceKey.of(PluginItemduct.ITEMDUCT_ID, "itemductactivator"))
                .build();

        superActivatorItem = activatorItem.copy();
        superActivatorItem.offer(ItemductData.USES, config.superActivatorUses);
        superActivatorItem.offer(Keys.LORE, Collections.singletonList(Component.text("Uses: ").append(Component.text(config.superActivatorUses == -1 ? "Infinite" : String.valueOf(config.superActivatorUses)))));
        superActivatorItem.offer(Keys.DISPLAY_NAME, Component.text("ItemDuct Super Activator", NamedTextColor.GOLD));

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

    @SuppressWarnings("unchecked")
    public static boolean isActivator(ItemStack item)
    {
        if (item.getType().isAnyOf(ItemTypes.HOPPER))
        {
            Enchantment ench = Enchantment.builder().type(EnchantmentTypes.LOOTING).level(1).build();
            return item.get(Keys.APPLIED_ENCHANTMENTS).orElse(Collections.emptyList()).contains(ench);
        }
        return false;
    }

    public static void consumeActivator(ServerPlayer player, ItemStack itemInHand)
    {
        if (player.get(Keys.GAME_MODE).get() != GameModes.CREATIVE.get())
        {
            ItemStack newStack = itemInHand.copy();
            ItemStack sepStack = itemInHand.copy();

            Integer uses = newStack.get(ItemductData.USES).orElse(0);
            uses--;

            if (uses <= 0) // Item used up?
            {
                if (uses == -2) // or infinite usage?
                {
                    uses++;
                }
                else
                {
                    newStack.setQuantity(itemInHand.getQuantity() - 1);
                }
                sepStack.setQuantity(0);
            }
            else
            {
                sepStack.setQuantity(newStack.getQuantity() - 1);
                newStack.setQuantity(1);
            }
            newStack.offer(ItemductData.USES, uses);
            ItemductManager.updateUses(newStack);

            player.setItemInHand(HandTypes.MAIN_HAND, newStack);
            player.getInventory().offer(sepStack);
        }
    }
}
