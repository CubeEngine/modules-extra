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
package org.cubeengine.module.writer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.event.lifecycle.RegisterDataPackValueEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.query.QueryTypes;
import org.spongepowered.api.item.recipe.RecipeRegistration;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe;
import org.spongepowered.api.item.recipe.crafting.Ingredient;

import static org.spongepowered.api.item.ItemTypes.WRITABLE_BOOK;
import static org.spongepowered.api.item.ItemTypes.WRITTEN_BOOK;

public class WriterItems
{
    public static void registerRecipes(RegisterDataPackValueEvent<RecipeRegistration>event)
    {
        final ItemStack stack = ItemStack.of(WRITABLE_BOOK);
        stack.offer(Keys.HIDE_ENCHANTMENTS, true);
        stack.offer(Keys.APPLIED_ENCHANTMENTS, Arrays.asList(Enchantment.of(EnchantmentTypes.MENDING, 1)));
        stack.offer(WriterData.WRITER, true);

        final RecipeRegistration signWriterBook = CraftingRecipe
            .shapedBuilder().aisle("sss", "sbs", "sss")
            .where('s', Ingredient.of(ItemTypes.ACACIA_SIGN, ItemTypes.BIRCH_SIGN, ItemTypes.DARK_OAK_SIGN, ItemTypes.JUNGLE_SIGN, ItemTypes.OAK_SIGN, ItemTypes.SPRUCE_SIGN))
            .where('b', Ingredient.of(WRITABLE_BOOK))
            .result(grid -> {
                final ItemStack newStack = grid.peekAt(4).get();
                newStack.offer(WriterData.WRITER, true);
                newStack.offer(Keys.HIDE_ENCHANTMENTS, true);
                newStack.offer(Keys.APPLIED_ENCHANTMENTS, Arrays.asList(Enchantment.of(EnchantmentTypes.MENDING, 1)));
                return newStack;
             }, stack)
            .key(ResourceKey.of(PluginWriter.WRITER_ID, "signwriterbook"))
            .build();
        event.register(signWriterBook);

        final RecipeRegistration revertWrittenBook = CraftingRecipe
            .shapelessBuilder().addIngredients(ItemTypes.LEATHER, WRITTEN_BOOK)
            .result(grid -> {
                final ItemStack writtenBook = grid.query(QueryTypes.ITEM_TYPE, WRITTEN_BOOK).peek();
                final List<String> lines = writtenBook.get(Keys.PAGES).orElse(
                    Collections.emptyList()).stream().map(line -> PlainComponentSerializer.plain().serialize(line)).collect(
                    Collectors.toList());
                final ItemStack writableBook = ItemStack.of(WRITABLE_BOOK);
                writableBook.offer(Keys.PLAIN_PAGES, lines);
                return writableBook;
            }, ItemStack.of(WRITABLE_BOOK))
            .key(ResourceKey.of(PluginWriter.WRITER_ID, "revertwritterbook"))
            .build();
        event.register(revertWrittenBook);
    }
}
