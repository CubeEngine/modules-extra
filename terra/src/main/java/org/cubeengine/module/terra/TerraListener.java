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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.logscribe.Log;
import org.cubeengine.module.terra.data.TerraData;
import org.cubeengine.module.terra.data.TerraItems;
import org.cubeengine.module.terra.data.TerraItems.Essence;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.entity.carrier.Campfire;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.entity.CookingEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.UseItemStackEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.SerializationBehavior;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.server.WorldManager;
import org.spongepowered.api.world.server.WorldTemplate;
import org.spongepowered.math.vector.Vector3i;

@Singleton
public class TerraListener
{
    @Inject private I18n i18n;
    @Inject private Log logger;
    @Inject private TaskManager taskManager;

    private Queue<WorldGeneration> worldGenerationQueue = new LinkedList<>();
    private WorldGeneration currentGeneration = null;

    private Map<ResourceKey, WorldGeneration> futureWorlds = new HashMap<>();
    private Map<UUID, UUID> potions = new HashMap<>();

    private class WorldGeneration
    {
        private ResourceKey worldKey;
        private WorldTemplate template;
        private CompletableFuture<ServerWorld> worldFuture;

        public WorldGeneration(ResourceKey worldKey, WorldTemplate template)
        {
            this.worldKey = worldKey;
            this.template = template;
        }

        private void generateWorld()
        {
            // Evacuate
            final WorldManager wm = Sponge.server().worldManager();

            // Unload and delete
            CompletableFuture<Boolean> worldDeletedFuture = CompletableFuture.completedFuture(true);
            if (wm.world(worldKey).isPresent()) {
                wm.world(worldKey).get().properties().setSerializationBehavior(SerializationBehavior.NONE);
                worldDeletedFuture = wm.unloadWorld(worldKey).thenCompose(b -> wm.deleteWorld(worldKey));
            }

            // Save Template and Load
            this.worldFuture = worldDeletedFuture.thenCompose(b -> {
                wm.saveTemplate(template);
                return wm.loadWorld(template);
            });

            this.worldFuture.handle((w, t) -> {
                if (t != null)
                {
                    logger.error(t, "Error while generating world {}", worldKey);
                }
                return null;
            });
        }

        public boolean isDone()
        {
            return this.worldFuture != null && this.worldFuture.isDone();
        }

        public boolean isReady()
        {
            return this.worldFuture != null && this.worldFuture.isDone() && !this.worldFuture.isCompletedExceptionally();
        }

        public ServerWorld getWorld()
        {
            try
            {
                return this.worldFuture.get();
            }
            catch (InterruptedException | ExecutionException e)
            {
                throw new IllegalStateException(e);
            }
        }

        public void cancel()
        {
            if (this.worldFuture != null)
            {
                this.worldFuture.completeExceptionally(new InterruptedException("Interrupted by Command"));
            }
        }
    }

    @Listener
    public void onUseItem(UseItemStackEvent.Finish event, @First ServerPlayer player)
    {
        if (!event.context().get(EventContextKeys.USED_HAND).get().equals(HandTypes.MAIN_HAND.get()))
        {
            return;
        }
        final ItemStackSnapshot terraPotion = event.itemStackInUse();
        if (TerraItems.isTerraEssence(terraPotion))
        {
            final Optional<UUID> uuid = event.itemStackInUse().get(TerraData.WORLD_UUID);
            final Optional<ResourceKey> worldKey = terraPotion.get(TerraData.WORLD_KEY).map(ResourceKey::resolve);
            if (uuid.isPresent() && worldKey.isPresent())
            {
                final Optional<ServerWorld> world = Sponge.server().worldManager().world(worldKey.get());
                if (world.isPresent() && world.get().uniqueId().equals(uuid.get()))
                {
                    this.tpPlayer(player, world.get());
                    return;
                }
                event.setCancelled(true);
                player.world().playSound(Sound.sound(SoundTypes.BLOCK_GLASS_BREAK, Source.PLAYER, 1, 1), player.position());
                player.setItemInHand(HandTypes.MAIN_HAND, ItemStack.empty());
                i18n.send(ChatType.ACTION_BAR, player, MessageType.NEGATIVE, "The potion broke. It must have been too old.");
                return;
            }
            i18n.send(ChatType.ACTION_BAR, player, MessageType.NEGATIVE, "The liquid is too cold to drink.");
            event.setCancelled(true);
        }
    }

    // Deletes Terra worlds without players
    public void checkForUnload(ScheduledTask task)
    {
        // TODO grace period
        final WorldManager wm = Sponge.server().worldManager();
        for (ServerWorld world : new ArrayList<>(wm.worlds()))
        {
            if (!futureWorlds.containsKey(world.key()) || futureWorlds.get(world.key()).isDone())
            {
                if (world.key().namespace().equals(PluginTerra.TERRA_ID))
                {
                    if (world.players().isEmpty())
                    {
                        logger.info("Deleting empty Terra world: " + world.key());
                        futureWorlds.remove(world.key());
                        wm.deleteWorld(world.key());
                    }
                }
            }
        }
    }

