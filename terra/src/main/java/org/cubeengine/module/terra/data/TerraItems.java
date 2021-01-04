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
package org.cubeengine.module.terra.data;

import static org.spongepowered.api.world.biome.Biomes.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.cubeengine.module.terra.PluginTerra;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.lifecycle.RegisterDataPackValueEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.recipe.RecipeRegistration;
import org.spongepowered.api.item.recipe.crafting.Ingredient;
import org.spongepowered.api.item.recipe.crafting.ShapelessCraftingRecipe;
import org.spongepowered.api.registry.RegistryReference;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.world.biome.Biome;
import org.spongepowered.api.world.biome.Biomes;
import org.spongepowered.api.world.server.ServerWorld;

public class TerraItems
{

    public static final ItemStack INK_BOTTLE = ItemStack.of(ItemTypes.POTION.get());
    public static final ItemStack SPLASH_INK_BOTTLE = ItemStack.of(ItemTypes.SPLASH_POTION.get());
    public static final ItemStack TERRA_ESSENCE = ItemStack.of(ItemTypes.POTION.get());
    public static final ItemStack SPLASH_TERRA_ESSENCE = ItemStack.of(ItemTypes.SPLASH_POTION.get());

    public static void registerRecipes(RegisterDataPackValueEvent<RecipeRegistration>event)
    {
        INK_BOTTLE.offer(Keys.COLOR, Color.BLACK);
        INK_BOTTLE.offer(Keys.CUSTOM_NAME, Component.text("Ink Bottle"));
        INK_BOTTLE.offer(Keys.HIDE_MISCELLANEOUS, true);
        final RecipeRegistration inkBottleRecipe = ShapelessCraftingRecipe.builder()
                              .addIngredients(ItemTypes.GLASS_BOTTLE, ItemTypes.INK_SAC)
                              .result(INK_BOTTLE)
                              .key(ResourceKey.of(PluginTerra.TERRA_ID, "inkbottle"))
                              .build();
        event.register(inkBottleRecipe);

        SPLASH_INK_BOTTLE.offer(Keys.COLOR, Color.BLACK);
        SPLASH_INK_BOTTLE.offer(Keys.CUSTOM_NAME, Component.text("Splash Ink Bottle"));
        SPLASH_INK_BOTTLE.offer(Keys.HIDE_MISCELLANEOUS, true);
        final RecipeRegistration splashInkBottleRecipe = ShapelessCraftingRecipe.builder()
                              .addIngredients(ItemTypes.GLASS_BOTTLE, ItemTypes.INK_SAC, ItemTypes.GUNPOWDER)
                              .result(SPLASH_INK_BOTTLE)
                              .key(ResourceKey.of(PluginTerra.TERRA_ID, "splash_inkbottle"))
                              .build();
        event.register(splashInkBottleRecipe);

        TERRA_ESSENCE.offer(Keys.COLOR, Color.WHITE);
        TERRA_ESSENCE.offer(Keys.CUSTOM_NAME, Component.text("Terra Essence"));
        TERRA_ESSENCE.offer(Keys.POTION_EFFECTS, Arrays.asList(PotionEffect.of(PotionEffectTypes.BLINDNESS.get(), 0, 20 * 60)));
        TERRA_ESSENCE.offer(Keys.HIDE_MISCELLANEOUS, true);
        final RecipeRegistration terraEssenceRecipe = ShapelessCraftingRecipe.builder()
                               .addIngredients(ItemTypes.SUGAR, ItemTypes.ENDER_PEARL)
                               .addIngredients(Ingredient.of(INK_BOTTLE))
                               .result(grid -> TerraItems.getCraftedEssence(), TERRA_ESSENCE)
                               .key(ResourceKey.of(PluginTerra.TERRA_ID, "terraessence"))
                               .build();
        event.register(terraEssenceRecipe);

        final RecipeRegistration randomTerraEssence = ShapelessCraftingRecipe.builder()
             .addIngredients(ItemTypes.SUGAR, ItemTypes.ENDER_PEARL, ItemTypes.NETHER_STAR)
             .addIngredients(Ingredient.of(INK_BOTTLE))
             .result(grid -> TerraItems.getRandomCraftedEssence(TERRA_ESSENCE), TERRA_ESSENCE)
             .key(ResourceKey.of(PluginTerra.TERRA_ID, "random_terraessence"))
             .build();
        event.register(randomTerraEssence);

        SPLASH_TERRA_ESSENCE.offer(Keys.COLOR, Color.WHITE);
        SPLASH_TERRA_ESSENCE.offer(Keys.CUSTOM_NAME, Component.text("Splash Terra Essence"));
        SPLASH_TERRA_ESSENCE.offer(Keys.POTION_EFFECTS, Arrays.asList(PotionEffect.of(PotionEffectTypes.BLINDNESS.get(), 0, 20 * 60)));
        SPLASH_TERRA_ESSENCE.offer(Keys.HIDE_MISCELLANEOUS, true);
        final RecipeRegistration splashEssence = ShapelessCraftingRecipe.builder()
                                 .addIngredients(ItemTypes.SUGAR, ItemTypes.ENDER_PEARL)
                                 .addIngredients(Ingredient.of(SPLASH_INK_BOTTLE))
                                 .result(grid -> TerraItems.getRandomCraftedEssence(SPLASH_TERRA_ESSENCE), TERRA_ESSENCE)
                                 .key(ResourceKey.of(PluginTerra.TERRA_ID, "splash_terraessence"))
                                 .build();
        event.register(splashEssence);

        final RecipeRegistration splashRandomTerraEssence = ShapelessCraftingRecipe.builder()
                                     .addIngredients(ItemTypes.SUGAR, ItemTypes.ENDER_PEARL, ItemTypes.NETHER_STAR)
                                     .addIngredients(Ingredient.of(INK_BOTTLE))
                                     .result(grid -> TerraItems.getRandomCraftedEssence(SPLASH_TERRA_ESSENCE), TERRA_ESSENCE)
                                     .key(ResourceKey.of(PluginTerra.TERRA_ID, "splash_random_terraessence"))
                                     .build();
        event.register(splashRandomTerraEssence);


    }

