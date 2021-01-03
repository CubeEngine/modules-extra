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
import java.util.stream.Collectors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import org.cubeengine.libcube.service.task.SpongeTaskManager;
import org.cubeengine.module.terra.data.TerraItems;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.transaction.BlockTransaction;
import org.spongepowered.api.block.transaction.BlockTransactionReceipt;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.NotifyNeighborBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.UseItemStackEvent;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.WorldArchetype;
import org.spongepowered.api.world.biome.BiomeType;
import org.spongepowered.api.world.dimension.DimensionTypes;
import org.spongepowered.api.world.server.ServerWorld;
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
            final ResourceKey key = ResourceKey.of(PluginTerra.TERRA_ID, player.getName().toLowerCase());
//            final GeneratorModifierType buffet = Sponge.getRegistry().getCatalogRegistry().get(GeneratorModifierType.class, ResourceKey.resolve("buffet")).get();
//            final DataContainer settings = buffet.getDefaultGeneratorSettings();
//            settings.set(DataQuery.of("biome_source", "type"), "minecraft:checkerboard");
//            List<BiomeType> biomeTypeList = TerraItems.getBiomesForItem(event.getItemStackInUse());
//            settings.set(DataQuery.of("biome_source", "options", "biomes"), biomeTypeList.stream().map(BiomeType::getKey).map(ResourceKey::asString).collect(Collectors.toList()));
            final WorldArchetype archeType = WorldArchetype.builder()
                                                           .dimensionType(DimensionTypes.OVERWORLD.get())
//                                                           .generatorModifierType(buffet)
                                                           .keepSpawnLoaded(false)
                                                           .generateSpawnOnLoad(false)
//                                                           .generatorSettings(settings)
//                                                           .generateStructures(false)
                                                           .build();
            taskManager.runTask(() -> afterUseItem(key, archeType, player));
        }
    }

    // TODO remove me
//    @Listener
//    public void onBlockAll(ChangeBlockEvent.All event)
//    {
//        for (BlockTransaction transaction : event.getTransactions())
//        {
//            final Vector3i pos = transaction.getOriginal().getPosition();
//            if (pos.getX() >> 4 == -16 && pos.getZ() >> 4 == -16)
//            {
////                transaction.setValid(false);
//                if (!event.getCause().first(ServerPlayer.class).isPresent())
//                {
//                    event.setCancelled(true);
//                }
//
//                return;
//            }
//        }
//
////        System.out.println("ChangeBlockEvent.All:");
////        for (BlockTransaction receipt : event.getTransactions())
////        {
////
////            System.out.println(receipt.getOperation().getKey().asString() + " at " +
////                                   receipt.getOriginal().getPosition() + " " +
////                                   receipt.getOriginal().getState().getType().getKey().asString() + "->" +
////                                   receipt.getFinal().getState().getType().getKey().asString());
////        }
//    }

//    @Listener
//    public void onBlock(ChangeBlockEvent.Post event)
//    {
////        if (event.getWorld().getKey().getNamespace().equals(PluginTerra.TERRA_ID))
//        {
//            System.out.println("ChangeBlockEvent.Post:");
//            for (BlockTransactionReceipt receipt : event.getReceipts())
//            {
//
//                System.out.println(receipt.getOperation().getKey().asString() + " at " +
//                                receipt.getOriginal().getPosition() + " " +
//                                       receipt.getOriginal().getState().getType().getKey().asString() + "->" +
//                                       receipt.getFinal().getState().getType().getKey().asString());
//            }
//        }
//    }

    private void afterUseItem(ResourceKey key, WorldArchetype archeType, ServerPlayer player)
    {
        this.time = System.currentTimeMillis();
        System.out.println("del");
        Sponge.getServer().getWorldManager().deleteWorld(key);
        System.out.println("prop" + (System.currentTimeMillis() - this.time) + "ms");
        Sponge.getServer().getWorldManager().createProperties(key, archeType).thenAccept(prop -> {
            System.out.println("world" + (System.currentTimeMillis() - this.time) + "ms");
            Sponge.getServer().getWorldManager().loadWorld(prop).thenAccept(w -> {
                taskManager.runAsynchronousTask(Terra.class, () -> {
                    setupWorld(w);
                    taskManager.runTask(Terra.class, () -> tpPlayer(player, w));
                });
            });
        });
    }


    private void tpPlayer(ServerPlayer player, ServerWorld w)
    {
        System.out.println("tp" + (System.currentTimeMillis() - this.time) + "ms");
        ServerLocation spawnLoc = w.getLocation(w.getProperties().getSpawnPosition());
        Sponge.getServer().getTeleportHelper().getSafeLocation(spawnLoc).orElse(spawnLoc);
        player.setLocation(spawnLoc);
        final List<PotionEffect> list = player.get(Keys.POTION_EFFECTS).orElse(Collections.emptyList());
        list.removeIf(effect -> effect.getType() == PotionEffectTypes.BLINDNESS.get());
        list.add(PotionEffect.of(PotionEffectTypes.BLINDNESS, 0, 40));
        player.offer(Keys.POTION_EFFECTS, list);
    }

    private void setupWorld(ServerWorld w)
    {
        System.out.println("setup" + (System.currentTimeMillis() - this.time) + "ms");
        final Vector3i spawn = w.getProperties().getSpawnPosition();
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
