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
package org.cubeengine.module.vigil;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.vigil.data.VigilData;
import org.cubeengine.module.vigil.storage.QueryManager;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent.Primary.Start;
import org.spongepowered.api.event.block.InteractBlockEvent.Secondary;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.world.server.ServerLocation;

@Singleton
public class ToolListener
{
    private QueryManager qm;
    private final Permission toolPerm;

    @Inject
    public ToolListener(PermissionManager pm, QueryManager qm)
    {
        this.qm = qm;
        toolPerm = pm.register(Vigil.class, "use-logtool", "Allows using log-tools", null);
    }

    @Listener
    public void onClick(InteractBlockEvent.Secondary event, @First ServerPlayer player)
    {
        handleLRClicks(event, player);
    }

    @Listener
    public void onClick(InteractBlockEvent.Primary.Start event, @First ServerPlayer player)
    {
        handleLRClicks(event, player);
    }

    private void handleLRClicks(InteractBlockEvent event, ServerPlayer player)
    {
        if (event.context().get(EventContextKeys.USED_HAND).map(h -> h.equals(HandTypes.MAIN_HAND.get())).orElse(false))
        {
            ItemStack itemInHand = player.itemInHand(HandTypes.MAIN_HAND);
            itemInHand.get(VigilData.REPORTS).ifPresent(reports -> {
                if (!toolPerm.check(player) || event.block() == BlockSnapshot.NONE.get())
                {
                    return;
                }
                ServerLocation loc;
                if (event instanceof InteractBlockEvent.Primary.Start)
                {
                    loc = event.block().location().get();

                    ((Start)event).setCancelled(true);
                }
                else if (event instanceof InteractBlockEvent.Secondary)
                {
                    loc = event.block().location().get().relativeTo(event.targetSide());
                    ((Secondary)event).setCancelled(true);
                }
                else
                {
                    throw new IllegalStateException("impossible");
                }
                qm.queryAndShow(new Lookup(itemInHand).with(loc), player);

            });
        }
    }

    @Listener
    public void onDropTool(DropItemEvent.Pre event)
    {
        event.droppedItems().removeIf(item -> item.get(VigilData.REPORTS).isPresent());
    }
}