    @Listener
    public void onCampfireTick(CookingEvent.Tick event)
    {
        if (!(event.blockEntity() instanceof Campfire))
        {
            return;
        }
        final ItemStackSnapshot original = event.transactions().get(0).original();
        if (TerraItems.isTerraEssence(original))
        {
            final Optional<String> worldKeyString = original.get(TerraData.WORLD_KEY);
            if (worldKeyString.isPresent())
            {
                final ResourceKey worldKey = ResourceKey.resolve(worldKeyString.get());
                final WorldGeneration futureWorld = this.futureWorlds.get(worldKey);
                if (futureWorld == null || !futureWorld.isDone())
                {
                    event.setCancelled(true);
                }
            }
        }
    }

    @Listener
    public void onCampfireTick(CookingEvent.Finish event)
    {
        if (!(event.blockEntity() instanceof Campfire))
        {
            return;
        }
        final ItemStackSnapshot result = event.cookedItems().get(0);
        if (TerraItems.isTerraEssence(result))
        {
            event.blockEntity().world().playSound(Sound.sound(SoundTypes.ENTITY_GENERIC_EXTINGUISH_FIRE, Source.PLAYER, 5, 2f), event.blockEntity().blockPosition().toDouble());
        }
    }

    @Listener
    public void onUseItem(UseItemStackEvent.Tick event, @First ServerPlayer player)
    {
        final ItemStackSnapshot itemStackInUse = event.itemStackInUse();
        if (!event.context().get(EventContextKeys.USED_HAND).get().equals(HandTypes.MAIN_HAND.get()) || !TerraItems.isTerraEssence(itemStackInUse))
        {
            return;
        }
        if (TerraItems.isTerraEssence(itemStackInUse))
        {
            final Optional<UUID> uuid = itemStackInUse.get(TerraData.WORLD_UUID);
            if (!uuid.isPresent())
            {
                if (itemStackInUse.get(TerraData.POTION_UUID).isPresent())
                {
                    i18n.send(ChatType.ACTION_BAR, player, MessageType.NEGATIVE, "Bad Potion");
                    player.setItemInHand(HandTypes.MAIN_HAND, ItemStack.empty());
                    player.world().playSound(Sound.sound(SoundTypes.BLOCK_GLASS_BREAK, Source.PLAYER, 1, 1), player.position());
                    event.setCancelled(true);
                    return;
                }
                i18n.send(ChatType.ACTION_BAR, player, MessageType.NEGATIVE, "The liquid is too cold to drink.");
                event.setRemainingDuration(20);
                event.setCancelled(true);
                return;
            }

            if (event.remainingDuration() > 15)
            {
                event.setRemainingDuration(15); // Gulp it down fast
                final List<PotionEffect> potionEffects = player.get(Keys.POTION_EFFECTS).orElse(new ArrayList<>());
                potionEffects.add(PotionEffect.of(PotionEffectTypes.BLINDNESS, 0, 60));
                player.offer(Keys.POTION_EFFECTS, potionEffects);
            }
        }

    }

    @Listener
    public void onFatalDamage(DamageEntityEvent event, @Getter("entity") ServerPlayer player)
    {
        if (event.finalDamage() > player.health().get() && player.world().key().namespace().equals(PluginTerra.TERRA_ID))
        {
            event.setCancelled(true);
            List<Entity> leashed = new ArrayList<>();
            for (Entity nearbyEntity : player.nearbyEntities(5))
            {
                if (nearbyEntity.get(Keys.LEASH_HOLDER).map(holder -> holder.equals(player)).orElse(false))
                {
                    leashed.add(nearbyEntity);
                }
            }

            player.offer(Keys.FALL_DISTANCE, 0.0);
            player.offer(Keys.FIRE_TICKS, Ticks.of(0));
            player.offer(Keys.HEALTH, player.get(Keys.MAX_HEALTH).orElse(20.0));

            taskManager.runTask(() -> {
                i18n.send(player, MessageType.NEUTRAL, "The world you were in disappeared as if it was a dream.");

                final ServerWorld defaultWorld = Sponge.server().worldManager().defaultWorld();
                player.setLocation(defaultWorld.location(defaultWorld.properties().spawnPosition()));

                player.world().playSound(Sound.sound(SoundTypes.ITEM_TOTEM_USE, Source.PLAYER, 1, 1), player.position());
                for (Entity leashedEntity : leashed)
                {
                    leashedEntity.setLocation(player.serverLocation());
                }
            });
        }
    }

