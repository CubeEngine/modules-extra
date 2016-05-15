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
package org.cubeengine.module.itemrepair;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.itemrepair.repair.RepairBlockManager;
import org.cubeengine.module.itemrepair.repair.RepairRequest;
import org.cubeengine.module.itemrepair.repair.blocks.RepairBlock;
import org.cubeengine.module.itemrepair.repair.blocks.RepairBlock.RepairBlockInventory;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;

public class ItemRepairListener
{
    private final Itemrepair module;
    private final RepairBlockManager rbm;
    private final Map<UUID, RepairRequest> repairRequests;
    private I18n i18n;

    public ItemRepairListener(Itemrepair module, I18n i18n)
    {
        this.module = module;
        this.rbm = module.getRepairBlockManager();
        this.i18n = i18n;
        this.repairRequests = new HashMap<>();
    }

    @Listener
    public void onPlayerQuit(ClientConnectionEvent.Disconnect event)
    {
        this.repairRequests.remove(event.getTargetEntity().getUniqueId());
        this.rbm.removePlayer(event.getTargetEntity());
    }

    @Listener
    public void onPlayerInteract(InteractBlockEvent event, @First Player player)
    {
        final Location<World> block = event.getTargetBlock().getLocation().orElse(null);
        if (block == null)
        {
            return;
        }
        RepairBlock repairBlock = this.rbm.getRepairBlock(block);
        if (repairBlock == null)
        {
            return;
        }
        event.setCancelled(true);

        if (!player.hasPermission(repairBlock.getPermission().getId()))
        {
            i18n.sendTranslated(player, NEGATIVE, "You are not allowed to use this repair block!");
            return;
        }

        RepairBlockInventory inventory = repairBlock.getInventory(player);
        
        if (event.getAction() == RIGHT_CLICK_BLOCK)
        {
            this.cancelRequest(event);
            player.openInventory(inventory.inventory);
        }
        else if (event.getAction() == LEFT_CLICK_BLOCK)
        {
            event.setCancelled(true);
            if (this.repairRequests.containsKey(player.getUniqueId()))
            {
                RepairRequest request = this.repairRequests.get(player.getUniqueId());
                if (request.getRepairBlock() == repairBlock)
                {
                    repairBlock.repair(request);
                    this.repairRequests.remove(player.getUniqueId());
                }
            }
            else
            {
                if (!this.repairRequests.containsKey(player.getUniqueId()))
                {
                    RepairRequest request = repairBlock.requestRepair(inventory);
                    if (request != null)
                    {
                        this.repairRequests.put(player.getUniqueId(), request);
                    }
                }
            }
        }
        else
        {
            this.cancelRequest(event);
        }
    }

    @Listener
    public void onCancelRepair(PlayerInteractEvent event)
    {
        this.cancelRequest(event);
    }

    private void cancelRequest(PlayerInteractEvent event)
    {
        if (event.getAction() != Action.PHYSICAL)
        {
            final User user = this.module.getCore().getUserManager().getExactUser(event.getPlayer().getUniqueId());
            if (this.repairRequests.containsKey(user.getUniqueId()))
            {
                user.sendTranslated(NEUTRAL, "The repair has been cancelled!");
                this.repairRequests.remove(user.getUniqueId());
                event.setCancelled(true);
            }
        }
    }
}
