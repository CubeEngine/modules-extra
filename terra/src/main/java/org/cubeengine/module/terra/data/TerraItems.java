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
import org.cubeengine.module.terra.PluginTerra;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.lifecycle.RegisterDataPackValueEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.recipe.RecipeRegistration;
import org.spongepowered.api.item.recipe.crafting.Ingredient;
import org.spongepowered.api.item.recipe.crafting.ShapelessCraftingRecipe;
import org.spongepowered.api.registry.RegistryReference;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.world.biome.BiomeType;
import org.spongepowered.api.world.biome.BiomeTypes;
import org.spongepowered.api.world.server.ServerWorld;

public class TerraItems
{

    public static final ItemStack INK_BOTTLE = ItemStack.of(ItemTypes.POTION.get());
    public static final ItemStack TERRA_ESSENCE = ItemStack.of(ItemTypes.POTION.get());

    public static void registerRecipes(RegisterDataPackValueEvent event)
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
        TERRA_ESSENCE.offer(Keys.COLOR, Color.WHITE);
        TERRA_ESSENCE.offer(Keys.CUSTOM_NAME, Component.text("Terra Essence"));
        TERRA_ESSENCE.offer(Keys.APPLIED_ENCHANTMENTS, Arrays.asList(Enchantment.of(EnchantmentTypes.UNBREAKING.get(), 1)));
        TERRA_ESSENCE.offer(Keys.POTION_EFFECTS, Arrays.asList(PotionEffect.of(PotionEffectTypes.BLINDNESS.get(), 0, 20 * 60)));
        TERRA_ESSENCE.offer(Keys.HIDE_MISCELLANEOUS, true);
        TERRA_ESSENCE.offer(Keys.HIDE_ENCHANTMENTS, true);
        final RecipeRegistration terraEssenceRecipe = ShapelessCraftingRecipe.builder()
                               .addIngredients(ItemTypes.SUGAR, ItemTypes.BLAZE_POWDER, ItemTypes.GUNPOWDER, ItemTypes.REDSTONE)
                               .addIngredients(Ingredient.of(INK_BOTTLE))
                               .result(grid -> TerraItems.getCraftedEssence(), TERRA_ESSENCE)
                               .key(ResourceKey.of(PluginTerra.TERRA_ID, "terraessence"))
                               .build();
        event.register(terraEssenceRecipe);

