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
package org.cubeengine.module.itemrepair;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.itemrepair.repair.RepairBlockManager;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent.Primary;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent.Secondary;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Command(name = "itemrepair", desc = "ItemRepair commands", alias = "ir")
public class ItemRepairCommands extends ContainerCommand
{
    private final Set<UUID> removeRequests;
    private final Set<UUID> addRequests;
    private final RepairBlockManager rbm;
    private final Itemrepair module;
    private I18n i18n;

    public ItemRepairCommands(CommandManager cm, Itemrepair module, EventManager em, I18n i18n)
    {
        super(cm, Itemrepair.class);

        this.module = module;
        this.i18n = i18n;
        em.registerListener(Itemrepair.class, this);
        this.rbm = module.getRepairBlockManager();
        this.addRequests = new HashSet<>();
        this.removeRequests = new HashSet<>();
    }

    @Command(desc = "Adds a new RepairBlock")
    @Restricted(value = Player.class, msg = "You only need to right-click... {text:NOW!:color=DARK_RED}\n" + "Too slow.")
    public void add(Player context)
    {
        if (this.addRequests.contains(context.getUniqueId()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are already adding a repair block!");
            return;
        }
        if (this.removeRequests.contains(context.getUniqueId()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are already removing a repair block!");
            return;
        }
        this.addRequests.add(context.getUniqueId());
        i18n.sendTranslated(context, NEUTRAL, "Rightclick the block.");
    }

    @Command(desc = "Removes an existing RepairBlock")
    public void remove(Player context)
    {
        if (this.removeRequests.contains(context.getUniqueId()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are already removing a repair block!");
            return;
        }
        if (this.addRequests.contains(context.getUniqueId()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are already adding a repair block!");
            return;
        }
        this.removeRequests.add(context.getUniqueId());
        i18n.sendTranslated(context, NEUTRAL, "Rightclick the block.");
    }

    @Listener
    public void onAdd(InteractBlockEvent event, @First Player player)
    {
        if (!this.addRequests.contains(player.getUniqueId()) || !event.getTargetBlock().getLocation().isPresent())
        {
            return;
        }
        if (event instanceof InteractBlockEvent.Primary)
        {
            this.addRequests.remove(player.getUniqueId());
            return;
        }
        event.setCancelled(true);
        Location<World> block = event.getTargetBlock().getLocation().get();
        if (rbm.isRepairBlock(block))
        {
            i18n.sendTranslated(player, NEGATIVE, "This block is already a repair block!");
        }
        this.addRequests.remove(player.getUniqueId());
        if (rbm.attachRepairBlock(block))
        {
            i18n.sendTranslated(player, POSITIVE, "Repair block successfully added!");
            return;
        }
        i18n.sendTranslated(player, NEGATIVE, "This block can't be used as a repair block!");
    }

    @Listener
    public void onRemove(InteractBlockEvent event, @First Player player)
    {
        if (!this.removeRequests.contains(player.getUniqueId()))
        {
            return;
        }
        if (event instanceof InteractBlockEvent.Secondary)
        {
            event.setCancelled(true);
            if (this.rbm.detachRepairBlock(event.getTargetBlock().getLocation().get()))
            {
                i18n.sendTranslated(player, POSITIVE, "Repair block successfully removed!");
            }
            else
            {
                i18n.sendTranslated(player, NEGATIVE, "This block is not a repair block!");
            }
            this.removeRequests.remove(player.getUniqueId());
        }
    }
}
