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
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.cubeengine.libcube.service.task.SpongeTaskManager;
import org.cubeengine.libcube.util.ItemUtil;
import org.cubeengine.module.terra.data.TerraItems;
import org.cubeengine.module.terra.data.TerraItems.Essence;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.entity.projectile.Potion;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.ChangeEntityPotionEffectEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.UseItemStackEvent;
import org.spongepowered.api.registry.RegistryReference;
import org.spongepowered.api.util.Color;
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
    @Inject private I18n i18n;

    private CompletableFuture<Void> future;

    @Listener
    public void onUseItem(UseItemStackEvent.Finish event, @First ServerPlayer player)
    {
        if (TerraItems.isTerraEssence(event.getItemStackInUse()))
        {
            if (this.future != null && !this.future.isDone())
            {
                i18n.send(ChatType.ACTION_BAR, player, MessageType.NEGATIVE, "You don't feel like drinking this now.");
                return;
            }

            TerraItems.getEssenceForItem(event.getItemStackInUse()).ifPresent(essence -> runEssenceCode(player, essence));
        }
    }

    private void runEssenceCode(ServerPlayer player, Essence essence)
    {
        final ResourceKey worldKey = ResourceKey.of(PluginTerra.TERRA_ID, player.getName().toLowerCase());
        final List<RegistryReference<Biome>> biomeList = essence.getBiomes();

        final Random random = player.getWorld().getRandom();
        final List<AttributedBiome> biomes = biomeList.stream().map(biome ->
            AttributedBiome.of(biome, BiomeAttributes.of(random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat()))).collect(Collectors.toList());

        final MultiNoiseBiomeConfig multiNoiseBiomeConfig = MultiNoiseBiomeConfig.builder().biomes(biomes).build();

        final NoiseGeneratorConfig noiseGeneratorConfig = NoiseGeneratorConfig.overworld();

        final WorldTemplate template = WorldTemplate.builder()
                                                    .from(WorldTemplate.overworld())
                                                    .key(worldKey)
                                                    .worldType(WorldTypes.OVERWORLD)
                                                    .serializationBehavior(SerializationBehavior.NONE)
                                                    .displayName(Component.text("Dream world by " + player.getName()))
                                                    .generator(ChunkGenerator.noise(BiomeProvider.multiNoise(multiNoiseBiomeConfig), noiseGeneratorConfig))
                                                    .difficulty(Difficulties.HARD)
                                                    .loadOnStartup(false)
                                                    .build();
        taskManager.runTask(() -> afterUseItem(worldKey, template, player));
    }

    @Listener
    public void onSplash(ChangeEntityPotionEffectEvent event, @First Potion potion) {
        final ServerPlayer player = event.getCause().first(ServerPlayer.class).orElse(null);
        if (player == null)
        {
            return;
        }
        if (this.future != null && !this.future.isDone())
        {
            ItemUtil.spawnItem(potion.getServerLocation(), potion.item().get().createStack());
            return;
        }

        TerraItems.getEssenceForItem(potion.item().get()).ifPresent(essence -> runEssenceCode(player, essence));
    }

    private void afterUseItem(ResourceKey worldKey, WorldTemplate template, ServerPlayer player)
    {
        final WorldManager wm = Sponge.getServer().getWorldManager();
        if (player.getWorld().getKey().equals(worldKey)) {
            player.setLocation(ServerLocation.of(wm.defaultWorld(), wm.defaultWorld().getProperties().spawnPosition()));
        }

        CompletableFuture<Boolean> worldDeletedFuture = CompletableFuture.completedFuture(true);
        if (wm.world(worldKey).isPresent()) {
            wm.world(worldKey).get().getProperties().setSerializationBehavior(SerializationBehavior.NONE);
            worldDeletedFuture = wm.unloadWorld(worldKey).thenCompose(b -> wm.deleteWorld(worldKey));
        }

        future = worldDeletedFuture.thenCompose(b -> {
            wm.saveTemplate(template);
            return wm.loadWorld(template);
        }).thenAccept(w -> {
            taskManager.runTask(() -> tpPlayer(player, w));
        }).exceptionally(e -> {
            e.printStackTrace();
            return null;
        });
    }


    private void tpPlayer(ServerPlayer player, ServerWorld w)
    {
        setupWorld(w);
        ServerLocation spawnLoc = w.getLocation(w.getProperties().spawnPosition());
        spawnLoc = Sponge.getServer().getTeleportHelper().getSafeLocation(spawnLoc, 50, 10).orElse(spawnLoc);
        player.setLocation(spawnLoc);
        final List<PotionEffect> list = player.get(Keys.POTION_EFFECTS).orElse(Collections.emptyList());
        list.removeIf(effect -> effect.getType() == PotionEffectTypes.BLINDNESS.get());
        list.add(PotionEffect.of(PotionEffectTypes.BLINDNESS, 0, 40));
        player.offer(Keys.POTION_EFFECTS, list);
    }

    private void setupWorld(ServerWorld w)
    {
        final Vector3i spawn = w.getProperties().spawnPosition();
        w.getBorder().setCenter(spawn.getX(), spawn.getZ());
        w.getBorder().setDiameter(16 * 17);
    }
}
