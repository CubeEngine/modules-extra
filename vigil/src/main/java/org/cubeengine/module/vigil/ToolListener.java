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
package org.cubeengine.module.vigil;

import java.util.Optional;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.module.vigil.storage.QueryManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.manipulator.mutable.DisplayNameData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.module.vigil.commands.VigilCommands.toolName;

public class ToolListener
{
    private QueryManager qm;
    private final Permission toolPerm;

    public ToolListener(PermissionManager pm, QueryManager qm)
    {
        this.qm = qm;
        toolPerm = pm.register(Vigil.class, "use-logtool", "Allows using log-tools", null);
    }

    @Listener
    public void onClick(InteractBlockEvent event, @First Player player)
    {
        if (!player.hasPermission(toolPerm.getId()) || event.getTargetBlock() == BlockSnapshot.NONE)
        {
            return;
        }
        Optional<ItemStack> itemInHand = player.getItemInHand();
        if (itemInHand.isPresent() && itemInHand.get().get(DisplayNameData.class)
                                        .map(data -> data.displayName().get().toPlain().equals(toolName.toPlain()))
                                        .orElse(false))
        {
            Location<World> loc;
            if (event instanceof InteractBlockEvent.Primary)
            {
                loc = event.getTargetBlock().getLocation().get();
            }
            else
            {
                loc = event.getTargetBlock().getLocation().get().getRelative(event.getTargetSide());
            }

            qm.queryAndShow(Lookup.builder().with(loc).build(), player);
            event.setCancelled(true);
        }
    }
}
