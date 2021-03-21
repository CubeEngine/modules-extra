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
package org.cubeengine.module.spawner;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.processor.Module;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.block.transaction.BlockTransaction;
import org.spongepowered.api.block.transaction.Operations;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityArchetype;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.weighted.RandomObjectTable;
import org.spongepowered.api.util.weighted.WeightedSerializableObject;
import org.spongepowered.api.util.weighted.WeightedTable;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.math.vector.Vector3i;

import static java.util.Collections.singletonList;
import static org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType.ACTION_BAR;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.data.type.HandTypes.MAIN_HAND;
import static org.spongepowered.api.entity.EntityTypes.*;
import static org.spongepowered.api.entity.living.player.gamemode.GameModes.CREATIVE;
import static org.spongepowered.api.event.Order.POST;
import static org.spongepowered.api.item.enchantment.EnchantmentTypes.LURE;
import static org.spongepowered.api.item.enchantment.EnchantmentTypes.SILK_TOUCH;

/**
 * A module to gather monster spawners with silk touch and reactivate them using spawneggs
 */
@Singleton
@Module
public class Spawner
{
    private ItemStack spawnerItem;
    private Permission eggPerms;
    private Permission breakPerm;
    private Permission dropEggPerm;
    private final Map<EntityType<?>, Permission> perms = new HashMap<>();
    private final Map<ResourceKey, Set<Vector3i>> brokenSpawners = new HashMap<>();

    @Inject private PermissionManager pm;
    @Inject private I18n i18n;
    private final Map<EntityType<?>, ItemType> eggs = new HashMap<>();
    private final Map<ItemType, EntityType<?>> entities = new HashMap<>();

    @Listener
    public void onEnable(StartingEngineEvent<Server> event)
    {
        this.eggPerms = pm.register(Spawner.class, "egg", "Allows creating all types of spawners with spawneggs", null);
        this.breakPerm = pm.register(Spawner.class, "break", "Allows obtaining inactive monster spawners", null);
        this.dropEggPerm = pm.register(Spawner.class, "drop-egg", "Allows dropping monster eggs from spawners", null);

        this.registerType(CREEPER, ItemTypes.CREEPER_SPAWN_EGG);
        this.registerType(SKELETON, ItemTypes.SKELETON_SPAWN_EGG);
        this.registerType(SPIDER, ItemTypes.SPIDER_SPAWN_EGG);
        this.registerType(ZOMBIE, ItemTypes.ZOMBIE_SPAWN_EGG);
        this.registerType(SLIME, ItemTypes.SLIME_SPAWN_EGG);
        this.registerType(GHAST, ItemTypes.GHAST_SPAWN_EGG);
        this.registerType(ZOMBIFIED_PIGLIN, ItemTypes.ZOMBIFIED_PIGLIN_SPAWN_EGG);
        this.registerType(ENDERMAN, ItemTypes.ENDERMAN_SPAWN_EGG);
        this.registerType(CAVE_SPIDER, ItemTypes.CAVE_SPIDER_SPAWN_EGG);
        this.registerType(SILVERFISH, ItemTypes.SILVERFISH_SPAWN_EGG);
        this.registerType(BLAZE, ItemTypes.BLAZE_SPAWN_EGG);
        this.registerType(MAGMA_CUBE, ItemTypes.MAGMA_CUBE_SPAWN_EGG);
        this.registerType(WITCH, ItemTypes.WITCH_SPAWN_EGG);
        this.registerType(BAT, ItemTypes.BAT_SPAWN_EGG);
        this.registerType(PIG, ItemTypes.PIG_SPAWN_EGG);
        this.registerType(SHEEP, ItemTypes.SHEEP_SPAWN_EGG);
        this.registerType(COW, ItemTypes.COW_SPAWN_EGG);
        this.registerType(CHICKEN, ItemTypes.CHICKEN_SPAWN_EGG);
        this.registerType(SQUID, ItemTypes.SQUID_SPAWN_EGG);
        this.registerType(WOLF, ItemTypes.WOLF_SPAWN_EGG);
        this.registerType(MOOSHROOM, ItemTypes.MOOSHROOM_SPAWN_EGG);
        this.registerType(OCELOT, ItemTypes.OCELOT_SPAWN_EGG);
        this.registerType(HORSE, ItemTypes.HORSE_SPAWN_EGG);
        this.registerType(VILLAGER, ItemTypes.VILLAGER_SPAWN_EGG);
        
        this.spawnerItem = ItemStack.of(ItemTypes.SPAWNER, 1);
        spawnerItem.offer(Keys.APPLIED_ENCHANTMENTS, singletonList(Enchantment.builder().type(LURE).level(1).build()));
    }
    
