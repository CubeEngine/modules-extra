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

import org.cubeengine.module.vigil.storage.QueryManager;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.permission.PermissionManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.manipulator.mutable.DisplayNameData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.permission.PermissionDescription;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;

import java.util.Optional;

import static org.cubeengine.module.vigil.commands.VigilCommands.toolName;

public class ToolListener
{
    private Game game;
    private I18n i18n;
    private final PermissionManager pm;
    private QueryManager qm;
    private final PermissionDescription toolPerm;

    public ToolListener(Vigil module, I18n i18n, PermissionManager pm, QueryManager qm, Game game)
    {
        this.i18n = i18n;
        this.pm = pm;
        this.qm = qm;
        this.game = game;
        toolPerm = pm.register(module, "use-logtool", "Allows using log-tools", null);
    }

    @Listener
    public void onClick(InteractBlockEvent event, @First Player player)
    {
        if (!player.hasPermission(toolPerm.getId()))
        {
            return;
        }
        Optional<ItemStack> itemInHand = player.getItemInHand();
        if (itemInHand.isPresent() && itemInHand.get().get(DisplayNameData.class)
                                        .map(data -> data.displayName().get().toPlain().equals(toolName.toPlain()))
                                        .orElse(false))
        {
            Location loc;
            if (event instanceof InteractBlockEvent.Primary)
            {
                loc = event.getTargetBlock().getLocation().get();
            }
            else
            {
                loc = event.getTargetBlock().getLocation().get().getRelative(event.getTargetSide());
            }
            // TODO generate and pass Lookup with parameters and show settings
            // TODO set lookuplocation to Block ; left: clicked block ; righ: would placed block
            qm.queryAndShow(loc.getPosition(), player);
            event.setCancelled(true);
        }
    }
}
