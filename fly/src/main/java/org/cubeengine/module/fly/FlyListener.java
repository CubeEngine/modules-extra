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
package org.cubeengine.module.fly;

import java.util.HashMap;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.libcube.service.task.Task;
import org.cubeengine.libcube.service.task.TaskManager;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

public class FlyListener
{
    private final HashMap<Player, Task> tasks = new HashMap<>();
    private final Fly module;
    private I18n i18n;

    private final Permission FLY_FEATHER;
    private TaskManager tm;

    public FlyListener(Fly module, PermissionManager pm, I18n i18n, TaskManager tm)
    {
        this.module = module;
        this.i18n = i18n;
        FLY_FEATHER = pm.register(Fly.class, "feather", "", null);
        this.tm = tm;
    }

    @Listener
    public void playerInteract(final InteractBlockEvent.Secondary event, @First Player player)
    {
        if (!player.getItemInHand(HandTypes.MAIN_HAND).map(i -> i.getItem().equals(ItemTypes.FEATHER)).orElse(false))
        {
            return;
        }

        if (!player.hasPermission(FLY_FEATHER.getId()))
        {
            i18n.send(player, NEGATIVE, "You dont have permission to use this!");
            player.offer(Keys.CAN_FLY, false); //Disable when player is flying
            player.offer(Keys.IS_FLYING, false);
            return;
        }

        //I Believe I Can Fly ...
        player.offer(Keys.CAN_FLY, !player.get(Keys.CAN_FLY).get());
        if (player.get(Keys.CAN_FLY).get())
        {
            final ItemStack feather = ItemStack.of(ItemTypes.FEATHER, -1);
            player.getInventory().query(feather).poll(1);
            player.setVelocity(player.getVelocity().add(0, 1, 0));
            player.setLocation(player.getLocation().add(0, 0.05, 0)); //make sure the player stays flying
            player.offer(Keys.IS_FLYING, true);
            i18n.send(player, POSITIVE, "You can now fly!");
            Task flymore = new Task(Fly.class, tm)
            {
                public void run()//2 feather/min
                {
                    if (!player.get(Keys.IS_FLYING).get())
                    {
                        player.offer(Keys.CAN_FLY, false);
                        this.cancelTask();
                        return;
                    }
                    if (player.getInventory().contains(ItemTypes.FEATHER))
                    {
                        player.getInventory().query(feather).poll(1);
                    }
                    else
                    {
                        player.offer(Keys.CAN_FLY, false);
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
            player.offer(Keys.FALL_DISTANCE, 0f);
            i18n.send(player, NEUTRAL, "You cannot fly anymore!");
        }
    }
}
