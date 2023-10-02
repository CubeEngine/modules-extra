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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.event.lifecycle.RegisterDataPackValueEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.query.QueryTypes;
import org.spongepowered.api.item.recipe.RecipeRegistration;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe;

import static org.spongepowered.api.item.ItemTypes.WRITABLE_BOOK;
import static org.spongepowered.api.item.ItemTypes.WRITTEN_BOOK;

public class WriterItems
{
    public static void registerRecipes(RegisterDataPackValueEvent<RecipeRegistration>event)
    {
        final RecipeRegistration revertWrittenBook = CraftingRecipe
            .shapelessBuilder().addIngredients(ItemTypes.LEATHER, WRITTEN_BOOK)
            .result(grid -> {
                final ItemStack writtenBook = grid.query(QueryTypes.ITEM_TYPE, WRITTEN_BOOK).peek();
                final List<String> lines = writtenBook.get(Keys.PAGES).orElse(
                    Collections.emptyList()).stream().map(line -> GsonComponentSerializer.gson().serialize(line)).collect(
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
