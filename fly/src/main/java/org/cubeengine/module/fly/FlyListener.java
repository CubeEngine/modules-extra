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
package org.cubeengine.module.fly;

import java.util.HashMap;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.world.Location;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

public class FlyListener
{
    private final HashMap<Player, Task> tasks = new HashMap<>();
    private final Fly module;
    private I18n i18n;
    private final Location helperLocation = new Location(null, 0, 0, 0);

    private final Permission FLY_FEATHER;

    public FlyListener(Fly module, PermissionManager pm, I18n i18n)
    {
        this.module = module;
        this.i18n = i18n;
        FLY_FEATHER = pm.register(module, "feather", "", null);
    }

    @Listener
    public void playerInteract(final InteractBlockEvent.Secondary event, @First Player player)
    {
        if (!player.getItemInHand().map(i -> i.getItem().equals(ItemTypes.FEATHER)).orElse(false))
        {
            return;
        }

        if (!player.hasPermission(FLY_FEATHER.getId()))
        {
            i18n.sendTranslated(player, NEGATIVE, "You dont have permission to use this!");
            player.setAllowFlight(false); //Disable when player is flying
            return;
        }

        FlyStartEvent flyStartEvent = new FlyStartEvent(module, player);
        if (flyStartEvent.isCancelled())
        {
            i18n.sendTranslated(player, NEGATIVE, "You are not allowed to fly now!");
            player.setAllowFlight(false); //Disable when player is flying
            return;
        }
        //I Believe I Can Fly ...     
        player.setAllowFlight(!player.getAllowFlight());
        if (player.getAllowFlight())
        {
            final ItemStack feather = new ItemStack(Material.FEATHER, 1);
            player.getInventory().removeItem(feather);
            player.setVelocity(player.getVelocity().setY(player.getVelocity().getY() + 1));
            player.teleport(player.getLocation(this.helperLocation).add(new Vector(0, 0.05, 0))); //make sure the player stays flying
            player.setFlying(true);
            i18n.sendTranslated(player, POSITIVE, "You can now fly!");
            Task flymore = new Task(module)
            {
                public void run()//2 feather/min
                {
                    if (!player.isFlying())
                    {
                        player.setAllowFlight(false);
                        this.cancelTask();
                        return;
                    }
                    if (player.getInventory().contains(Material.FEATHER))
                    {
                        player.getInventory().removeItem(feather);
                    }
                    else
                    {
                        player.setAllowFlight(false);
                        this.cancelTask();
                    }
                }
            };
            flymore.scheduleAsyncRepeatingTask(1000 * 30, 1000 * 30);
            Task oldTask = this.tasks.put(player, flymore);
            if (oldTask != null)
            {
                oldTask.cancelTask();
            }
        }
        else
        {//or not
            player.setFallDistance(0);
            i18n.sendTranslated(player, NEUTRAL, "You cannot fly anymore!");
        }
    }
}
