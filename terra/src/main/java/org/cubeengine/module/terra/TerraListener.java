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
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.entity.CookingEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.UseItemStackEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.scheduler.ScheduledTask;
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

    private Queue<Supplier<CompletableFuture<ServerWorld>>> worldGenerationQueue = new LinkedList<>();
    private CompletableFuture<ServerWorld> currentGeneration = null;

    private Map<ResourceKey, CompletableFuture<ServerWorld>> futureWorlds = new HashMap<>();
    private Map<UUID, UUID> potions = new HashMap<>();

    @Listener
    public void onUseItem(UseItemStackEvent.Finish event, @First ServerPlayer player)
    {
        if (!event.getContext().get(EventContextKeys.USED_HAND).get().equals(HandTypes.MAIN_HAND.get()))
        {
            return;
        }
        final ItemStackSnapshot terraPotion = event.getItemStackInUse();
        if (TerraItems.isTerraEssence(terraPotion))
        {
            final Optional<UUID> uuid = event.getItemStackInUse().get(TerraData.WORLD_UUID);
            final Optional<ResourceKey> worldKey = terraPotion.get(TerraData.WORLD_KEY).map(ResourceKey::resolve);
            if (uuid.isPresent() && worldKey.isPresent())
            {
                final Optional<ServerWorld> world = Sponge.getServer().getWorldManager().world(worldKey.get());
                if (world.isPresent() && world.get().getUniqueId().equals(uuid.get()))
                {
                    this.tpPlayer(player, world.get());
                    return;
                }
                event.setCancelled(true);
                player.getWorld().playSound(Sound.sound(SoundTypes.BLOCK_GLASS_BREAK, Source.PLAYER, 1, 1), player.getPosition());
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
        for (ServerWorld world : new ArrayList<>(Sponge.getServer().getWorldManager().worlds()))
        {
            if (!futureWorlds.containsKey(world.getKey()) || futureWorlds.get(world.getKey()).isDone())
            {
                if (world.getKey().getNamespace().equals(PluginTerra.TERRA_ID))
                {
                    if (world.getPlayers().isEmpty())
                    {
                        logger.info("Deleting empty Terra world: " + world.getKey());
                        futureWorlds.remove(world.getKey());
                        Sponge.getServer().getWorldManager().deleteWorld(world.getKey());
                    }
                }
            }
        }
    }

    @Listener
    public void onCampfireTick(CookingEvent.Tick event)
    {
        if (!(event.getBlockEntity() instanceof Campfire))
        {
            return;
        }
        final ItemStackSnapshot original = event.getTransactions().get(0).getOriginal();
        if (TerraItems.isTerraEssence(original))
        {
            final ResourceKey worldKey = ResourceKey.resolve(original.get(TerraData.WORLD_KEY).get());
            final CompletableFuture<ServerWorld> futureWorld = this.futureWorlds.get(worldKey);
            if (futureWorld == null || !futureWorld.isDone())
            {
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onUseItem(UseItemStackEvent.Tick event, @First ServerPlayer player)
    {
        if (!event.getContext().get(EventContextKeys.USED_HAND).get().equals(HandTypes.MAIN_HAND.get()) || !TerraItems.isTerraEssence(event.getItemStackInUse()))
        {
            return;
        }
        final Optional<UUID> uuid = event.getItemStackInUse().get(TerraData.WORLD_UUID);
        if (!uuid.isPresent())
        {
            if (event.getItemStackInUse().get(TerraData.POTION_UUID).isPresent())
            {
                i18n.send(ChatType.ACTION_BAR, player, MessageType.NEGATIVE, "Bad Potion");
                player.setItemInHand(HandTypes.MAIN_HAND, ItemStack.empty());
                player.getWorld().playSound(Sound.sound(SoundTypes.BLOCK_GLASS_BREAK, Source.PLAYER, 1, 1), player.getPosition());
                event.setCancelled(true);
                return;
            }
            i18n.send(ChatType.ACTION_BAR, player, MessageType.NEGATIVE, "The liquid is too cold to drink.");
            event.setRemainingDuration(20);
            event.setCancelled(true);
            return;
        }

        if (event.getRemainingDuration() > 15)
        {
            event.setRemainingDuration(15); // Gulp it down fast
            final List<PotionEffect> potionEffects = player.get(Keys.POTION_EFFECTS).orElse(new ArrayList<>());
            potionEffects.add(PotionEffect.of(PotionEffectTypes.BLINDNESS, 0, 60));
            player.offer(Keys.POTION_EFFECTS, potionEffects);
        }
    }

    @Listener
    public void onStartPotionHeatup(InteractBlockEvent.Secondary event, @First ServerPlayer player)
    {
        if (event.getBlock().getState().getType().isAnyOf(BlockTypes.CAMPFIRE))
        {
            final Campfire campfire = (Campfire) event.getBlock().getLocation().get().getBlockEntity().get();
            if (campfire.getInventory().freeCapacity() <= 0)
            {
                return;
            }
            final ItemStack itemInHand = player.getItemInHand(HandTypes.MAIN_HAND);
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
                    player.getWorld().playSound(Sound.sound(SoundTypes.BLOCK_GLASS_BREAK, Source.PLAYER, 1, 1), player.getPosition());
                    event.setCancelled(true);
                    return;
                }
                if (player.getWorld().getKey().getNamespace().equals(PluginTerra.TERRA_ID))
                {
                    i18n.send(ChatType.ACTION_BAR, player, MessageType.NEGATIVE, "It feels wrong to do that here.");
                    return;
                }
                final Essence essence = TerraItems.getEssenceForItem(itemInHand.createSnapshot()).get();
                final ResourceKey worldKey = ResourceKey.of(PluginTerra.TERRA_ID, player.getName().toLowerCase());
                if (futureWorlds.getOrDefault(worldKey, CompletableFuture.completedFuture(null)).isDone())
                {
                    itemInHand.offer(Keys.LORE, Arrays.asList(coldPotionLore(player), potionOwnerLore(player, worldKey.getValue())));
                    itemInHand.offer(TerraData.WORLD_KEY, worldKey.asString());
                    final UUID potionUuid = UUID.randomUUID();
                    this.potions.put(player.getUniqueId(), potionUuid);
                    itemInHand.offer(TerraData.POTION_UUID, potionUuid);

                    final WorldTemplate template = essence.createWorldTemplate(player, worldKey);

                    this.evacuateWorld(worldKey);
                    final CompletableFuture<ServerWorld> doneFuture = new CompletableFuture<>();
                    futureWorlds.put(worldKey, doneFuture);
                    worldGenerationQueue.add(() -> generateWorld(worldKey, template, doneFuture));

                    i18n.send(ChatType.ACTION_BAR, player, MessageType.POSITIVE, "Tiny images form in the bubbling essence");
                }
                else if (futureWorlds.get(worldKey) != null) // Currently generating
                {
                    final UUID curPotionUuid = this.potions.get(player.getUniqueId());
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
        Sponge.getServer().getWorldManager().world(worldKey).ifPresent(w -> {
            final ServerWorld defaultWorld = Sponge.getServer().getWorldManager().defaultWorld();
            final Collection<ServerPlayer> players = new ArrayList<>(w.getPlayers());
            for (ServerPlayer player : players)
            {
                i18n.send(player, MessageType.NEUTRAL, "The world you were in disappeared as if it was a dream.");
                player.setLocation(defaultWorld.getLocation(defaultWorld.getProperties().spawnPosition()));
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

    private CompletableFuture<ServerWorld> generateWorld(ResourceKey worldKey, WorldTemplate template, CompletableFuture<ServerWorld> doneFuture)
    {
        // Evacuate
        final WorldManager wm = Sponge.getServer().getWorldManager();

        // Unload and delete
        CompletableFuture<Boolean> worldDeletedFuture = CompletableFuture.completedFuture(true);
        if (wm.world(worldKey).isPresent()) {
            wm.world(worldKey).get().getProperties().setSerializationBehavior(SerializationBehavior.NONE);
            worldDeletedFuture = wm.unloadWorld(worldKey).thenCompose(b -> wm.deleteWorld(worldKey));
        }

        // Save Template and Load
        return worldDeletedFuture.thenCompose(b -> {
            wm.saveTemplate(template);
            return wm.loadWorld(template);
        }).thenApply(w -> {
            doneFuture.complete(w);
            return w;
        });
    }

    private void tpPlayer(ServerPlayer player, ServerWorld w)
    {
        setupWorld(w);
        ServerLocation spawnLoc = w.getLocation(w.getProperties().spawnPosition());
        if (spawnLoc.getPosition().getY() == 127)
        {
            spawnLoc = Sponge.getServer().getTeleportHelper().getSafeLocation(spawnLoc.add(Vector3i.UP.mul(-60)), 50, 10).orElse(spawnLoc);
        }
        spawnLoc = Sponge.getServer().getTeleportHelper().getSafeLocation(spawnLoc, 50, 10).orElse(spawnLoc);
        player.setLocation(spawnLoc);
    }

    private void setupWorld(ServerWorld w)
    {
        final Vector3i spawn = w.getProperties().spawnPosition();
        w.getBorder().setCenter(spawn.getX(), spawn.getZ());
        w.getBorder().setDiameter(16 * 17);
    }

    public ItemStack finalizePotion(ItemStack terraPotion)
    {
        final ResourceKey worldKey = ResourceKey.resolve(terraPotion.get(TerraData.WORLD_KEY).get());
        final CompletableFuture<ServerWorld> futureWorld = this.futureWorlds.get(worldKey);
        if (futureWorld != null && futureWorld.isDone() && !futureWorld.isCompletedExceptionally())
        {
            final Optional<ServerPlayer> optPlayer = Sponge.getServer().getPlayer(worldKey.getValue());
            final Audience audience = optPlayer.map(Audience.class::cast).orElse(Sponge.getGame().getSystemSubject());
            final ArrayList<Component> lore = new ArrayList<>();
            lore.add(i18n.translate(audience, Style.style(NamedTextColor.GOLD), "The liquid feels warm and glows with excitement."));
            lore.add(i18n.translate(audience, Style.style(NamedTextColor.GRAY), "Drink it!"));
            lore.add(potionOwnerLore(audience, worldKey.getValue()));
            terraPotion.offer(Keys.LORE, lore);
            terraPotion.offer(TerraData.WORLD_UUID, futureWorld.join().getUniqueId());
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
        for (Entry<ResourceKey, CompletableFuture<ServerWorld>> entry : futureWorlds.entrySet())
        {
            if (entry.getValue().isCompletedExceptionally())
            {
                i18n.send(audience, MessageType.NEGATIVE, " - {name} failed to generate.", entry.getKey().asString());
            }
            else if (entry.getValue().isDone())
            {
                i18n.send(audience, MessageType.POSITIVE, " - {name} is done generating.", entry.getKey().asString());
            }
            else
            {
                i18n.send(audience, MessageType.NEUTRAL, " - {name} is generating.", entry.getKey().asString());
            }
        }
    }

    public void cancelAll(Audience audience)
    {
        for (Entry<ResourceKey, CompletableFuture<ServerWorld>> entry : futureWorlds.entrySet())
        {
            if (!entry.getValue().isDone())
            {
                entry.getValue().completeExceptionally(new InterruptedException("Interrupted by Command"));
                i18n.send(audience, MessageType.POSITIVE, " - Cancelled generating {name}", entry.getKey().asString());
            }
        }
        worldGenerationQueue.clear();
    }

    public void doGenerate()
    {
        if (this.worldGenerationQueue.isEmpty())
        {
            return;
        }
        if (currentGeneration == null || currentGeneration.isDone())
        {
            currentGeneration = this.worldGenerationQueue.poll().get();
        }
    }
}
