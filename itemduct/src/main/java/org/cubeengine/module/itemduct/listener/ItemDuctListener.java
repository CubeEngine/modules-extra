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

import com.flowpowered.math.vector.Vector3d;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.itemduct.Itemduct;
import org.cubeengine.module.itemduct.data.DuctData;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
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
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Collections;
import java.util.Optional;

import javax.inject.Inject;

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
    public void onInteractPiston(InteractBlockEvent.Secondary.MainHand event, @Root Player player)
    {
        Optional<ItemStack> itemInHand = player.getItemInHand(HandTypes.MAIN_HAND);
        Boolean hasActivator = itemInHand.map(ItemDuctListener::isActivator).orElse(false);
        if (hasActivator)
        {
            event.setCancelled(true);
        }

        if (!isDuctInteraction(event))
        {
            return;
        }

        Location<World> loc = event.getTargetBlock().getLocation().get();
        Direction dir = loc.get(Keys.DIRECTION).orElse(Direction.NONE);
        Location<World> te = loc.getRelative(dir);
        Optional<DuctData> ductData = te.get(DuctData.class);
        Direction dirO = dir.getOpposite();
        if (!ductData.map(d -> d.has(dirO)).orElse(false) && hasActivator)
        {
            if (loc.getBlockType() == BlockTypes.OBSERVER)
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

            te.offer(ductData.orElse(new DuctData()).with(dirO));
            playCreateEffect(loc);

            if (player.get(Keys.GAME_MODE).get() != GameModes.CREATIVE)
            {
                ItemStack newStack = itemInHand.get().copy();
                newStack.setQuantity(itemInHand.get().getQuantity() - 1);
                player.setItemInHand(HandTypes.MAIN_HAND, newStack);
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
            Location<World> loc = trans.getOriginal().getLocation().get();
            if (isEndPointType(type))
            {
                Direction dir = trans.getOriginal().get(Keys.DIRECTION).orElse(Direction.NONE);
                Optional<DuctData> data = loc.getRelative(dir).get(DuctData.class);
                if (data.isPresent())
                {
                    data.get().remove(dir.getOpposite());
                    if (data.get().getFilters().isEmpty())
                    {
                        loc.getRelative(dir).remove(DuctData.class);
                    }
                    else
                    {
                        loc.getRelative(dir).offer(data.get());
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
            return item.get(Keys.ITEM_ENCHANTMENTS).orElse(Collections.emptyList()).contains(ench);
        }
        return false;
    }

    private static boolean isDuctInteraction(InteractBlockEvent event)
    {
        if (!(event instanceof InteractBlockEvent.Primary.MainHand) && !(event instanceof InteractBlockEvent.Secondary.MainHand))
        {
            return false;
        }
        if (!event.getTargetBlock().getLocation().isPresent())
        {
            return false;
        }
        Location<World> loc = event.getTargetBlock().getLocation().get();
        BlockType type = loc.getBlockType();
        if (!isEndPointType(type))
        {
            return false;
        }
        Direction dir = loc.get(Keys.DIRECTION).orElse(Direction.NONE);
        Optional<TileEntity> te = loc.getRelative(dir).getTileEntity();
        return te.isPresent() && te.get() instanceof Carrier;
    }

    private static boolean isEndPointType(BlockType type)
    {
        return BlockTypes.PISTON.equals(type) || STICKY_PISTON.equals(type) || BlockTypes.OBSERVER.equals(type);
    }

    private static void playCreateEffect(Location<World> loc)
    {
        ParticleEffect effect = ParticleEffect.builder().type(ParticleTypes.CLOUD).build();

        Vector3d center = loc.getPosition().add(0.5, 0.5, 0.5);
        for (Direction effectDir : Direction.values())
        {
            if (effectDir.isCardinal() || effectDir.isUpright())
            {
                loc.getExtent().spawnParticles(effect, center.add(effectDir.asOffset().div(1.9)));
            }
        }
        loc.getExtent().playSound(SoundTypes.BLOCK_ANVIL_USE, loc.getPosition(), 1);
    }

}
