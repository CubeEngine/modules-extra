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

import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityArchetype;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.weighted.RandomObjectTable;
import org.spongepowered.api.util.weighted.WeightedSerializableObject;
import org.spongepowered.api.util.weighted.WeightedTable;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static java.util.Collections.singletonList;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.block.BlockTypes.MOB_SPAWNER;
import static org.spongepowered.api.data.key.Keys.*;
import static org.spongepowered.api.data.type.HandTypes.MAIN_HAND;
import static org.spongepowered.api.entity.EntityTypes.*;
import static org.spongepowered.api.entity.living.player.gamemode.GameModes.CREATIVE;
import static org.spongepowered.api.event.Order.POST;
import static org.spongepowered.api.item.enchantment.EnchantmentTypes.LURE;
import static org.spongepowered.api.item.enchantment.EnchantmentTypes.SILK_TOUCH;
import static org.spongepowered.api.text.chat.ChatTypes.ACTION_BAR;

/**
 * A module to gather monster spawners with silk touch and reactivate them using spawneggs
 */
@Singleton
@Module
public class Spawner extends CubeEngineModule
{
    private ItemStack spawnerItem;
    private Permission eggPerms;
    private Permission breakPerm;
    private Permission dropEggPerm;
    private Map<EntityType, Permission> perms = new HashMap<>();
    private Set<Location<World>> brokenSpawners = new HashSet<>();
    private Map<Location<World>, EntityType> spawnerTypes = new HashMap<>();

    @Inject private PermissionManager pm;
    @Inject private I18n i18n;

    @Listener
    public void onEnable(GamePreInitializationEvent event)
    {
        this.eggPerms = pm.register(Spawner.class, "egg", "Allows creating all types of spawners with spawneggs", null);
        this.breakPerm = pm.register(Spawner.class, "break", "Allows obtaining inactive monster spawners", null);
        this.dropEggPerm = pm.register(Spawner.class, "drop-egg", "Allows dropping monster eggs from spawners", null);
        this.initPerm(CREEPER, SKELETON, SPIDER, ZOMBIE, SLIME, GHAST, PIG_ZOMBIE, ENDERMAN,
                      CAVE_SPIDER, SILVERFISH, BLAZE, MAGMA_CUBE, WITCH, BAT, PIG, SHEEP, COW,
                      CHICKEN, SQUID, WOLF, MUSHROOM_COW, OCELOT, HORSE, VILLAGER);

        this.spawnerItem = ItemStack.of(ItemTypes.MOB_SPAWNER, 1);
        spawnerItem.offer(ITEM_ENCHANTMENTS, singletonList(Enchantment.builder().type(LURE).level(1).build()));
    }

    private void initPerm(EntityType... types)
    {
        for (EntityType type : types)
        {
            Permission child = pm.register(Spawner.class, type.getName(),
                    "Allows creating " + type.getName() + " spawners", eggPerms);
            this.perms.put(type, child);
        }
    }

