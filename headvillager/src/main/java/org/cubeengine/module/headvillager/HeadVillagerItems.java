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
package org.cubeengine.module.headvillager;

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

public interface HeadVillagerItems {


    static void register(RegisterDataPackValueEvent<RecipeRegistration> event)
    {
        RecipeRegistration recipe = ShapedCraftingRecipe.builder().aisle("eee", "ehe", "eee")
                            .where('e', Ingredient.of(ItemTypes.EMERALD_BLOCK))
                            .where('h', Ingredient.of(ItemTypes.DRAGON_HEAD))
                            .result(headItem())
                            .key(ResourceKey.of(PluginHeadVillager.HEADVILLAGER_ID, "head"))
                            .build();
        event.register(recipe);
    }

    static ItemStack headItem()
    {
        final ItemStack head = ItemStack.of(ItemTypes.PLAYER_HEAD);
        head.offer(HeadVillagerData.VILLAGER, "head");
        final GameProfile profile = GameProfile.of(UUID.fromString("01bf9227-95fd-413f-af29-4ace675801bc")).withProperty(ProfileProperty.of(ProfileProperty.TEXTURES,
    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODZhZGRiZDVkZWRhZDQwOTk5NDczYmU0YTdmNDhmNjIzNmE3OWEwZGNlOTcxYjVkYmQ3MzcyMDE0YWUzOTRkIn19fQ=="));
        head.offer(Keys.GAME_PROFILE, profile);
        head.offer(Keys.CUSTOM_NAME, Component.text("Master Headsman's Pendant", NamedTextColor.GOLD));
        head.offer(Keys.LORE, Arrays.asList(Component.text("Use on a Villager to convert his Job", NamedTextColor.GRAY)));
        return head;
    }
}
