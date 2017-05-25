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
package org.cubeengine.module.module.kits;

import java.util.List;
import java.util.Optional;

import org.cubeengine.butler.parameter.IncorrectUsageException;
import org.cubeengine.libcube.service.command.exception.PermissionDeniedException;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.module.module.kits.data.KitData;
import org.joda.time.Duration;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;

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
    private Permission permission;
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
            this.permission = module.getPermissionManager().register(Kits.class, name, "Permission for the kit: " + name, module.perms().KITS);
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
                boolean reached = false;
                Optional<KitData> kitData = player.get(KitData.class);
                if (kitData.isPresent())
                {
                    reached = limitUsagePerPlayer >= kitData.get().getTimes().getOrDefault(this.name, 0);
                }
                if (reached)
                {
                    throw new IncorrectUsageException(false, "Kit-limit reached.");
                }
            }
            if (limitUsageDelay > 0)
            {
                boolean inDelay = false;
                // System.currentTimeMillis() - lastUsage < limitUsageDelay)
                Optional<KitData> kitData = player.get(KitData.class);
                if (kitData.isPresent())
                {
                    inDelay = limitUsageDelay <= System.currentTimeMillis() - kitData.get().getTime().getOrDefault(this.name, System.currentTimeMillis());
                }
                if (inDelay)
                {
                   throw new IncorrectUsageException(false, "This kit isn't available at the moment. Try again later!");
                }
            }
        }
        items.forEach(i -> player.getInventory().offer(i.copy())); // TODO what if not enough place
        KitData kitData = player.get(KitData.class).orElse(null);
        if (kitData == null)
        {
            kitData = new KitData();
        }
        kitData.getTime().put(name, System.currentTimeMillis());
        kitData.getTimes().put(name, kitData.getTimes().getOrDefault(name, 0) + 1);
        player.offer(kitData);
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

    public Permission getPermission()
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

    public List<ItemStack> getItems()
    {
        return items;
    }

    public void clearCommands()
    {
        this.commands.clear();;
    }

    public void setCommands(List<String> commands)
    {
        this.commands = commands;
    }
}