    public enum Essence
    {
        GREEN_LANDSCAPE("Green Landscape", Color.ofRgb(0x336633), Arrays.asList(PLAINS, SUNFLOWER_PLAINS, FOREST, BIRCH_FOREST, TALL_BIRCH_FOREST, BIRCH_FOREST_HILLS, FLOWER_FOREST), Arrays.asList()),
        SWAMP_FOREST("Dark Swamp", Color.ofRgb(0x333333), Arrays.asList(DARK_FOREST, DARK_FOREST_HILLS, SWAMP), Arrays.asList()),
        JUNGLE("Viney Jungle", Color.ofRgb(0x339933), Arrays.asList(Biomes.JUNGLE, MODIFIED_JUNGLE, JUNGLE_EDGE, MODIFIED_JUNGLE_EDGE, BAMBOO_JUNGLE), Arrays.asList(BEACH)),
        MUSHROOMS("Mushrooms", Color.ofRgb(0x996666), Arrays.asList(MUSHROOM_FIELD_SHORE, MUSHROOM_FIELDS), Arrays.asList(COLD_OCEAN)),
        SAVANNA("Dry Savanna", Color.ofRgb(0x666633), Arrays.asList(Biomes.SAVANNA, SHATTERED_SAVANNA, SHATTERED_SAVANNA_PLATEAU, SAVANNA_PLATEAU), Arrays.asList()),
        DESERT("Hot Desert", Color.ofRgb(0xCCCC99), Arrays.asList(Biomes.DESERT, DESERT_LAKES, DESERT_HILLS), Arrays.asList()),
        MESA("Colorful Badlands", Color.ofRgb(0xCC6633), Arrays.asList(BADLANDS, ERODED_BADLANDS, WOODED_BADLANDS_PLATEAU, MODIFIED_WOODED_BADLANDS_PLATEAU, BADLANDS_PLATEAU), Arrays.asList( DESERT_HILLS)),
        TAIGA("Chilly Mountains", Color.ofRgb(0x333300), Arrays.asList(Biomes.TAIGA, TAIGA_HILLS, TAIGA_MOUNTAINS, SNOWY_TAIGA, SNOWY_TAIGA_HILLS, SNOWY_TAIGA_MOUNTAINS, GIANT_TREE_TAIGA, GIANT_TREE_TAIGA_HILLS, GIANT_SPRUCE_TAIGA, GIANT_SPRUCE_TAIGA_HILLS), Arrays.asList(SNOWY_BEACH, COLD_OCEAN)),
        TUNDRA("Cold Mountains", Color.ofRgb(0xFFFFFF), Arrays.asList(SNOWY_TUNDRA, SNOWY_MOUNTAINS, MOUNTAINS, MOUNTAIN_EDGE, GRAVELLY_MOUNTAINS, MODIFIED_GRAVELLY_MOUNTAINS, WOODED_MOUNTAINS), Arrays.asList()),
        // Special Biomes
        ICE_SPIKES("Frozen World", Color.ofRgb(0x6699CC), Arrays.asList(Biomes.ICE_SPIKES), Arrays.asList()),
        CORAL_REEF("Coral Reef", Color.ofRgb(0xCC66CC), Arrays.asList(WARM_OCEAN), Arrays.asList()),
        FLOWERY_FOREST("Flowery Forest", Color.ofRgb(0xCC6600), Arrays.asList(FLOWER_FOREST), Arrays.asList()),
        // Needs special casing
        END("The End", Color.ofRgb(0x999966), Arrays.asList(SMALL_END_ISLANDS, END_MIDLANDS, END_BARRENS, END_HIGHLANDS), Arrays.asList()),
        NETHER("Hellscape", Color.ofRgb(0x330000), Arrays.asList(NETHER_WASTES, CRIMSON_FOREST, WARPED_FOREST, SOUL_SAND_VALLEY, BASALT_DELTAS), Arrays.asList()),
        ;


