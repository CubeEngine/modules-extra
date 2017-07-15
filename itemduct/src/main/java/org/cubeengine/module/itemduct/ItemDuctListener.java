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
package org.cubeengine.module.itemduct;

import com.flowpowered.math.vector.Vector3d;
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
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

public class ItemDuctListener
{
    @Listener
    public void onInteractPiston(InteractBlockEvent.Secondary.MainHand event, @Root Player player)
    {
        if (!isDuctInteraction(event))
        {
            return;
        }

        Location<World> loc = event.getTargetBlock().getLocation().get();
        Direction dir = loc.get(Keys.DIRECTION).orElse(Direction.NONE);
        Location<World> te = loc.getRelative(dir);
        Optional<DuctData> ductData = te.get(DuctData.class);

        if (ductData.map(d -> d.get(dir.getOpposite()).isPresent()).orElse(false))
        {
            if (!player.getItemInHand(HandTypes.MAIN_HAND).isPresent())
            {
                playEffect(loc); // Play Effect for DuctPiston
            }
        }
        else if (player.getItemInHand(HandTypes.MAIN_HAND).map(i -> i.getItem().equals(ItemTypes.HOPPER)).orElse(false))
        {
            te.offer(ductData.orElse(new DuctData()).with(dir.getOpposite()));
            playCreateEffect(loc);
            event.setCancelled(true);
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
            if (type.equals(BlockTypes.PISTON) || type.equals(BlockTypes.STICKY_PISTON))
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

    @Listener
    public void onInteractPiston(InteractBlockEvent.Primary.MainHand event, @Root Player player)
    {
        if (!isDuctInteraction(event))
        {
            return;
        }
        Location<World> loc = event.getTargetBlock().getLocation().get();
        if (player.getItemInHand(HandTypes.MAIN_HAND).map(i -> i.getItem().equals(ItemTypes.HOPPER)).orElse(false))
        {
            playCreateEffect(loc);
            event.setCancelled(true);
        }
    }

    private boolean isDuctInteraction(InteractBlockEvent event)
    {
        if (!event.getTargetBlock().getLocation().isPresent())
        {
            return false;
        }
        Location<World> loc = event.getTargetBlock().getLocation().get();
        BlockType type = loc.getBlockType();
        if (!type.equals(BlockTypes.PISTON) && !type.equals(BlockTypes.STICKY_PISTON))
        {
            return false;
        }
        Direction dir = loc.get(Keys.DIRECTION).orElse(Direction.NONE);
        Optional<TileEntity> te = loc.getRelative(dir).getTileEntity();
        if (!te.isPresent() || !(te.get() instanceof Carrier))
        {
            return false;
        }
        return true;
    }

    private void playCreateEffect(Location<World> loc)
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

    private void playEffect(Location<World> loc)
    {
        ParticleEffect effect = ParticleEffect.builder().type(ParticleTypes.REDSTONE_DUST).build();
        Vector3d center = loc.getPosition().add(0.5, 0.5, 0.5);
        for (Direction effectDir : Direction.values())
        {
            if (effectDir.isCardinal() || effectDir.isUpright())
            {
                loc.getExtent().spawnParticles(effect, center.add(effectDir.asOffset().div(1.9)));
            }
        }
        loc.getExtent().playSound(SoundTypes.BLOCK_DISPENSER_DISPENSE, loc.getPosition(), 1);
    }
}
