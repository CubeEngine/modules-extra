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
package org.cubeengine.module.mechanism;

import org.cubeengine.module.mechanism.sign.HiddenButton;
import org.cubeengine.module.mechanism.sign.HiddenLever;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.event.lifecycle.RegisterDataPackValueEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.recipe.RecipeRegistration;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe;
import org.spongepowered.api.item.recipe.crafting.Ingredient;

import static org.spongepowered.api.item.ItemTypes.OAK_SIGN;

public class MechanismItems
{
    public static void registerRecipes(RegisterDataPackValueEvent<RecipeRegistration> event, MechanismManager manager)
    {
        final ItemStack hiddenLeverSign = manager.makeSign(HiddenLever.class, ItemStack.of(OAK_SIGN));

        final RecipeRegistration hiddenLever = CraftingRecipe
            .shapedBuilder().aisle("rlr", "lsl", "rlr")
            .where('s', Ingredient.of(ItemTypes.ACACIA_SIGN, ItemTypes.BIRCH_SIGN, ItemTypes.DARK_OAK_SIGN, ItemTypes.JUNGLE_SIGN, ItemTypes.OAK_SIGN, ItemTypes.SPRUCE_SIGN))
            .where('r', Ingredient.of(ItemTypes.REDSTONE))
            .where('l', Ingredient.of(ItemTypes.LEVER))
            .result(grid -> {
                final ItemStack newStack = grid.peekAt(4).get();
                manager.makeSign(HiddenLever.class, newStack);
                newStack.setQuantity(1);
                return newStack;
             }, hiddenLeverSign)
            .key(ResourceKey.of(PluginMechanism.MECHANISM_ID, "hidden-lever"))
            .build();
        event.register(hiddenLever);

        final ItemStack hiddenButtonSign = manager.makeSign(HiddenButton.class, ItemStack.of(OAK_SIGN));

        final RecipeRegistration hiddenButton = CraftingRecipe
            .shapedBuilder().aisle("rbr", "bsb", "rbr")
            .where('s', Ingredient.of(ItemTypes.ACACIA_SIGN, ItemTypes.BIRCH_SIGN, ItemTypes.DARK_OAK_SIGN, ItemTypes.JUNGLE_SIGN, ItemTypes.OAK_SIGN, ItemTypes.SPRUCE_SIGN))
            .where('r', Ingredient.of(ItemTypes.REDSTONE))
            .where('b', Ingredient.of(ItemTypes.STONE_BUTTON))
            .result(grid -> {
                final ItemStack newStack = grid.peekAt(4).get();
                manager.makeSign(HiddenButton.class, newStack);
                newStack.setQuantity(1);
                return newStack;
            }, hiddenButtonSign)
            .key(ResourceKey.of(PluginMechanism.MECHANISM_ID, "hidden-button"))
            .build();
        event.register(hiddenButton);

    }
}