    private static boolean hasEnchantment(ItemStackSnapshot item, EnchantmentType ench)
    {
        Optional<List<Enchantment>> enchs = item.get(ITEM_ENCHANTMENTS);
        if (enchs.isPresent())
        {
            for (Enchantment e : enchs.get())
            {
                if (e.getType().equals(ench))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean breaks(ChangeBlockEvent.Break event, BlockType type)
    {
        return has(event, type, snap -> snap.getOriginal().getState().getType());
    }

    private static boolean places(ChangeBlockEvent.Place event, BlockType type)
    {
        return has(event, type, snap -> snap.getFinal().getState().getType());
    }

    private static boolean has(ChangeBlockEvent event, BlockType type, Function<Transaction<BlockSnapshot>, BlockType> func)
    {
        for (Transaction<BlockSnapshot> trans : event.getTransactions())
        {
            if (func.apply(trans).equals(type))
            {
                return true;
            }
        }
        return false;
    }

    @Listener(order = POST)
    public void onPreBlockBreak(ChangeBlockEvent.Pre event, @First Player player)
    {
        List<Location<World>> locations = event.getLocations();
        if (locations.size() != 1)
        {
            return;
        }

        Optional<ItemStackSnapshot> inHand = player.getItemInHand(MAIN_HAND).map(ItemStack::createSnapshot);
        Location<World> loc = locations.get(0);
        if (inHand.isPresent() && hasEnchantment(inHand.get(), SILK_TOUCH) && loc.getBlockType() == MOB_SPAWNER && this.dropEggPerm.check(player) && this.breakPerm.check(player))
        {
            loc.get(SPAWNER_NEXT_ENTITY_TO_SPAWN).map(wso -> wso.get().getType()).ifPresent(type -> {
                if (perms.containsKey(type))
                {
                    spawnerTypes.put(loc, type);
                }
            });
        }
    }

    @Listener(order = POST)
    @IsCancelled(Tristate.UNDEFINED)
    public void onBlockBreak(ChangeBlockEvent.Break event, @First Player player)
    {
        if (event.getTransactions().size() != 1)
        {
            return;
        }

        Optional<ItemStackSnapshot> inHand = event.getContext().get(EventContextKeys.USED_ITEM);
        if (inHand.isPresent() && hasEnchantment(inHand.get(), SILK_TOUCH) && breaks(event, MOB_SPAWNER) && this.breakPerm.check(player))
        {
            Transaction<BlockSnapshot> trans = event.getTransactions().get(0);
            Location<World> loc = trans.getFinal().getLocation().get();
            EntityType type = spawnerTypes.remove(loc);
            if (event.isCancelled())
            {
                return;
            }

            ItemStack spawnerItem = this.spawnerItem.copy();
            spawnerItem.offer(ITEM_ENCHANTMENTS, singletonList(Enchantment.builder().type(LURE).level(1).build()));
            spawnerItem.offer(DISPLAY_NAME, i18n.translate(player, NONE, "Inactive monster spawner"));

            brokenSpawners.add(loc);

            Entity item = player.getWorld().createEntity(ITEM, player.getLocation().getPosition());
            item.offer(REPRESENTED_ITEM, spawnerItem.createSnapshot());
            Sponge.getCauseStackManager().pushCause(player);
            player.getWorld().spawnEntity(item);

            if (this.dropEggPerm.check(player) && type != null)
            {
                ItemStack eggItem = ItemStack.of(ItemTypes.SPAWN_EGG, 1);
                eggItem.offer(Keys.SPAWNABLE_ENTITY_TYPE, type);
                Entity eggEntity = player.getWorld().createEntity(ITEM, player.getLocation().getPosition());
                eggEntity.offer(REPRESENTED_ITEM, eggItem.createSnapshot());
                player.getWorld().spawnEntity(eggEntity);
            }
            i18n.send(ACTION_BAR, player, POSITIVE, "Dropped inactive monster spawner!");
        }
    }

    @Listener
    public void onSpawnerExp(SpawnEntityEvent event, @Root BlockSnapshot snap)
    {
        if (snap.getLocation().isPresent())
        {
            if (brokenSpawners.remove(snap.getLocation().get()))
            {
                event.setCancelled(true);
            }
        }
    }

    @Listener(order = POST)
    public void onBlockPlace(ChangeBlockEvent.Place event, @First Player player)
    {
        EntityArchetype hidden = EntityArchetype.builder().type(SNOWBALL).set(INVISIBLE, true).build();
        Optional<ItemStackSnapshot> inHand = event.getContext().get(EventContextKeys.USED_ITEM);
        if (inHand.isPresent() &&
            places(event, MOB_SPAWNER) &&
            hasEnchantment(inHand.get(), LURE))
        {
            for (Transaction<BlockSnapshot> trans : event.getTransactions())
            {
                if (trans.getFinal().getState().getType().equals(MOB_SPAWNER))
                {
                    BlockSnapshot snap = trans.getFinal();
                    Location<World> loc = snap.getLocation().get();
                    loc.offer(SPAWNER_ENTITIES, new WeightedTable<>());
                    loc.offer(SPAWNER_NEXT_ENTITY_TO_SPAWN, new WeightedSerializableObject<>(hidden, 1));
                    i18n.send(ACTION_BAR, player, POSITIVE, "Inactive Monster Spawner placed!");
                    return;
                }
            }
        }
    }

    @Listener(order = POST)
    public void onInteract(InteractBlockEvent.Secondary event, @First Player player)
    {
        if (!event.getTargetBlock().getLocation().isPresent()) {
            return;
        }
        Location<World> block = event.getTargetBlock().getLocation().get();
        if (block.getBlockType().equals(MOB_SPAWNER)
         && player.getItemInHand(MAIN_HAND).map(i -> i.getType().equals(ItemTypes.SPAWN_EGG)).orElse(false))
        {
            event.setCancelled(true);

            if (block.get(SPAWNER_ENTITIES).map(RandomObjectTable::isEmpty).orElse(false))
            {
                ItemStack itemInHand = player.getItemInHand(MAIN_HAND).get();
                EntityType type = itemInHand.get(Keys.SPAWNABLE_ENTITY_TYPE).get();

                Permission perm = this.perms.get(type);
                if (perm == null)
                {
                    this.initPerm(type);
                    perm = this.perms.get(type);
                }
                if (perm == null && !player.hasPermission(eggPerms.getId()))
                {
                    i18n.send(ACTION_BAR, player, NEGATIVE, "Invalid SpawnEgg!");
                    return;
                }
                if (perm != null && !player.hasPermission(perm.getId()))
                {
                    i18n.send(ACTION_BAR, player, NEGATIVE, "You are not allowed to change Monster Spawner to this EntityType!");
                    return;
                }

                WeightedTable<EntityArchetype> spawns = new WeightedTable<>();
                EntityArchetype nextSpawn = EntityArchetype.builder().type(type).build();
                spawns.add(nextSpawn, 1);
                block.offer(SPAWNER_ENTITIES, spawns);
                block.offer(SPAWNER_NEXT_ENTITY_TO_SPAWN, new WeightedSerializableObject<>(nextSpawn, 1));

                if (!player.gameMode().get().equals(CREATIVE))
                {
                    itemInHand.setQuantity(itemInHand.getQuantity() - 1);
                    player.setItemInHand(MAIN_HAND, itemInHand);
                }

                i18n.send(ACTION_BAR, player, POSITIVE, "Monster Spawner activated!");
                return;
            }
            i18n.send(ACTION_BAR, player, NEGATIVE, "You can only change inactive Monster Spawner!");
        }
    }
}
