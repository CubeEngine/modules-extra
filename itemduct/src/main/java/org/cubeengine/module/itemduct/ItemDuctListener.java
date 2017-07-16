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
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.itemduct.data.DuctData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.Chest;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleOptions;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.format.TextFormat;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

public class ItemDuctListener
{
    private Map<Location<World>, Long> promtedTransfers = new HashMap<>();

    @Inject private PluginContainer plugin;
    @Inject private I18n i18n;
    private Task task;

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
                if (player.get(Keys.IS_SNEAKING).orElse(false))
                {
                    openFilter(player, ductData.get(), dir.getOpposite(), te);
                }
                else
                {
                    playEffect(loc); // Play Effect for DuctPiston
                }
            }
        }
        else if (player.getItemInHand(HandTypes.MAIN_HAND).map(i -> i.getItem().equals(ItemTypes.HOPPER)).orElse(false))
        {
            te.offer(ductData.orElse(new DuctData()).with(dir.getOpposite()));
            playCreateEffect(loc);
            event.setCancelled(true);
        }
    }

    private void openFilter(Player player, DuctData ductData, Direction dir, Location<World> loc)
    {
        List<ItemStack> list = ductData.get(dir).get();
        DuctFilterCarrier carrier = new DuctFilterCarrier(ductData, loc, dir);
        Inventory inventory = Inventory.builder()
                .property(InventoryTitle.PROPERTY_NAME, InventoryTitle.of(i18n.translate(player, TextFormat.NONE, "ItemDuct Filters")))
                .withCarrier(carrier)
                .build(plugin);
        carrier.init(((CarriedInventory) inventory));

        for (ItemStack itemStack : list)
        {
            inventory.offer(itemStack);
        }

        player.openInventory(inventory, Cause.source(plugin).build()); // TODO player cause
    }

    @Listener
    public void onCloseInventory(InteractInventoryEvent.Close event, @Root Player player)
    {
        if (event.getTargetInventory() instanceof CarriedInventory)
        {
            Optional<Carrier> carrier = ((CarriedInventory) event.getTargetInventory()).getCarrier();
            if (carrier.orElse(null) instanceof DuctFilterCarrier)
            {
                ((DuctFilterCarrier) carrier.get()).update(event.getTargetInventory().iterator().next());
            }
            promptTransfer(carrier);
        }
    }

    private void promptTransfer(Optional<Carrier> carrier)
    {
        if (carrier.orElse(null) instanceof TileEntity)
        {
            Location<World> loc = ((TileEntity) carrier.get()).getLocation();
            Optional<DuctData> data = loc.get(DuctData.class);
            if (data.isPresent())
            {
                if (this.promtedTransfers.isEmpty())
                {
                    task = Sponge.getScheduler().createTaskBuilder().delayTicks(10).intervalTicks(20).execute(this::transfer).submit(plugin);
                }
                Long time = this.promtedTransfers.computeIfAbsent(loc, k -> System.currentTimeMillis());
                if (System.currentTimeMillis() - time > 2000)
                {
                    this.promtedTransfers.put(loc, System.currentTimeMillis());
                }
            }
        }
    }

    private void transfer()
    {
        for (Location<World> loc : this.promtedTransfers.keySet())
        {
            // Check if data is still present
            Optional<DuctData> data = loc.get(DuctData.class);
            if (data.isPresent())
            {
                for (Direction dir : Direction.values())
                {
                    if (dir.isCardinal() || dir.isUpright())
                    {
                        if (BlockTypes.STICKY_PISTON.equals(loc.getRelative(dir).getBlockType()))
                        {
                            Optional<List<ItemStack>> filters = data.get().get(dir);
                            if (filters.isPresent())
                            {
                                DuctUtil.Network network = DuctUtil.findNetwork(loc.getRelative(dir));
                                TileEntity te = loc.getTileEntity().get();
                                Inventory inventory = ((Carrier) te).getInventory();
                                if (te instanceof Chest)
                                {
                                    inventory = ((Chest) te).getDoubleChestInventory().orElse(inventory);
                                }
                                network.transfer(inventory, filters.get());
                            }
                        }
                    }
                }
            }
        }
        promtedTransfers.clear();
        task.cancel();
    }

    private class DuctFilterCarrier implements Carrier
    {

        private CarriedInventory<? extends Carrier> inventory;

        private final DuctData ductData;
        private final Location<World> loc;
        private final Direction dir;

        public DuctFilterCarrier(DuctData ductData, Location<World> loc, Direction dir)
        {
            this.ductData = ductData;
            this.loc = loc;
            this.dir = dir;
        }

        public void init(CarriedInventory<? extends Carrier> inventory)
        {
            this.inventory = inventory;
        }

        @Override
        public CarriedInventory<? extends Carrier> getInventory()
        {
            return this.inventory;
        }

        public void update(Inventory inventory)
        {
            List<ItemStack> list = ductData.get(dir).get();
            list.clear();
            for (Inventory item : inventory.slots())
            {
                if (item.peek().isPresent())
                {
                    list.add(item.peek().get());
                }
            }
            loc.offer(ductData);
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

    @Listener
    public void onTransferInventory(ChangeInventoryEvent.Transfer event) // TODO pre event as we dont actually care about the inventory changes
    {
        if (event.getTargetInventory() instanceof CarriedInventory)
        {
            promptTransfer(((CarriedInventory) event.getTargetInventory()).getCarrier());
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
        ParticleEffect badEffect = ParticleEffect.builder().type(ParticleTypes.REDSTONE_DUST).build();
        ParticleEffect goodEffect = ParticleEffect.builder().type(ParticleTypes.REDSTONE_DUST).option(ParticleOptions.COLOR, Color.GREEN).build();
        ParticleEffect neutralEffect = ParticleEffect.builder().type(ParticleTypes.REDSTONE_DUST).option(ParticleOptions.COLOR, Color.YELLOW).build();
        ParticleEffect smoke = ParticleEffect.builder().type(ParticleTypes.LARGE_SMOKE).build();
        Vector3d center = loc.getPosition().add(0.5, 0.5, 0.5);
        for (Direction effectDir : Direction.values())
        {
            if (effectDir.isCardinal() || effectDir.isUpright())
            {
                loc.getExtent().spawnParticles(goodEffect, center.add(effectDir.asOffset().div(1.9)));
            }
        }
        loc.getExtent().playSound(SoundTypes.BLOCK_DISPENSER_DISPENSE, loc.getPosition(), 1);

        DuctUtil.Network network = DuctUtil.findNetwork(loc);
        System.out.print("Network: Pipes " +  network.pipes.size() + " Exits " + network.exitPoints.size() + "\n");

        for (Location<World> pipe : network.pipes)
        {
            pipe.getExtent().spawnParticles(network.errors.isEmpty() ? goodEffect : neutralEffect, pipe.getPosition().add(0.5,0.5,0.5));
            if (network.exitPoints.isEmpty())
            {
                pipe.getExtent().spawnParticles(smoke, pipe.getPosition().add(0.5,0.5,0.5));
            }
        }

        for (Location<World> error : network.errors)
        {
            error.getExtent().spawnParticles(badEffect, error.getPosition().add(0.5,0.5,0.5));
            error.getExtent().spawnParticles(smoke, error.getPosition().add(0.5,0.5,0.5));
        }

        for (Location<World> exit : network.exitPoints.keySet())
        {
            center = exit.getPosition().add(0.5,0.5,0.5);
            for (Direction effectDir : Direction.values())
            {
                if (effectDir.isCardinal() || effectDir.isUpright())
                {
                    exit.getExtent().spawnParticles(goodEffect, center.add(effectDir.asOffset().div(1.9)));
                }
            }
            exit.getExtent().playSound(SoundTypes.BLOCK_DISPENSER_DISPENSE, exit.getPosition(), 1);
        }


    }
}
