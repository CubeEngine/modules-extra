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

import com.google.inject.Inject;
import org.cubeengine.module.itemduct.ItemDuctManager;
import org.cubeengine.module.itemduct.Itemduct;
import org.cubeengine.module.itemduct.Network;
import org.cubeengine.module.itemduct.data.DuctData;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.block.entity.carrier.chest.Chest;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandType;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.TransferInventoryEvent;
import org.spongepowered.api.event.item.inventory.container.InteractContainerEvent;
import org.spongepowered.api.item.inventory.BlockCarrier;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.MultiBlockCarrier;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;
import org.spongepowered.plugin.PluginContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles ItemDuct transfer activation
 */
public class ItemDuctTransferListener
{
    @Inject private PluginContainer plugin;
    private Itemduct module;
    private ItemDuctManager manager;

    private final Map<ResourceKey, Map<Vector3i, Long>> promptedActivations = new HashMap<>();
    private ScheduledTask task;

    public void setup(Itemduct module, ItemDuctManager manager)
    {
        this.module = module;
        this.manager = manager;
    }

    @Listener
    public void onCloseInventory(InteractContainerEvent.Close event, @Root ServerPlayer player, @Getter("getContainer") Container inventory)
    {
        // When closing update filters and prompt activation
        if (inventory instanceof CarriedInventory && inventory.totalQuantity() > 0)
        {
            ((CarriedInventory<?>) inventory).getCarrier().ifPresent(carrier -> this.promptActivation(carrier, true, player));
        }
    }

    @Listener
    public void onOpenInventory(InteractContainerEvent.Open event, @Root ServerPlayer player, @Getter("getContainer") Container inventory)
    {
        // When opening prompt activation
        if (inventory instanceof CarriedInventory && inventory.totalQuantity() > 0)
        {
            ((CarriedInventory<?>) inventory).getCarrier().ifPresent(carrier -> this.promptActivation(carrier, false, player));
        }
    }

    @Listener
    public void onTransferInventory(TransferInventoryEvent.Pre event)
    {
        // When getting items transferred prompt activation
        if (event.getTargetInventory() instanceof CarriedInventory)
        {
            ((CarriedInventory<?>) event.getTargetInventory()).getCarrier().ifPresent(c -> this.promptActivation(c, true, null));
        }
    }

    @Listener
    public void onInteractPiston(InteractBlockEvent.Secondary event, @Root ServerPlayer player)
    {
        final Optional<HandType> usedHand = event.getContext().get(EventContextKeys.USED_HAND);
        if (usedHand.isPresent() && usedHand.get() == HandTypes.MAIN_HAND.get()) {
            event.getInteractionPoint().ifPresent(pos -> {
                final ServerLocation loc = player.getWorld().getLocation(pos);
                final Direction dir = loc.get(Keys.DIRECTION).orElse(Direction.NONE);
                final ServerLocation te = loc.add(dir.asBlockOffset());
                final Optional<Map<Direction, List<ItemStack>>> filters = te.get(DuctData.FILTERS);
                final ItemStack itemInHand = player.getItemInHand(HandTypes.MAIN_HAND);
                if (filters.isPresent() && itemInHand.isEmpty() && !player.get(Keys.IS_SNEAKING).orElse(false)) {
                    manager.playEffect(loc);
                }
            });
        }
    }

    private void promptActivation(Carrier carrier, boolean push, ServerPlayer player)
    {
        if (!(carrier instanceof BlockCarrier))
        {
            return;
        }
        if (carrier instanceof MultiBlockCarrier) {
            for (ServerLocation loc : ((MultiBlockCarrier) carrier).getLocations()) {
                promptAtLoc(push, player, loc);
            }
            return;
        }
        ServerLocation loc = ((BlockCarrier) carrier).getServerLocation();
        promptAtLoc(push, player, loc);
    }

    private void promptAtLoc(boolean push, ServerPlayer player, ServerLocation loc) {
        final Optional<Map<Direction, List<ItemStack>>> data = loc.get(DuctData.FILTERS);
        if (data.isPresent()) {
            if (!push)
            {
                // Pull only activates OBSERVER
                boolean pull = false;
                for (Direction dir : data.get().keySet())
                {
                    if (loc.add(dir.asBlockOffset()).getBlockType().equals(OBSERVER.get()))
                    {
                        pull = true;
                    }
                }
                if (!pull)
                {
                    return;
                }
            }
            final Map<Vector3i, Long> activationMap = this.promptedActivations.computeIfAbsent(loc.getWorldKey(), k -> new HashMap<>());
            if (activationMap.isEmpty())
            {
                if (task != null)
                {
                    task.cancel();
                }
                final Task build = Task.builder().delayTicks(20).intervalTicks(20).execute(this::activate).plugin(plugin).build();
                task = Sponge.getServer().getScheduler().submit(build);
            }

            activationMap.computeIfAbsent(loc.getBlockPosition(), k -> System.currentTimeMillis());
            if (player != null)
            {
                player.getProgress(module.prompted).get(module.promptCriterion).ifPresent(c -> c.add(1));
            }
        }
    }

    private void activate()
    {
        List<Network> networks = new ArrayList<>();
        for (Map.Entry<ResourceKey, Map<Vector3i, Long>> entry : this.promptedActivations.entrySet()) {
            final ServerWorld world = Sponge.getServer().getWorldManager().getWorld(entry.getKey()).orElse(null);
            final Map<Vector3i, Long> activatedPositions = entry.getValue();
            if (world == null) {
                activatedPositions.clear();
                continue;
            }
            for (Iterator<Vector3i> it = activatedPositions.keySet().iterator(); it.hasNext(); )
            {
                final Vector3i pos = it.next();
                ServerLocation loc = world.getLocation(pos);

                if (activatedPositions.get(pos) - 1000 > System.currentTimeMillis())
                {
                    activatedPositions.clear();
                    continue;
                }

                it.remove();

                // Check if data is still present
                final Optional<Map<Direction, List<ItemStack>>> data = loc.get(DuctData.FILTERS);
                if (data.isPresent())
                {
                    for (Direction dir : Direction.values())
                    {
                        if (dir.isCardinal() || dir.isUpright())
                        {
                            BlockType type = loc.add(dir.asBlockOffset()).getBlockType();
                            if (type.isAnyOf(STICKY_PISTON, OBSERVER))
                            {
                                List<ItemStack> filters = data.get().get(dir);
                                if (filters != null)
                                {
                                    Network network = manager.findNetwork(loc.add(dir.asBlockOffset()));
                                    BlockEntity te = loc.getBlockEntity().get();
                                    Inventory inventory = ((Carrier) te).getInventory();
                                    if (te instanceof Chest)
                                    {
                                        inventory = ((Chest) te).getDoubleChestInventory().orElse(inventory);
                                    }
                                    network.activate(inventory, filters);
                                    networks.add(network);
                                }
                            }
                        }
                    }
                }
            }
        }




        for (Network network : networks)
        {
            for (ServerLocation exitLoc : network.exitPoints.keySet())
            {
                Direction exitDir = exitLoc.get(Keys.DIRECTION).orElse(Direction.NONE).getOpposite();
                exitLoc = exitLoc.add(exitDir.getOpposite().asBlockOffset());
                promptActivation(exitLoc.getBlockEntity().filter(t -> t instanceof Carrier).map(Carrier.class::cast).orElse(null), true, null);
            }
        }

        if (promptedActivations.isEmpty())
        {
            task.cancel();
        }
    }
}
