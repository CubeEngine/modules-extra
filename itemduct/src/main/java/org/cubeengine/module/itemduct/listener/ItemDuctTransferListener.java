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

import static org.spongepowered.api.block.BlockTypes.OBSERVER;
import static org.spongepowered.api.block.BlockTypes.STICKY_PISTON;

import org.cubeengine.module.itemduct.ItemDuctManager;
import org.cubeengine.module.itemduct.Itemduct;
import org.cubeengine.module.itemduct.Network;
import org.cubeengine.module.itemduct.data.DuctData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.Chest;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

/**
 * Handles ItemDuct transfer activation
 */
public class ItemDuctTransferListener
{
    @Inject private PluginContainer plugin;
    private Itemduct module;
    private ItemDuctManager manager;

    private Map<Location<World>, Long> promptedActivations = new HashMap<>();
    private Task task;

    public void setup(Itemduct module, ItemDuctManager manager)
    {
        this.module = module;
        this.manager = manager;
    }

    @Listener
    public void onCloseInventory(InteractInventoryEvent.Close event, @Root Player player, @Getter("getTargetInventory") Container inventory)
    {
        // When closing update filters and prompt activation
        if (inventory instanceof CarriedInventory && inventory.size() > 0)
        {
            ((CarriedInventory<?>) inventory).getCarrier().ifPresent(carrier -> this.promptActivation(carrier, true, player));
        }
    }

    @Listener
    public void onOpenInventory(InteractInventoryEvent.Open event, @Root Player player, @Getter("getTargetInventory") Container inventory)
    {
        // When opening prompt activation
        if (inventory instanceof CarriedInventory && inventory.size() > 0)
        {
            ((CarriedInventory<?>) inventory).getCarrier().ifPresent(carrier -> this.promptActivation(carrier, false, player));
        }
    }

    @Listener
    public void onTransferInventory(ChangeInventoryEvent.Transfer.Pre event, @Getter("getTargetInventory") Container inventory)
    {
        // When getting items transferred prompt activation
        if (inventory instanceof CarriedInventory && inventory.size() > 0)
        {
            ((CarriedInventory<?>) inventory).getCarrier().ifPresent(c -> this.promptActivation(c, true, null));
        }
    }

    @Listener
    public void onInteractPiston(InteractBlockEvent.Secondary.MainHand event, @Root Player player)
    {
        event.getTargetBlock().getLocation().ifPresent(loc -> {
            Direction dir = loc.get(Keys.DIRECTION).orElse(Direction.NONE);
            Location<World> te = loc.getRelative(dir);
            Optional<DuctData> ductData = te.get(DuctData.class);
            Optional<ItemStack> itemInHand = player.getItemInHand(HandTypes.MAIN_HAND);

            if (ductData.isPresent() && !itemInHand.isPresent() && !player.get(Keys.IS_SNEAKING).orElse(false))
            {
                manager.playEffect(loc); // Play Effect for DuctPiston
            }
        });
    }

    private void promptActivation(Carrier carrier, boolean push, Player player)
    {
        if (!(carrier instanceof TileEntity))
        {
            return;
        }
        Location<World> loc = ((TileEntity) carrier).getLocation();
        Optional<DuctData> data = loc.get(DuctData.class);
        if (data.isPresent())
        {
            if (!push)
            {
                // Pull only activates OBSERVER
                boolean pull = false;
                for (Direction dir : data.get().getFilters().keySet())
                {
                    if (loc.getRelative(dir).getBlockType().equals(OBSERVER))
                    {
                        pull = true;
                    }
                }
                if (!pull)
                {
                    return;
                }
            }
            if (this.promptedActivations.isEmpty())
            {
                if (task != null)
                {
                    task.cancel();
                }
                task = Sponge.getScheduler().createTaskBuilder().delayTicks(20).intervalTicks(20).execute(this::activate).submit(plugin);
            }
            this.promptedActivations.computeIfAbsent(loc, k -> System.currentTimeMillis());
            if (player != null)
            {
                player.getProgress(module.prompted).get(module.promptCriterion).get().add(1);
            }

        }
    }

    private void activate()
    {
        List<Network> networks = new ArrayList<>();
        for (Iterator<Location<World>> it = this.promptedActivations.keySet().iterator(); it.hasNext(); )
        {
            Location<World> loc = it.next();

            if (this.promptedActivations.get(loc) - 1000 > System.currentTimeMillis())
            {
                promptedActivations.clear();
                continue;
            }

            it.remove();

            // Check if data is still present
            Optional<DuctData> data = loc.get(DuctData.class);
            if (data.isPresent()) {
                for (Direction dir : Direction.values())
                {
                    if (dir.isCardinal() || dir.isUpright())
                    {
                        BlockType type = loc.getRelative(dir).getBlockType();
                        if (STICKY_PISTON.equals(type) || OBSERVER.equals(type))
                        {
                            Optional<List<ItemStack>> filters = data.get().get(dir);
                            if (filters.isPresent())
                            {
                                Network network = manager.findNetwork(loc.getRelative(dir));
                                TileEntity te = loc.getTileEntity().get();
                                Inventory inventory = ((Carrier) te).getInventory();
                                if (te instanceof Chest)
                                {
                                    inventory = ((Chest) te).getDoubleChestInventory().orElse(inventory);
                                }
                                network.activate(inventory, filters.get());
                                networks.add(network);
                            }
                        }
                    }
                }
            }
        }

        for (Network network : networks)
        {
            for (Location<World> exitLoc : network.exitPoints.keySet())
            {
                Direction exitDir = exitLoc.get(Keys.DIRECTION).orElse(Direction.NONE).getOpposite();
                exitLoc = exitLoc.getRelative(exitDir.getOpposite());
                promptActivation(exitLoc.getTileEntity().filter(t -> t instanceof Carrier).map(Carrier.class::cast).orElse(null), true, null);
            }
        }

        if (promptedActivations.isEmpty())
        {
            task.cancel();
        }
    }
}
