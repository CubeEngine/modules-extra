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
package org.cubeengine.module.traders;

import java.util.Arrays;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.event.lifecycle.RegisterDataPackValueEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.recipe.RecipeRegistration;
import org.spongepowered.api.item.recipe.crafting.Ingredient;
import org.spongepowered.api.item.recipe.crafting.ShapedCraftingRecipe;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.property.ProfileProperty;

public interface TradersItems {


    static void register(RegisterDataPackValueEvent<RecipeRegistration> event)
    {
        RecipeRegistration fisherRecipe = ShapedCraftingRecipe.builder().aisle("eee", "ehe", "eee")
                            .where('e', Ingredient.of(ItemTypes.EMERALD_BLOCK))
                            .where('h', Ingredient.of(ItemTypes.CONDUIT))
                            .result(conduitHead())
                            .key(ResourceKey.of(PluginTraders.TRADERS_ID, "fisher"))
                            .build();
        RecipeRegistration icemanRecipe = ShapedCraftingRecipe.builder().aisle("eee", "ehe", "eee")
                                                        .where('e', Ingredient.of(ItemTypes.EMERALD_BLOCK))
                                                        .where('h', Ingredient.of(ItemTypes.BLUE_ICE))
                                                        .result(iceHead())
                                                        .key(ResourceKey.of(PluginTraders.TRADERS_ID, "ice"))
                                                        .build();
        event.register(fisherRecipe);
        event.register(icemanRecipe);
    }

    static ItemStack conduitHead()
    {
        final ItemStack head = ItemStack.of(ItemTypes.PLAYER_HEAD);
        head.offer(TradersData.VILLAGER, "fisher");
        final GameProfile profile = GameProfile.of(UUID.fromString("c0bbeabc-c17a-45af-995c-6d5b6e048442"), "Mini-Conduit").withProperty(ProfileProperty.of(ProfileProperty.TEXTURES,
    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTI5NWExMWVlNjE5ZWQ1NzYxYmY3YTdlYTE5MGM3NWQyYmVlYjdkZWYxYjE1NWRlNjAzZmYyMTdhNmM4NzEyNyJ9fX0="));
        head.offer(Keys.GAME_PROFILE, profile);
        head.offer(Keys.CUSTOM_NAME, Component.text("Mini-Conduit", NamedTextColor.GOLD));
        head.offer(Keys.LORE, Arrays.asList(Component.text("Use on a Wandering Trader to unlock special trades", NamedTextColor.GRAY)));
        return head;
    }

    static ItemStack iceHead()
    {
        final ItemStack head = ItemStack.of(ItemTypes.PLAYER_HEAD);
        head.offer(TradersData.VILLAGER, "ice");
        // 9f55e0a3-1911-4aac-8977-0ad7525d6b84
        final GameProfile profile = GameProfile.of(UUID.fromString("c0bbeabc-c17a-45af-995c-6d5b6e048442"), "Icy Rune").withProperty(ProfileProperty.of(ProfileProperty.TEXTURES,
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGRiYTY0MmVmZmZhMTNlYzM3MzBlYWZjNTkxNGFiNjgxMTVjMWY5OTg4MDNmNzQ0NTJlMmUwY2QyNmFmMGI4In19fQ=="));
        head.offer(Keys.GAME_PROFILE, profile);
        head.offer(Keys.CUSTOM_NAME, Component.text("Icy Rune", NamedTextColor.GOLD));
        head.offer(Keys.LORE, Arrays.asList(Component.text("Use on a Wandering Trader to unlock special trades", NamedTextColor.GRAY)));
        return head;
    }
}
