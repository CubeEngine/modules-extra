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

import static org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType.ACTION_BAR;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.spongepowered.api.block.BlockTypes.OBSERVER;
import static org.spongepowered.api.block.BlockTypes.STICKY_PISTON;

import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.itemduct.data.ItemductAdvancements;
import org.cubeengine.module.itemduct.data.ItemductBlocks;
import org.cubeengine.module.itemduct.data.ItemductData;
import org.cubeengine.module.itemduct.data.ItemductEffects;
import org.cubeengine.module.itemduct.data.ItemductItems;
import org.cubeengine.module.itemduct.data.ItemductPerms;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.block.entity.carrier.chest.Chest;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.item.inventory.BlockCarrier;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.ContainerTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.MultiBlockCarrier;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.item.inventory.type.ViewableInventory;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;
import org.spongepowered.plugin.PluginContainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ItemductManager
{
    @Inject private I18n i18n;
    @Inject public ItemductBlocks blocks;
    @Inject private ItemductPerms perms;
    @Inject private PluginContainer plugin;

    private int maxPipes = 1000;

    public void setup(ItemductConfig config)
    {
        this.reload(config);
        this.blocks.init();
    }

    public static void updateUses(ItemStack item)
    {
        int uses = item.get(ItemductData.USES).orElse(0);
        if (uses > 0)
        {
            item.offer(Keys.LORE, Collections.singletonList(Component.text("Uses: ").append(Component.text(uses))));
        }
        else if (uses == -1)
        {
            item.offer(Keys.LORE, Collections.singletonList(Component.text("Uses: Infinite")));
        }
    }

    public void reload(ItemductConfig config)
    {
        this.maxPipes = config.maxPipes;
    }

    public Network discoverNetwork(ServerWorld world, Vector3i start)
    {
        return new Network(blocks, world, maxPipes).discover(start);
    }

    public void openFilter(ServerPlayer player, NetworkFilter networkFilter)
    {
        if (!perms.FILTER_VIEW.check(player))
        {
            i18n.send(ACTION_BAR, player, NEGATIVE, "You are not allowed to edit filters");
            return;
        }

        final ViewableInventory inventory = ViewableInventory.builder().type(ContainerTypes.GENERIC_9X3).completeStructure().build();
        networkFilter.getFilterStacks().forEach(inventory::offer);
        final InventoryMenu menu = InventoryMenu.of(inventory);
        boolean canEdit = perms.FILTER_EDIT.check(player);
        menu.setReadOnly(!canEdit);
        menu.setTitle(canEdit ? i18n.translate(player, "ItemDuct Filters") :
                                i18n.translate(player, "View ItemDuct Filters"));
        menu.registerClose((cause, container) -> onClose(inventory, networkFilter));
        menu.open(player);
        player.progress(ItemductAdvancements.USE_FILTERS).grant();
    }

    private void onClose(ViewableInventory inventory, NetworkFilter networkFilter)
    {
        List<ItemStack> list = new ArrayList<>();
        for (Slot slot : inventory.slots()) {
            final ItemStack item = slot.peek();
            if (!item.isEmpty()) {
                list.add(item);
            }
        }
        networkFilter.setFilterStacks(list);

    }

    public void playNetworkEffects(ServerLocation loc)
    {
        final Network network = discoverNetwork(loc.world(), loc.blockPosition());
        ItemductEffects.playNetworkSound(loc.position(), network);
        ItemductEffects.playNetworkEffects(network);
    }

    public void activateNetwork(InteractBlockEvent.Secondary event, ServerPlayer player, ItemStack itemInHand, NetworkFilter networkFilter) {
        if (networkFilter.filterLoc.blockType().isAnyOf(BlockTypes.OBSERVER))
        {
            if (!perms.ACTIVATE_OBSERVER.check(player)) {
                event.setCancelled(true);
            }
        }
        else if (!perms.ACTIVATE_PISTON.check(player)) {
            event.setCancelled(true);
            return;
        }

        ItemductItems.consumeActivator(player, itemInHand);
        networkFilter.setFilterStacks(new ArrayList<>());
        ItemductEffects.playCreateEffect(networkFilter.filterLoc);

        player.progress(ItemductAdvancements.ACTIVATE_NETWORK).grant();
    }

    private final Map<ResourceKey, Map<Vector3i, Long>> triggers = new HashMap<>();
    private ScheduledTask task;

    public void triggerAtLoc(boolean push, ServerPlayer player, ServerLocation loc) {
        final Optional<Map<Direction, List<ItemStack>>> data = loc.get(ItemductData.FILTERS);
        if (data.isPresent()) {
            if (!push)
            {
                // Pull only activates OBSERVER
                boolean pull = false;
                for (Direction dir : data.get().keySet())
                {
                    if (loc.relativeTo(dir).blockType().isAnyOf(OBSERVER))
                    {
                        pull = true;
                    }
                }
                if (!pull)
                {
                    return;
                }
            }
            final Map<Vector3i, Long> activationMap = triggers.computeIfAbsent(loc.worldKey(), k -> new HashMap<>());
            if (activationMap.isEmpty())
            {
                if (task != null)
                {
                    task.cancel();
                }
                final Task build = Task.builder().delay(Ticks.of(20)).interval(Ticks.of(20)).execute(this::trigger).plugin(plugin).build();
                task = Sponge.server().scheduler().submit(build);
            }

            activationMap.computeIfAbsent(loc.blockPosition(), k -> System.currentTimeMillis());
            if (player != null)
            {
                player.progress(ItemductAdvancements.USE_NETWORK).get(ItemductAdvancements.USE_NETWORK_CRITERION).ifPresent(c -> c.add(1));
            }
        }
    }

    private void trigger()
    {
        List<Network> networks = new ArrayList<>();
        for (Map.Entry<ResourceKey, Map<Vector3i, Long>> entry : triggers.entrySet()) {
            final ServerWorld world = Sponge.server().worldManager().world(entry.getKey()).orElse(null);
            final Map<Vector3i, Long> activatedPositions = entry.getValue();
            if (world == null) {
                activatedPositions.clear();
                continue;
            }
            for (Iterator<Vector3i> it = activatedPositions.keySet().iterator(); it.hasNext(); )
            {
                final Vector3i pos = it.next();
                ServerLocation loc = world.location(pos);

                if (activatedPositions.get(pos) - 1000 > System.currentTimeMillis())
                {
                    activatedPositions.clear();
                    continue;
                }

                it.remove();

                // Check if data is still present
                final Optional<Map<Direction, List<ItemStack>>> data = loc.get(ItemductData.FILTERS);
                if (data.isPresent())
                {
                    for (Direction dir : Direction.values())
                    {
                        if (dir.isCardinal() || dir.isUpright())
                        {
                            BlockType type = loc.relativeTo(dir).blockType();
                            if (type.isAnyOf(STICKY_PISTON, OBSERVER))
                            {
                                List<ItemStack> filters = data.get().get(dir);
                                if (filters != null)
                                {
                                    Network network = discoverNetwork(loc.world(), loc.blockPosition().add(dir.asBlockOffset()));
                                    BlockEntity te = loc.blockEntity().get();
                                    Inventory inventory = ((Carrier) te).inventory();
                                    if (te instanceof Chest)
                                    {
                                        inventory = ((Chest) te).doubleChestInventory().orElse(inventory);
                                    }
                                    network.trigger(inventory, filters);
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
            for (Vector3i exitLoc : network.exitPoints.keySet())
            {

                Direction exitDir = network.world.get(exitLoc, Keys.DIRECTION).orElse(Direction.NONE).opposite();
                exitLoc = exitLoc.add(exitDir.opposite().asBlockOffset());
                promptActivation(network.world.blockEntity(exitLoc).filter(t -> t instanceof Carrier).map(Carrier.class::cast).orElse(null), true, null);
            }
        }

        if (triggers.isEmpty())
        {
            task.cancel();
        }
    }

    public void promptActivation(Carrier carrier, boolean push, ServerPlayer player)
    {
        if (!(carrier instanceof BlockCarrier))
        {
            return;
        }
        if (carrier instanceof MultiBlockCarrier) {
            for (ServerLocation loc : ((MultiBlockCarrier) carrier).locations()) {
                triggerAtLoc(push, player, loc);
            }
            return;
        }
        ServerLocation loc = ((BlockCarrier) carrier).serverLocation();
        triggerAtLoc(push, player, loc);
    }

}