    @Listener
    public void onStartPotionHeatup(InteractBlockEvent.Secondary event, @First ServerPlayer player)
    {
        if (event.block().state().type().isAnyOf(BlockTypes.CAMPFIRE))
        {
            final Campfire campfire = (Campfire) event.block().location().get().blockEntity().get();
            if (campfire.inventory().freeCapacity() <= 0)
            {
                return;
            }
            final ItemStack itemInHand = player.itemInHand(HandTypes.MAIN_HAND);
            if (TerraItems.isTerraEssence(itemInHand.createSnapshot()))
            {
                if (itemInHand.get(TerraData.WORLD_UUID).isPresent())
                {
                    i18n.send(ChatType.ACTION_BAR, player, MessageType.NEUTRAL, "The essence is ready to drink");
                    event.setCancelled(true);
                    return;
                }
                if (itemInHand.get(TerraData.POTION_UUID).isPresent())
                {
                    i18n.send(ChatType.ACTION_BAR, player, MessageType.NEGATIVE, "Bad Potion");
                    player.setItemInHand(HandTypes.MAIN_HAND, ItemStack.empty());
                    player.world().playSound(Sound.sound(SoundTypes.BLOCK_GLASS_BREAK, Source.PLAYER, 1, 1), player.position());
                    event.setCancelled(true);
                    return;
                }
                if (player.world().key().namespace().equals(PluginTerra.TERRA_ID))
                {
                    i18n.send(ChatType.ACTION_BAR, player, MessageType.NEGATIVE, "It feels wrong to do that here.");
                    return;
                }
                final Essence essence = TerraItems.getEssenceForItem(itemInHand.createSnapshot()).get();
                final ResourceKey worldKey = ResourceKey.of(PluginTerra.TERRA_ID, player.name().toLowerCase());
                if (!futureWorlds.containsKey(worldKey) || futureWorlds.get(worldKey).isDone())
                {
                    itemInHand.offer(Keys.LORE, Arrays.asList(coldPotionLore(player), potionOwnerLore(player, worldKey.value())));
                    itemInHand.offer(TerraData.WORLD_KEY, worldKey.asString());
                    final UUID potionUuid = UUID.randomUUID();
                    this.potions.put(player.uniqueId(), potionUuid);
                    itemInHand.offer(TerraData.POTION_UUID, potionUuid);

                    final WorldTemplate template = essence.createWorldTemplate(player, worldKey);

                    this.evacuateWorld(worldKey);

                    final WorldGeneration worldGeneration = new WorldGeneration(worldKey, template);
                    futureWorlds.put(worldKey, worldGeneration);
                    worldGenerationQueue.add(worldGeneration);

                    i18n.send(ChatType.ACTION_BAR, player, MessageType.POSITIVE, "Tiny images form in the bubbling essence");
                }
                else if (futureWorlds.get(worldKey) != null) // Currently generating
                {
                    final UUID curPotionUuid = this.potions.get(player.uniqueId());
                    if (curPotionUuid == null || !curPotionUuid.equals(itemInHand.get(TerraData.POTION_UUID).orElse(null)))
                    {
                        event.setCancelled(true); // Only allow the current potion
                        i18n.send(ChatType.ACTION_BAR, player, MessageType.POSITIVE, "Only one essence at a time");
                    }
                    else
                    {
                        i18n.send(ChatType.ACTION_BAR, player, MessageType.POSITIVE, "It slowly heats up");
                    }
                }
            }
        }

    }

    public Component potionOwnerLore(Audience audience, String name)
    {
        return i18n.translate(audience, MessageType.POSITIVE, "The essence is attuned to {player}", name);
    }

    public Component coldPotionLore(Audience player)
    {
        return i18n.translate(player, Style.style(NamedTextColor.AQUA), "The liquid is freezing cold");
    }

    private void evacuateWorld(ResourceKey worldKey)
    {
        final WorldManager wm = Sponge.server().worldManager();
        final ServerWorld defaultWorld = wm.defaultWorld();
        wm.world(worldKey).ifPresent(w -> {
            final Collection<ServerPlayer> players = new ArrayList<>(w.players());
            for (ServerPlayer player : players)
            {
                i18n.send(player, MessageType.NEUTRAL, "The world you were in disappeared as if it was a dream.");
                player.setLocation(defaultWorld.location(defaultWorld.properties().spawnPosition()));
            }
        });
    }

// TODO event not called yet
//    @Listener
//    public void onSplash(ChangeEntityPotionEffectEvent event, @First Potion potion) {
//        final ServerPlayer player = event.getCause().first(ServerPlayer.class).orElse(null);
//        if (player == null)
//        {
//            return;
//        }
//        if (this.worldGenerationQueue != null && !this.worldGenerationQueue.isDone())
//        {
//            ItemUtil.spawnItem(potion.getServerLocation(), potion.item().get().createStack());
//            event.setCancelled(true);
//            return;
//        }
//
//    }


