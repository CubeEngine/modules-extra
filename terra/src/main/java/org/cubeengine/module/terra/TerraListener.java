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
package org.cubeengine.module.terra;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.libcube.service.task.SpongeTaskManager;
import org.cubeengine.module.terra.data.TerraItems;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.UseItemStackEvent;
import org.spongepowered.api.registry.RegistryReference;
import org.spongepowered.api.world.SerializationBehavior;
import org.spongepowered.api.world.WorldTypes;
import org.spongepowered.api.world.biome.AttributedBiome;
import org.spongepowered.api.world.biome.Biome;
import org.spongepowered.api.world.biome.BiomeAttributes;
import org.spongepowered.api.world.biome.provider.BiomeProvider;
import org.spongepowered.api.world.biome.provider.MultiNoiseBiomeConfig;
import org.spongepowered.api.world.difficulty.Difficulties;
import org.spongepowered.api.world.generation.ChunkGenerator;
import org.spongepowered.api.world.generation.config.NoiseGeneratorConfig;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.server.WorldManager;
import org.spongepowered.api.world.server.WorldTemplate;
import org.spongepowered.math.vector.Vector3i;
import org.spongepowered.plugin.PluginContainer;

@Singleton
public class TerraListener {

    @Inject private PluginContainer plugin;
    @Inject private SpongeTaskManager taskManager;

    private long time;

    @Listener
    public void onUseItem(UseItemStackEvent.Finish event, @First ServerPlayer player)
    {
        if (TerraItems.isTerraEssence(event.getItemStackInUse()))
        {
            final ResourceKey worldKey = ResourceKey.of(PluginTerra.TERRA_ID, player.getName().toLowerCase());

            List<RegistryReference<Biome>> biomeList = TerraItems.getBiomesForItem(event.getItemStackInUse());
            final Random random = player.getWorld().getRandom();
            final List<AttributedBiome> biomes = biomeList.stream().map(biome ->
                AttributedBiome.of(biome, BiomeAttributes.of(random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat()))).collect(Collectors.toList());

            final MultiNoiseBiomeConfig multiNoiseBiomeConfig = MultiNoiseBiomeConfig.builder().biomes(biomes)
                                                                                     .build();

            final NoiseGeneratorConfig noiseGeneratorConfig = NoiseGeneratorConfig.overworld();

            final WorldTemplate template = WorldTemplate.builder()
                                                        .from(WorldTemplate.overworld())
                                                        .key(worldKey)
                                                        .worldType(WorldTypes.OVERWORLD)
                                                        .generateSpawnOnLoad(false)
                                                        .displayName(Component.text("Dream world by " + player.getName()))
                                                        .generator(ChunkGenerator.noise(BiomeProvider.multiNoise(multiNoiseBiomeConfig), noiseGeneratorConfig))
                                                        .difficulty(Difficulties.HARD)
                                                        .loadOnStartup(false)
                                                        .build();
            taskManager.runTask(() -> afterUseItem(worldKey, template, player));
        }
    }

    private void afterUseItem(ResourceKey worldKey, WorldTemplate template, ServerPlayer player)
    {
        final WorldManager wm = Sponge.getServer().getWorldManager();
        this.time = System.currentTimeMillis();

        if (player.getWorld().getKey().equals(worldKey)) {
            player.setLocation(ServerLocation.of(wm.defaultWorld(), wm.defaultWorld().getProperties().spawnPosition()));
        }

        CompletableFuture<Boolean> worldDeletedFuture = CompletableFuture.completedFuture(true);
        if (wm.world(worldKey).isPresent()) {
            wm.world(worldKey).get().getProperties().setSerializationBehavior(SerializationBehavior.NONE);
            System.out.println("unload" + (System.currentTimeMillis() - this.time) + "ms");
            worldDeletedFuture = wm.unloadWorld(worldKey).thenCompose(b -> {
                System.out.println("delete" + (System.currentTimeMillis() - this.time) + "ms");
                return wm.deleteWorld(worldKey);
            });
        }

        worldDeletedFuture.thenCompose(b -> {
            System.out.println("save" + (System.currentTimeMillis() - this.time) + "ms");
            wm.saveTemplate(template);
            System.out.println("load" + (System.currentTimeMillis() - this.time) + "ms");
            return wm.loadWorld(template);
        }).thenAccept(w -> {
            taskManager.runTask(Terra.class, () -> tpPlayer(player, w));
        }).exceptionally(e -> {
            player.sendMessage(Identity.nil(), Component.text("OH NO! " + e.getMessage(), NamedTextColor.DARK_RED));
            e.printStackTrace();
            return null;
        });
    }


    private void tpPlayer(ServerPlayer player, ServerWorld w)
    {
        setupWorld(w);
        System.out.println("tp" + (System.currentTimeMillis() - this.time) + "ms");
        ServerLocation spawnLoc = w.getLocation(w.getProperties().spawnPosition());
        spawnLoc = Sponge.getServer().getTeleportHelper().getSafeLocation(spawnLoc).orElse(spawnLoc);
        player.setLocation(spawnLoc);
        final List<PotionEffect> list = player.get(Keys.POTION_EFFECTS).orElse(Collections.emptyList());
        list.removeIf(effect -> effect.getType() == PotionEffectTypes.BLINDNESS.get());
        list.add(PotionEffect.of(PotionEffectTypes.BLINDNESS, 0, 40));
        player.offer(Keys.POTION_EFFECTS, list);
    }

    private void setupWorld(ServerWorld w)
    {
        System.out.println("setup" + (System.currentTimeMillis() - this.time) + "ms");
        final Vector3i spawn = w.getProperties().spawnPosition();
        w.getBorder().setCenter(spawn.getX(), spawn.getZ());
        w.getBorder().setDiameter(16 * 17);
        final int cx = spawn.getX() / 16;
        final int cz = spawn.getZ() / 16;
        final int GENERATE_RADIUS = 2;
        System.out.println("pregen" + (System.currentTimeMillis() - this.time) + "ms");
        for (int gcx = cx - GENERATE_RADIUS; gcx < cx + GENERATE_RADIUS; gcx++)
        {
            for (int gcz = cz - GENERATE_RADIUS; gcz < cz + GENERATE_RADIUS; gcz++)
            {
                w.loadChunk(gcx, 0, gcz, true);
            }
        }
    }
}
