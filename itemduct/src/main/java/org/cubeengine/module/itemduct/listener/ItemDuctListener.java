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
package org.cubeengine.module.itemduct.listener;

import static org.spongepowered.api.block.BlockTypes.STICKY_PISTON;

import com.google.inject.Inject;
import net.kyori.adventure.sound.Sound;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.itemduct.Itemduct;
import org.cubeengine.module.itemduct.data.DuctData;
import org.cubeengine.module.itemduct.data.DuctRecipes;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles ItemDuct setup and cleanup and effects
 */
public class ItemDuctListener
{
    @Inject private PermissionManager pm;

    private Permission activateObserverPerm;
    private Permission activatePistonPerm;
    private Itemduct module;

    public void setup(Itemduct module)
    {
        this.module = module;
        activatePistonPerm = this.pm.register(Itemduct.class, "activate.piston", "Allows activating ItemDuct Piston Endpoints", null);
        activateObserverPerm = this.pm.register(Itemduct.class, "activate.observer", "Allows activating ItemDuct Observer Endpoints", null);
    }

    @Listener
    public void onInteractPiston(InteractBlockEvent.Secondary event, @Root ServerPlayer player)
    {
        final boolean isMainHand = event.getContext().get(EventContextKeys.USED_HAND).get() == HandTypes.MAIN_HAND;
        if (!isMainHand) {
            return;
        }
        ItemStack itemInHand = player.getItemInHand(HandTypes.MAIN_HAND);
        boolean hasActivator = ItemDuctListener.isActivator(itemInHand);
        if (hasActivator)
        {
            event.setCancelled(true);
        }

        if (!isDuctInteraction(event, player))
        {
            return;
        }


        ServerLocation loc = player.getWorld().getLocation(event.getInteractionPoint().get());
        Direction dir = loc.get(Keys.DIRECTION).orElse(Direction.NONE);
        ServerLocation te = loc.add(dir.asBlockOffset());
        final Optional<Map<Direction, List<ItemStack>>> ductData = te.get(DuctData.FILTERS);
        Direction dirO = dir.getOpposite();
        if (!ductData.map(d -> d.containsKey(dirO)).orElse(false) && hasActivator)
        {
            if (loc.getBlockType() == BlockTypes.OBSERVER.get())
            {
                if (!player.hasPermission(this.activateObserverPerm.getId()))
                {
                    event.setCancelled(true);
                    return;
                }
            }
            else
            {
                if (!player.hasPermission(this.activatePistonPerm.getId()))
                {
                    event.setCancelled(true);
                    return;
                }
            }

            final Map<Direction, List<ItemStack>> dd = ductData.get();
            dd.put(dirO, new ArrayList<>());
            te.offer(DuctData.FILTERS, dd);
            playCreateEffect(loc);

            if (player.get(Keys.GAME_MODE).get() != GameModes.CREATIVE)
            {
                ItemStack newStack = itemInHand.copy();
                ItemStack sepStack = itemInHand.copy();

                Integer uses = newStack.get(DuctData.USES).orElse(0);
                uses--;

                if (uses <= 0) // Item used up?
                {
                    if (uses == -2) // or infinite usage?
                    {
                        uses++;
                    }
                    else
                    {
                        newStack.setQuantity(itemInHand.getQuantity() - 1);
                    }
                    sepStack.setQuantity(0);
                }
                else
                {
                    sepStack.setQuantity(newStack.getQuantity() - 1);
                    newStack.setQuantity(1);
                }
                newStack.offer(DuctData.USES, uses);
                module.getManager().updateUses(newStack);

                player.setItemInHand(HandTypes.MAIN_HAND, newStack);
                player.getInventory().offer(sepStack);
            }

            player.getProgress(module.activate).grant();
        }
    }

    @Listener
    public void onBreak(ChangeBlockEvent.Break event)
    {
        for (Transaction<BlockSnapshot> trans : event.getTransactions())
        {
            if (!trans.getOriginal().getLocation().isPresent())
            {
                continue;
            }
            BlockType type = trans.getOriginal().getState().getType();
            ServerLocation loc = trans.getOriginal().getLocation().get();
            if (isEndPointType(type))
            {
                Direction dir = trans.getOriginal().get(Keys.DIRECTION).orElse(Direction.NONE);
                final ServerLocation relative = loc.add(dir.asBlockOffset());
                Optional<Map<Direction, List<ItemStack>>> data = relative.get(DuctData.FILTERS);
                if (data.isPresent())
                {
                    data.get().remove(dir.getOpposite());
                    if (data.get().isEmpty())
                    {
                        relative.remove(DuctData.FILTERS);
                        event.getCause().first(Player.class).ifPresent(p -> {
                            if (p.get(Keys.GAME_MODE).map(mode -> mode != GameModes.CREATIVE).orElse(false)) {
                                Entity item = loc.createEntity(EntityTypes.ITEM.get());
                                item.offer(Keys.ITEM_STACK_SNAPSHOT, DuctRecipes.singleActivatorItem.createSnapshot());
                                loc.spawnEntity(item);
                            }
                        });

                    }
                    else
                    {
                        relative.offer(DuctData.FILTERS, data.get());
                    }
                }
            }
        }
    }

    private static boolean isActivator(ItemStack item)
    {
        if (ItemTypes.HOPPER.equals(item.getType()))
        {
            Enchantment ench = Enchantment.builder().type(EnchantmentTypes.LOOTING).level(1).build();
            return item.get(Keys.APPLIED_ENCHANTMENTS).orElse(Collections.emptyList()).contains(ench);
        }
        return false;
    }

    private static boolean isDuctInteraction(InteractBlockEvent event, ServerPlayer player)
    {
        // TODO and mainhand?
//        if (!(event instanceof InteractBlockEvent.Primary) && !(event instanceof InteractBlockEvent.Secondary))
//        {
//            return false;
//        }
        if (!event.getInteractionPoint().isPresent()) {
            return false;
        }
        final ServerLocation loc = player.getWorld().getLocation(event.getInteractionPoint().get());
        BlockType type = loc.getBlockType();
        if (!isEndPointType(type))
        {
            return false;
        }
        Direction dir = loc.get(Keys.DIRECTION).orElse(Direction.NONE);
        final Optional<? extends BlockEntity> te = loc.add(dir.asBlockOffset()).getBlockEntity();
        return te.isPresent() && te.get() instanceof Carrier;
    }

    private static boolean isEndPointType(BlockType type)
    {
        return BlockTypes.PISTON.equals(type) || STICKY_PISTON.equals(type) || BlockTypes.OBSERVER.equals(type);
    }

    private static void playCreateEffect(ServerLocation loc)
    {
        ParticleEffect effect = ParticleEffect.builder().type(ParticleTypes.CLOUD).build();

        Vector3d center = loc.getPosition().add(0.5, 0.5, 0.5);
        for (Direction effectDir : Direction.values())
        {
            if (effectDir.isCardinal() || effectDir.isUpright())
            {
                loc.getWorld().spawnParticles(effect, center.add(effectDir.asOffset().div(1.9)));
            }
        }
        loc.getWorld().playSound(Sound.of(SoundTypes.BLOCK_ANVIL_USE, Sound.Source.NEUTRAL, 1, 0), loc.getPosition());
    }

}