        final RecipeRegistration randomTerraEssence = ShapelessCraftingRecipe.builder()
             .addIngredients(ItemTypes.SUGAR, ItemTypes.BLAZE_POWDER, ItemTypes.GUNPOWDER, ItemTypes.REDSTONE, ItemTypes.NETHER_STAR)
             .addIngredients(Ingredient.of(INK_BOTTLE))
             .result(grid -> TerraItems.getRandomCraftedEssence(), TERRA_ESSENCE)
             .key(ResourceKey.of(PluginTerra.TERRA_ID, "random_terraessence"))
             .build();
        event.register(randomTerraEssence);
        
    }

    private enum Essence
    {
        GREEN(Color.GREEN, Arrays.asList(BiomeTypes.PLAINS, BiomeTypes.SUNFLOWER_PLAINS, BiomeTypes.FOREST, BiomeTypes.BIRCH_FOREST, BiomeTypes.TALL_BIRCH_FOREST, BiomeTypes.BIRCH_FOREST_HILLS, BiomeTypes.FLOWER_FOREST), Arrays.asList(BiomeTypes.RIVER)),
        DARK_GREEN(Color.DARK_GREEN, Arrays.asList(BiomeTypes.DARK_FOREST, BiomeTypes.DARK_FOREST_HILLS, BiomeTypes.SWAMP), Arrays.asList(BiomeTypes.RIVER)),
        LIME(Color.LIME, Arrays.asList(BiomeTypes.JUNGLE, BiomeTypes.MODIFIED_JUNGLE, BiomeTypes.JUNGLE_EDGE, BiomeTypes.MODIFIED_JUNGLE_EDGE, BiomeTypes.BAMBOO_JUNGLE), Arrays.asList(BiomeTypes.RIVER, BiomeTypes.BEACH)),
        MAGENTA(Color.MAGENTA, Arrays.asList(BiomeTypes.MUSHROOM_FIELD_SHORE, BiomeTypes.MUSHROOM_FIELDS), Arrays.asList(BiomeTypes.OCEAN, BiomeTypes.COLD_OCEAN, BiomeTypes.WARM_OCEAN, BiomeTypes.BEACH)),
        YELLOW(Color.YELLOW, Arrays.asList(BiomeTypes.DESERT, BiomeTypes.DESERT_LAKES, BiomeTypes.DESERT_HILLS, BiomeTypes.SAVANNA, BiomeTypes.SHATTERED_SAVANNA, BiomeTypes.SHATTERED_SAVANNA_PLATEAU, BiomeTypes.SAVANNA_PLATEAU), Arrays.asList(BiomeTypes.RIVER)),
        RED(Color.RED, Arrays.asList(BiomeTypes.BADLANDS, BiomeTypes.ERODED_BADLANDS, BiomeTypes.WOODED_BADLANDS_PLATEAU, BiomeTypes.MODIFIED_WOODED_BADLANDS_PLATEAU, BiomeTypes.BADLANDS_PLATEAU), Arrays.asList(BiomeTypes.DESERT_HILLS, BiomeTypes.RIVER)),
        WHITE(Color.WHITE, Arrays.asList(BiomeTypes.TAIGA, BiomeTypes.TAIGA_HILLS, BiomeTypes.TAIGA_MOUNTAINS, BiomeTypes.SNOWY_TAIGA, BiomeTypes.SNOWY_TAIGA_HILLS, BiomeTypes.SNOWY_TAIGA_MOUNTAINS, BiomeTypes.GIANT_TREE_TAIGA, BiomeTypes.GIANT_TREE_TAIGA_HILLS, BiomeTypes.GIANT_SPRUCE_TAIGA, BiomeTypes.GIANT_SPRUCE_TAIGA_HILLS), Arrays.asList(BiomeTypes.FROZEN_RIVER, BiomeTypes.RIVER, BiomeTypes.SNOWY_BEACH)),
        GRAY(Color.GRAY, Arrays.asList(BiomeTypes.SNOWY_TUNDRA, BiomeTypes.SNOWY_MOUNTAINS, BiomeTypes.MOUNTAINS, BiomeTypes.MOUNTAIN_EDGE, BiomeTypes.GRAVELLY_MOUNTAINS, BiomeTypes.MODIFIED_GRAVELLY_MOUNTAINS, BiomeTypes.WOODED_MOUNTAINS, BiomeTypes.FROZEN_OCEAN, BiomeTypes.DEEP_FROZEN_OCEAN, BiomeTypes.SNOWY_BEACH, BiomeTypes.ICE_SPIKES), Arrays.asList(BiomeTypes.FROZEN_RIVER, BiomeTypes.RIVER)),
        BLUE(Color.BLUE, Arrays.asList(BiomeTypes.OCEAN, BiomeTypes.DEEP_OCEAN, BiomeTypes.COLD_OCEAN, BiomeTypes.DEEP_COLD_OCEAN, BiomeTypes.LUKEWARM_OCEAN, BiomeTypes.DEEP_LUKEWARM_OCEAN, BiomeTypes.WARM_OCEAN, BiomeTypes.DEEP_WARM_OCEAN), Arrays.asList(BiomeTypes.BEACH)),
        DARK_MAGENTA(Color.DARK_MAGENTA, Arrays.asList(BiomeTypes.SMALL_END_ISLANDS, BiomeTypes.END_MIDLANDS, BiomeTypes.END_BARRENS, BiomeTypes.END_HIGHLANDS), Arrays.asList()),
        // does not work with normal terrain, BiomeTypes.THE_VOID
        PURPLE(Color.PURPLE, Arrays.asList(BiomeTypes.NETHER_WASTES, BiomeTypes.CRIMSON_FOREST, BiomeTypes.WARPED_FOREST, BiomeTypes.SOUL_SAND_VALLEY, BiomeTypes.BASALT_DELTAS), Arrays.asList()),
        ;


        private final Color color;
        private final List<RegistryReference<BiomeType>> biomeList;
        private final List<RegistryReference<BiomeType>> additionalBiomeList;
        private List<BiomeType> biomes;

        Essence(Color color, List<RegistryReference<BiomeType>> biomeList, List<RegistryReference<BiomeType>> additionalBiomeList)
        {
            this.color = color;
            this.biomeList = biomeList;
            this.additionalBiomeList = additionalBiomeList;
        }

        public List<BiomeType> getBiomes(ServerWorld world)
        {
            if (this.biomes == null)
            {
                biomes = new ArrayList<>();
                biomeList.forEach(b -> biomes.add(b.get(world.registries())));
                additionalBiomeList.forEach(b -> biomes.add(b.get(world.registries())));
            }
            return biomes;
        }

        public boolean hasBiome(BiomeType biomeType, ServerWorld world)
        {
            for (RegistryReference<BiomeType> biome : biomeList)
            {
                if (biome.get(world.registries()).equals(biomeType))
                {
                    return true;
                }
            }
            return false;
        }
    }

    private static Random random = new Random();

    private static ItemStack getRandomCraftedEssence()
    {
        final Essence essence = Essence.values()[random.nextInt(Essence.values().length)];
        final ItemStack craftedEssence = TerraItems.TERRA_ESSENCE.copy();
        craftedEssence.offer(Keys.COLOR, essence.color);
        return craftedEssence;
    }

    private static ItemStack getCraftedEssence()
    {
        final ItemStack craftedEssence = TerraItems.TERRA_ESSENCE.copy();
        final Optional<ServerPlayer> player = Sponge.getServer().getCauseStackManager().getCurrentCause().first(ServerPlayer.class);
        final Optional<BiomeType> biome = player.map(p -> p.getWorld().getBiome(p.getBlockPosition()));
        if (biome.isPresent())
        {
            Color color = Color.BLACK;
            for (Essence value : Essence.values())
            {
                if (value.hasBiome(biome.get(), player.get().getWorld()))
                {
                    color = value.color;
                    break;
                }
            }
            craftedEssence.offer(Keys.COLOR, color);
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

    public static List<BiomeType> getBiomesForItem(ItemStackSnapshot stack, ServerWorld world)
    {
        final Color color = stack.get(Keys.COLOR).orElse(Color.BLACK);
        for (Essence value : Essence.values())
        {
            if (value.color.equals(color))
            {
                return value.getBiomes(world);
            }
        }
        return Arrays.asList(BiomeTypes.PLAINS.get(world.registries()));
    }
}