    public <E extends Entity> void registerType(Supplier<EntityType<E>> entityType, Supplier<ItemType> spawnEgg) 
    {
        this.eggs.put(entityType.get(), spawnEgg.get());
        this.entities.put(spawnEgg.get(), entityType.get());
        this.initPerm(entityType.get());
    }

    private void initPerm(EntityType<?> type)
    {
        final ResourceKey resourceKey = Sponge.game().registries().registry(RegistryTypes.ENTITY_TYPE).valueKey(type);
        this.perms.put(type, pm.register(Spawner.class, resourceKey.value(), "Allows creating " + PlainComponentSerializer.plain().serialize(type.asComponent()) + " spawners", eggPerms));
    }

    private static boolean hasEnchantment(ItemStackSnapshot item, EnchantmentType ench)
    {
        Optional<List<Enchantment>> enchs = item.get(Keys.APPLIED_ENCHANTMENTS);
        if (enchs.isPresent())
        {
            for (Enchantment e : enchs.get())
            {
                if (e.type().equals(ench))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean breaks(ChangeBlockEvent.All event, BlockType type)
    {
        return has(event, type);
    }

    private static boolean places(ChangeBlockEvent.All event, BlockType type)
    {
        return has(event, type);
    }

    private static boolean has(ChangeBlockEvent.All event, BlockType type)
    {
        for (BlockTransaction trans : event.transactions())
        {
            if (trans.operation().equals(Operations.BREAK.get()))
            {
                if (trans.original().state().type().equals(type))
                {
                    return true;
                }
            }
            else if (trans.operation().equals(Operations.PLACE.get()))
            {
                if (trans.finalReplacement().state().type().equals(type))
                {
                    return true;
                }
            }
        }
        return false;
    }

    @Listener(order = POST)
    @IsCancelled(Tristate.UNDEFINED)
    public void onBlockBreak(ChangeBlockEvent.All event, @First ServerPlayer player)
    {
        if (event.transactions().size() != 1 && event.transactions(Operations.BREAK.get()).count() != 1)
        {
            return;
        }

        Optional<ItemStackSnapshot> inHand = event.context().get(EventContextKeys.USED_ITEM);
        if (inHand.isPresent() && hasEnchantment(inHand.get(), SILK_TOUCH.get()) && breaks(event, BlockTypes.SPAWNER.get()) && this.breakPerm.check(player))
        {
            Transaction<BlockSnapshot> trans = event.transactions().get(0);
            ServerLocation loc = trans.finalReplacement().location().get();
            trans.original().restore(true, BlockChangeFlags.NONE);
            EntityType<?> type = loc.blockEntity().flatMap(spawner -> spawner.get(Keys.NEXT_ENTITY_TO_SPAWN)).map(a -> a.get().type()).orElse(null);
            trans.defaultReplacement().restore(true, BlockChangeFlags.NONE);

            if (type == null || event.isCancelled())
            {
                return;
            }

            ItemStack spawnerItem = this.spawnerItem.copy();
            spawnerItem.offer(Keys.APPLIED_ENCHANTMENTS, singletonList(Enchantment.builder().type(LURE).level(1).build()));
            spawnerItem.offer(Keys.HIDE_ENCHANTMENTS, true);
            spawnerItem.offer(Keys.CUSTOM_NAME, i18n.translate(player, "Inactive monster spawner"));

            brokenSpawners.computeIfAbsent(loc.worldKey(), k -> new HashSet<>()).add(loc.blockPosition());

            Entity item = player.world().createEntity(ITEM.get(), player.location().position());
            item.offer(Keys.ITEM_STACK_SNAPSHOT, spawnerItem.createSnapshot());
            Sponge.server().causeStackManager().pushCause(player);
            player.world().spawnEntity(item);

            if (this.dropEggPerm.check(player))
            {
                ItemStack eggItem = ItemStack.of(this.eggs.get(type));
                Entity eggEntity = player.world().createEntity(ITEM.get(), player.location().position());
                eggEntity.offer(Keys.ITEM_STACK_SNAPSHOT, eggItem.createSnapshot());
                player.world().spawnEntity(eggEntity);
            }
            i18n.send(ACTION_BAR, player, POSITIVE, "Dropped inactive monster spawner!");
        }
    }

    @Listener
    public void onSpawnerExp(SpawnEntityEvent event, @First BlockSnapshot snap)
    {
        if (event instanceof SpawnEntityEvent.Pre)
        {
            return;
        }
        if (snap.location().isPresent())
        {
            if (brokenSpawners.getOrDefault(snap.world(), Collections.emptySet()).remove(snap.position()))
            {
                event.setCancelled(true);
            }
        }
    }

    @Listener(order = POST)
    public void onBlockPlace(ChangeBlockEvent.All event, @First Player player)
    {
        Optional<ItemStackSnapshot> inHand = event.context().get(EventContextKeys.USED_ITEM);
        if (inHand.isPresent() &&
            event.transactions().size() == 1 && event.transactions(Operations.PLACE.get()).count() == 1 &&
            places(event, BlockTypes.SPAWNER.get()) &&
            hasEnchantment(inHand.get(), LURE.get()))
        {
            EntityArchetype hidden = EntityArchetype.builder().type(SNOWBALL).add(Keys.IS_INVISIBLE, true)
                                                    .build();
            for (Transaction<BlockSnapshot> trans : event.transactions())
            {
                if (trans.finalReplacement().state().type().isAnyOf(BlockTypes.SPAWNER))
                {
                    BlockSnapshot snap = trans.finalReplacement();
                    ServerLocation loc = snap.location().get();
                    final BlockEntity spawner = loc.blockEntity().get();
                    spawner.offer(Keys.SPAWNABLE_ENTITIES, new WeightedTable<>());
                    spawner.offer(Keys.NEXT_ENTITY_TO_SPAWN, new WeightedSerializableObject<>(hidden, 1));
                    i18n.send(ACTION_BAR, player, POSITIVE, "Inactive monster spawner placed!");
                    return;
                }
            }
        }
    }

    @Listener(order = POST)
    public void onInteract(InteractBlockEvent.Secondary event, @First ServerPlayer player)
    {
        final ServerLocation block = event.block().location().get();
        if (block.blockType().isAnyOf(BlockTypes.SPAWNER)
         && player.itemInHand(MAIN_HAND).type().isAnyOf(ItemTypes.BAT_SPAWN_EGG))
        {
            event.setCancelled(true);

            if (block.get(Keys.SPAWNABLE_ENTITIES).map(RandomObjectTable::isEmpty).orElse(false))
            {
                ItemStack itemInHand = player.itemInHand(MAIN_HAND);
                final EntityType<?> type = this.entities.get(itemInHand.type());

                Permission perm = this.perms.get(type);
                if (perm == null)
                {
                    this.initPerm(type);
                    perm = this.perms.get(type);
                }

                if (perm == null && !eggPerms.check(player))
                {
                    i18n.send(ACTION_BAR, player, NEGATIVE, "Invalid SpawnEgg!");
                    return;
                }
                if (perm != null && !perm.check(player))
                {
                    i18n.send(ACTION_BAR, player, NEGATIVE, "You are not allowed to change monster spawner to this EntityType!");
                    return;
                }

                WeightedTable<EntityArchetype> spawns = new WeightedTable<>();
                EntityArchetype nextSpawn = EntityArchetype.builder().type(type).build();
                spawns.add(nextSpawn, 1);
                block.offer(Keys.SPAWNABLE_ENTITIES, spawns);
                block.offer(Keys.NEXT_ENTITY_TO_SPAWN, new WeightedSerializableObject<>(nextSpawn, 1));

                if (!player.gameMode().get().equals(CREATIVE))
                {
                    itemInHand.setQuantity(itemInHand.quantity() - 1);
                    player.setItemInHand(MAIN_HAND, itemInHand);
                }

                i18n.send(ACTION_BAR, player, POSITIVE, "Monster spawner activated!");
                return;
            }
            i18n.send(ACTION_BAR, player, NEGATIVE, "You can only change inactive monster spawner!");
        }
    }
}
