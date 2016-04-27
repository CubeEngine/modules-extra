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
package org.cubeengine.module.module.kits;

import java.util.List;
import org.cubeengine.butler.parameter.IncorrectUsageException;
import org.cubeengine.service.command.exception.PermissionDeniedException;
import org.joda.time.Duration;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.permission.PermissionDescription;

/**
 * A Kit of Items a User can receive
 */
public class Kit
{
    private final Kits module;
    private String name;
    private List<ItemStack> items;

    private boolean giveKitOnFirstJoin;
    private int limitUsagePerPlayer;
    private long limitUsageDelay;
    private PermissionDescription permission;
    private String customMessage;
    private List<String> commands;

    public Kit(Kits module, final String name, boolean giveKitOnFirstJoin, int limitUsagePerPlayer,
               long limitUsageDelay, boolean usePermission, String customMessage, List<String> commands,
               List<ItemStack> items)
    {
        this.module = module;
        this.name = name;
        this.items = items;
        this.commands = commands;
        this.customMessage = customMessage;
        if (usePermission)
        {
            this.permission = module.getPermissionManager().register(module, name, "Permission for the kit: " + name, module.perms().KITS);
        }
        else
        {
            this.permission = null;
        }
        this.giveKitOnFirstJoin = giveKitOnFirstJoin;
        this.limitUsagePerPlayer = limitUsagePerPlayer;
        this.limitUsageDelay = limitUsageDelay;
    }

    public boolean give(Player user, boolean force)
    {
        return this.give(null, user, force);
    }

    public boolean give(CommandSource sender, Player player, boolean force)
    {
        if (!force && this.getPermission() != null)
        {
            if (!sender.hasPermission(getPermission().getId()))
            {
                throw new PermissionDeniedException(getPermission());
            }
        }
        if (!force)
        {
            if (limitUsagePerPlayer > 0)
            {
                boolean reached = true; // TODO lookup player custom data
                // >= this.limitUsagePerPlayer
                if (reached)
                {
                    throw new IncorrectUsageException(false, "Kit-limit reached.");
                }
            }
            if (limitUsageDelay != 0)
            {
                boolean reached = true; // TODO lookup player custom data
                // System.currentTimeMillis() - lastUsage < limitUsageDelay)
                if (reached)
                {
                    throw new IncorrectUsageException(false, "This kit isn't available at the moment. Try again later!");
                }
            }
        }
        items.forEach(i -> player.getInventory().offer(i)); // TODO checked if ok
        // TODO update player custom data
        this.executeCommands(player);

        return true;
    }

    public boolean isGiveKitOnFirstJoin()
    {
        return giveKitOnFirstJoin;
    }


    private void executeCommands(Player player)
    {
        if (this.commands != null && !this.commands.isEmpty())
        {
            for (String cmd : commands)
            {
                cmd = cmd.replace("{PLAYER}", player.getName());
                module.getCommandManager().runCommand(player, cmd);
            }
        }
    }

    public PermissionDescription getPermission()
    {
        return this.permission;
    }

    public String getCustomMessage()
    {
        return this.customMessage;
    }


    public void applyToConfig(KitConfiguration config)
    {
        config.customReceiveMsg = this.customMessage;
        config.giveOnFirstJoin = this.giveKitOnFirstJoin;
        config.kitCommands = this.commands;
        config.kitItems = this.items;
        config.kitName = this.name;
        config.limitUsage = this.limitUsagePerPlayer;
        config.limitUsageDelay = new Duration(this.limitUsageDelay);
        config.usePerm = this.permission != null;
    }

    public String getKitName()
    {
        return this.name;
    }
}