    private void tpPlayer(ServerPlayer player, ServerWorld w)
    {
        setupWorld(w);
        ServerLocation spawnLoc = w.location(w.properties().spawnPosition());
        if (spawnLoc.position().getY() == 127)
        {
            spawnLoc = Sponge.server().teleportHelper().findSafeLocation(spawnLoc.add(Vector3i.UP.mul(-60)), 50, 10).orElse(spawnLoc);
        }
        spawnLoc = Sponge.server().teleportHelper().findSafeLocation(spawnLoc, 50, 10).orElse(spawnLoc);
        player.setLocation(spawnLoc);
        player.playSound(Sound.sound(SoundTypes.BLOCK_END_PORTAL_SPAWN, Source.PLAYER, 1, 0.5f), player.position());
        player.playSound(Sound.sound(SoundTypes.BLOCK_PORTAL_TRIGGER, Source.PLAYER, 1, 2f), player.position());
    }

    private void setupWorld(ServerWorld w)
    {
        final Vector3i spawn = w.properties().spawnPosition();
        w.border().setCenter(spawn.getX(), spawn.getZ());
        w.border().setDiameter(16 * 17);
    }

    public ItemStack finalizePotion(ItemStack terraPotion)
    {
        final ResourceKey worldKey = ResourceKey.resolve(terraPotion.get(TerraData.WORLD_KEY).get());
        final WorldGeneration futureWorld = this.futureWorlds.get(worldKey);
        if (futureWorld != null && futureWorld.isReady())
        {
            final Optional<ServerPlayer> optPlayer = Sponge.server().player(worldKey.value());
            final Audience audience = optPlayer.map(Audience.class::cast).orElse(Sponge.game().systemSubject());
            final ArrayList<Component> lore = new ArrayList<>();
            lore.add(i18n.translate(audience, Style.style(NamedTextColor.GOLD), "The liquid feels warm and glows with excitement."));
            lore.add(i18n.translate(audience, Style.style(NamedTextColor.GRAY), "Drink it!"));
            lore.add(potionOwnerLore(audience, worldKey.value()));
            terraPotion.offer(Keys.LORE, lore);
            terraPotion.offer(TerraData.WORLD_UUID, futureWorld.getWorld().uniqueId());
            final UUID potionUuid = terraPotion.get(TerraData.POTION_UUID).get();
            this.potions.values().removeIf(uuid -> uuid.equals(potionUuid));
        }
        return terraPotion;
    }

    public Component hintPotionLore(Audience player)
    {
        return i18n.translate(player, Style.style(NamedTextColor.GRAY), "Try heating it on a campfire.");
    }

    public void printStatus(Audience audience)
    {
        i18n.send(audience, MessageType.POSITIVE, "Terra worlds ({amount} in queue):", worldGenerationQueue.size());
        for (Entry<ResourceKey, WorldGeneration> entry : futureWorlds.entrySet())
        {
            if (entry.getValue().isReady())
            {
                i18n.send(audience, MessageType.POSITIVE, " - {name} is done generating.", entry.getKey().asString());

            }
            else if (entry.getValue().isDone())
            {
                i18n.send(audience, MessageType.NEGATIVE, " - {name} failed to generate.", entry.getKey().asString());
            }
            else
            {
                final boolean current = currentGeneration == entry.getValue();
                if (current)
                {
                    final Optional<ServerWorld> world = Sponge.server().worldManager().world(entry.getKey());
                    if (world.isPresent())
                    {
                        i18n.send(audience, MessageType.NEUTRAL, " - {name} is generating. {amount}/441 chunks", entry.getKey().asString(), ((Collection<?>)world.get().loadedChunks()).size());
                    }
                    else
                    {
                        i18n.send(audience, MessageType.NEGATIVE, " - {name} is generating but the world is missing!?", entry.getKey().asString());
                    }
                }
                else
                {
                    i18n.send(audience, MessageType.NEUTRAL, " - {name} waiting to generate.", entry.getKey().asString());
                }
            }
        }
    }

    public void cancelAll(Audience audience)
    {
        for (Entry<ResourceKey, WorldGeneration> entry : futureWorlds.entrySet())
        {
            if (!entry.getValue().isDone())
            {
                entry.getValue().cancel();
                i18n.send(audience, MessageType.POSITIVE, " - Cancelled generating {name}", entry.getKey().asString());
            }
        }
        worldGenerationQueue.clear();
        futureWorlds.clear();
        currentGeneration = null;
    }

    public void doGenerate()
    {
        if (this.worldGenerationQueue.isEmpty())
        {
            return;
        }
        if (currentGeneration == null || currentGeneration.isDone())
        {
            this.currentGeneration = this.worldGenerationQueue.poll();
            this.currentGeneration.generateWorld();
        }
    }
}