        private final Color color;
        private final List<RegistryReference<Biome>> biomeList;
        private final List<RegistryReference<Biome>> additionalBiomeList;
        private final String name;

        Essence(String name, Color color, List<RegistryReference<Biome>> biomeList, List<RegistryReference<Biome>> additionalBiomeList)
        {
            this.name = name;
            this.color = color;
            this.biomeList = biomeList;
            this.additionalBiomeList = additionalBiomeList;
        }

        public List<RegistryReference<Biome>> getBiomes()
        {
            final List<RegistryReference<Biome>> allBiomes = new ArrayList<>();
            allBiomes.addAll(this.biomeList);
            allBiomes.addAll(this.additionalBiomeList);
            return allBiomes;
        }

        public boolean hasBiome(Biome Biome, ServerWorld world)
        {
            for (RegistryReference<Biome> biome : biomeList)
            {
                if (biome.get(world.registries()).equals(Biome))
                {
                    return true;
                }
            }
            return false;
        }
    }

    private static Random random = new Random();

    private static ItemStack getRandomCraftedEssence(ItemStack baseStack)
    {
        final Essence essence = Essence.values()[random.nextInt(Essence.values().length)];
        final ItemStack craftedEssence = baseStack.copy();
        craftedEssence.offer(Keys.COLOR, essence.color);
        craftedEssence.offer(Keys.CUSTOM_NAME, baseStack.get(Keys.CUSTOM_NAME).get().append(Component.space()).append(Component.text(essence.name, TextColor.color(essence.color.getRgb()))));
        return craftedEssence;
    }

    private static ItemStack getCraftedEssence()
    {
        final ItemStack craftedEssence = TerraItems.TERRA_ESSENCE.copy();
        final Optional<ServerPlayer> player = Sponge.getServer().getCauseStackManager().getCurrentCause().first(ServerPlayer.class);
        final Optional<Biome> biome = player.map(p -> p.getWorld().getBiome(p.getBlockPosition()));
        if (biome.isPresent())
        {
            Essence essence = Essence.GREEN_LANDSCAPE;
            for (Essence value : Essence.values())
            {
                if (value.hasBiome(biome.get(), player.get().getWorld()))
                {
                    essence = value;
                    break;
                }
            }
            craftedEssence.offer(Keys.COLOR, essence.color);
            craftedEssence.offer(Keys.CUSTOM_NAME, Component.text(essence.name, NamedTextColor.AQUA));

        }
        return craftedEssence;
    }

    public static boolean isTerraEssence(ItemStackSnapshot stack)
    {
        if (!stack.getType().isAnyOf(ItemTypes.POTION))
        {
            return false;
        }
        return stack.get(Keys.COLOR).isPresent(); // TODO later check custom data
    }

    public static Optional<Essence> getEssenceForItem(ItemStackSnapshot stack)
    {
        final Color color = stack.get(Keys.COLOR).orElse(Color.BLACK);
        for (Essence value : Essence.values())
        {
            if (value.color.equals(color))
            {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
