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
package org.cubeengine.module.log.tool;

import org.cubeengine.module.log.Log;
import org.cubeengine.module.log.LogAttachment;
import org.cubeengine.module.log.storage.Lookup;
import org.cubeengine.module.log.storage.ShowParameter;
import org.cubeengine.module.log.commands.LogCommands;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.i18n.formatter.MessageType;
import org.cubeengine.service.permission.PermissionManager;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.inventory.DropItemEvent;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static java.util.stream.Collectors.toList;
import static org.spongepowered.api.data.key.Keys.DISPLAY_NAME;

public class ToolListener
{
    private final Log module;
    private I18n i18n;
    private final PermissionDescription toolPerm;

    public ToolListener(Log module, I18n i18n, PermissionManager pm)
    {
        this.module = module;
        this.i18n = i18n;
        toolPerm = pm.register(module, "use-logtool", "Allows using log-tools", null);
    }

    @Listener
    public void onClick(InteractBlockEvent event)
    {
        event.getCause().first(Player.class).ifPresent(player -> {
            // TODO don't trigger on pressureplates?
            if (player.hasPermission(toolPerm.getId()))
            {
                player.getItemInHand().ifPresent(item -> item.get(DISPLAY_NAME).ifPresent(display -> {
                    if (display.equals(LogCommands.toolName))
                    {
                        LogAttachment attachment = module.getAttachment(player);
                        Lookup lookup = attachment.getLookup(item.getItem());
                        if (lookup == null)
                        {
                            i18n.sendTranslated(player, MessageType.NEGATIVE, "Invalid LoggingTool-Block!");
                            return;
                        }

                        Location<World> loc = event instanceof InteractBlockEvent.Primary ? event.getTargetBlock().getLocation().get() : event.getTargetBlock().getLocation().get().getRelative(event.getTargetSide());
                        lookup.getQueryParameter().setSingleLocations(loc);

                        ShowParameter show = new ShowParameter();
                        show.showCoords = false;
                        attachment.queueShowParameter(show);
                        this.module.getLogManager().fillLookupAndShow(lookup, player);
                        event.setCancelled(true);
                    }
                }));
            }
        });
    }

    @Listener
    public void onDrop(DropItemEvent.Toss event)
    {
        event.getEntities().retainAll(event.getEntities().stream()
            .filter(item -> item.getItemData().item().get().createStack().get(DISPLAY_NAME).map(
                display -> !display.equals(LogCommands.toolName)).orElse(true)).collect(toList()));
    }

    // TODO InteractEntity show logs of entity
}
