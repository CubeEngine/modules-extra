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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        GREEN("Green Landscape", Color.GREEN, Arrays.asList(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.TALL_BIRCH_FOREST, Biomes.BIRCH_FOREST_HILLS, Biomes.FLOWER_FOREST), Arrays.asList(Biomes.RIVER)),
        DARK_GREEN("Dark Swamp", Color.DARK_GREEN, Arrays.asList(Biomes.DARK_FOREST, Biomes.DARK_FOREST_HILLS, Biomes.SWAMP), Arrays.asList(Biomes.RIVER)),
        LIME("Viney Jungle", Color.LIME, Arrays.asList(Biomes.JUNGLE, Biomes.MODIFIED_JUNGLE, Biomes.JUNGLE_EDGE, Biomes.MODIFIED_JUNGLE_EDGE, Biomes.BAMBOO_JUNGLE), Arrays.asList(Biomes.RIVER, Biomes.BEACH)),
        MAGENTA("Mushrooms", Color.MAGENTA, Arrays.asList(Biomes.MUSHROOM_FIELD_SHORE, Biomes.MUSHROOM_FIELDS), Arrays.asList()),
        YELLOW("Deserted Savanna", Color.YELLOW, Arrays.asList(Biomes.DESERT, Biomes.DESERT_LAKES, Biomes.DESERT_HILLS, Biomes.SAVANNA, Biomes.SHATTERED_SAVANNA, Biomes.SHATTERED_SAVANNA_PLATEAU, Biomes.SAVANNA_PLATEAU), Arrays.asList(Biomes.RIVER)),
        RED("Colorful Badlands", Color.RED, Arrays.asList(Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS_PLATEAU, Biomes.MODIFIED_WOODED_BADLANDS_PLATEAU, Biomes.BADLANDS_PLATEAU), Arrays.asList(Biomes.DESERT_HILLS, Biomes.RIVER)),
        WHITE("Cold Mountains", Color.WHITE, Arrays.asList(Biomes.TAIGA, Biomes.TAIGA_HILLS, Biomes.TAIGA_MOUNTAINS, Biomes.SNOWY_TAIGA, Biomes.SNOWY_TAIGA_HILLS, Biomes.SNOWY_TAIGA_MOUNTAINS, Biomes.GIANT_TREE_TAIGA, Biomes.GIANT_TREE_TAIGA_HILLS, Biomes.GIANT_SPRUCE_TAIGA, Biomes.GIANT_SPRUCE_TAIGA_HILLS), Arrays.asList(Biomes.FROZEN_RIVER, Biomes.RIVER, Biomes.SNOWY_BEACH)),
        GRAY("Frozen World", Color.GRAY, Arrays.asList(Biomes.SNOWY_TUNDRA, Biomes.SNOWY_MOUNTAINS, Biomes.MOUNTAINS, Biomes.MOUNTAIN_EDGE, Biomes.GRAVELLY_MOUNTAINS, Biomes.MODIFIED_GRAVELLY_MOUNTAINS, Biomes.WOODED_MOUNTAINS, Biomes.ICE_SPIKES), Arrays.asList(Biomes.FROZEN_RIVER, Biomes.RIVER)),
        BLUE("Coral Reef", Color.BLUE, Arrays.asList(Biomes.LUKEWARM_OCEAN), Arrays.asList()),
        DARK_MAGENTA("The End", Color.DARK_MAGENTA, Arrays.asList(Biomes.SMALL_END_ISLANDS, Biomes.END_MIDLANDS, Biomes.END_BARRENS, Biomes.END_HIGHLANDS), Arrays.asList()),
        // does not work with normal terrain, Biomes.THE_VOID
        PURPLE("Hellscape", Color.PURPLE, Arrays.asList(Biomes.NETHER_WASTES, Biomes.CRIMSON_FOREST, Biomes.WARPED_FOREST, Biomes.SOUL_SAND_VALLEY, Biomes.BASALT_DELTAS), Arrays.asList()),
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
        craftedEssence.offer(Keys.CUSTOM_NAME, Component.text(essence.name, NamedTextColor.AQUA));
        return craftedEssence;
    }

    private static ItemStack getCraftedEssence()
    {
        final ItemStack craftedEssence = TerraItems.TERRA_ESSENCE.copy();
        final Optional<ServerPlayer> player = Sponge.getServer().getCauseStackManager().getCurrentCause().first(ServerPlayer.class);
        final Optional<Biome> biome = player.map(p -> p.getWorld().getBiome(p.getBlockPosition()));
        if (biome.isPresent())
        {
            Essence essence = Essence.GREEN;
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
