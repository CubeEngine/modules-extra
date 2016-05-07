/**
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.meta.ItemEnchantment;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.entity.ConstructEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.Enchantment;
import org.spongepowered.api.item.Enchantments;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.util.weighted.RandomObjectTable;
import org.spongepowered.api.util.weighted.WeightedTable;

import static java.util.Collections.singletonList;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.block.BlockTypes.MOB_SPAWNER;
import static org.spongepowered.api.entity.EntityTypes.*;
import static org.spongepowered.api.entity.living.player.gamemode.GameModes.CREATIVE;
import static org.spongepowered.api.entity.living.player.gamemode.GameModes.SPECTATOR;
import static org.spongepowered.api.event.Order.POST;
import static org.spongepowered.api.item.Enchantments.LURE;
import static org.spongepowered.api.item.Enchantments.SILK_TOUCH;

/**
 * A module to gather monster spawners with silk touch and reactivate them using spawneggs
 */
@ModuleInfo(name = "Spawner", description = "Lets you move spawners")
public class Spawner extends Module
{
    private ItemStack spawnerItem;
    private PermissionDescription eggPerms;
    private Map<EntityType, PermissionDescription> perms = new HashMap<>();

    @Inject private PermissionManager pm;
    @Inject private EventManager em;
    @Inject private I18n i18n;

    @Enable
    public void onEnable()
    {
        this.eggPerms = pm.register(Spawner.class, "egg", "", null);
        this.initPerm(CREEPER, SKELETON, SPIDER, ZOMBIE, SLIME, GHAST, PIG_ZOMBIE, ENDERMAN,
                      CAVE_SPIDER, SILVERFISH, BLAZE, MAGMA_CUBE, WITCH, BAT, PIG, SHEEP, COW,
                      CHICKEN, SQUID, WOLF, MUSHROOM_COW, OCELOT, HORSE, VILLAGER);

        this.spawnerItem = ItemStack.of(ItemTypes.MOB_SPAWNER, 1);
        spawnerItem.offer(Keys.ITEM_ENCHANTMENTS, singletonList(new ItemEnchantment(Enchantments.LURE, 1)));
        em.registerListener(Spawner.class, this);
    }

    private void initPerm(EntityType... types)
    {
        for (EntityType type : types)
        {
            PermissionDescription child = pm.register(Spawner.class, type.getName(), "", eggPerms);
            this.perms.put(type, child);
        }
    }

    public static boolean hasEnchantment(ItemStack item, Enchantment ench)
    {
        // TODO Sponge 4.0 will have a check for this?
        Optional<List<ItemEnchantment>> enchs = item.get(Keys.ITEM_ENCHANTMENTS);
        if (enchs.isPresent())
        {
            for (ItemEnchantment e : enchs.get())
            {
                if (e.getEnchantment().equals(ench))
                {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean breaks(ChangeBlockEvent.Break event, BlockType type)
    {
        return has(event, type, snap -> snap.getOriginal().getState().getType());
    }

    public static boolean places(ChangeBlockEvent.Place event, BlockType type)
    {
        return has(event, type, snap -> snap.getFinal().getState().getType());
    }

    public static boolean has(ChangeBlockEvent event, BlockType type, Function<Transaction<BlockSnapshot>, BlockType> func)
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
    public void onBlockBreak(ChangeBlockEvent.Break event, @First Player player)
    {
        Optional<ItemStack> inHand = player.getItemInHand();
        if (inHand.isPresent() &&
            hasEnchantment(inHand.get(), SILK_TOUCH) &&
            breaks(event, MOB_SPAWNER))
        {
            ItemStack clone = spawnerItem.copy();
            clone.offer(Keys.ITEM_ENCHANTMENTS, singletonList(new ItemEnchantment(Enchantments.LURE, 1)));
            clone.offer(Keys.DISPLAY_NAME, i18n.getTranslation(player, NONE, "Inactive Monster Spawner"));

            Optional<Entity> item = player.getWorld().createEntity(ITEM, player.getLocation().getPosition());
            if (item.isPresent())
            {
                item.get().offer(Keys.REPRESENTED_ITEM, clone.createSnapshot());
                player.getWorld().spawnEntity(item.get(), Cause.of(NamedCause.source(player))); // TODO instead drop naturally at blockpos
                i18n.sendTranslated(player, POSITIVE, "Dropped inactive Monster Spawner!");
                // TODO cancel exp drops
                return;
            }
            throw new IllegalStateException("Could not spawn item");
        }
    }

    // TODO AbstractMethodError below @Listener
    public void onSpawnerExp(ConstructEntityEvent event)
    {
        if (event.getTargetType().equals(EntityTypes.EXPERIENCE_ORB))
        {
            System.out.println(event.getCause()); // TODO can i detect drops from blockbreak?
        }
    }

    @Listener(order = POST)
    public void onBlockPlace(ChangeBlockEvent.Place event, @First Player player)
    {
        if (places(event, MOB_SPAWNER) &&
            hasEnchantment(player.getItemInHand().get(), LURE))
        {
            for (Transaction<BlockSnapshot> trans : event.getTransactions())
            {
                if (trans.getFinal().getState().getType().equals(MOB_SPAWNER))
                {
                    BlockSnapshot snap = trans.getFinal();
                    snap.with(Keys.SPAWNER_ENTITIES, new WeightedTable<>());
                    trans.setCustom(snap);
                    i18n.sendTranslated(player, POSITIVE, "Inactive Monster Spawner placed!");
                    return;
                }
            }
        }
    }

    @Listener(order = POST)
    public void onInteract(InteractBlockEvent.Secondary event, @First Player player)
    {
        // TODO maybe allow multiple spawner eggs /w same weight
        BlockState state = event.getTargetBlock().getState();
        if (state.getType().equals(MOB_SPAWNER)
         && player.getItemInHand().map(i -> i.getItem().equals(ItemTypes.SPAWN_EGG)).orElse(false))
        {
            event.setCancelled(true);

            if (state.get(Keys.SPAWNER_ENTITIES).map(RandomObjectTable::isEmpty).orElse(false))
            {
                ItemStack itemInHand = player.getItemInHand().get();
                EntityType type = itemInHand.get(Keys.SPAWNABLE_ENTITY_TYPE).get();

                PermissionDescription perm = this.perms.get(type);
                if (perm == null && !player.hasPermission(eggPerms.getId()))
                {
                    i18n.sendTranslated(player, NEGATIVE, "Invalid SpawnEgg!");
                    return;
                }
                if (perm != null && !player.hasPermission(perm.getId()))
                {
                    i18n.sendTranslated(player, NEGATIVE, "You are not allowed to change Monster Spawner to this EntityType!");
                    return;
                }

                WeightedTable<EntitySnapshot> spawns = new WeightedTable<>();
                spawns.add(EntitySnapshot.builder().type(type).build(), 1);
                state.with(Keys.SPAWNER_ENTITIES, spawns);

                if (event.getTargetBlock().withState(state).restore(true, true)) // TODO no cause?
                {
                    if (!player.gameMode().get().equals(CREATIVE))
                    {
                        itemInHand.setQuantity(itemInHand.getQuantity() - 1);
                        player.setItemInHand(itemInHand); // TODO check if this sets no item if quantity 0
                    }

                    i18n.sendTranslated(player, POSITIVE, "Monster Spawner activated!");
                    return;
                }
                throw new IllegalStateException("Could not change SpawnerType");
            }
            i18n.sendTranslated(player, NEGATIVE, "You can only change inactive Monster Spawner!");
        }
    }
}
