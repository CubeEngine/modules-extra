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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.module.module.kits.data.KitData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
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

    public boolean give(ServerPlayer user)
    {
        return this.give(null, user, true);
    }

    public boolean give(CommandCause sender, ServerPlayer player, boolean force)
    {
        items.forEach(i -> player.getInventory().offer(i.copy())); // TODO what if not enough place
        final Map<String, Long> timeData = player.get(KitData.TIME).orElse(new HashMap<>());
        final Map<String, Integer> timesData = player.get(KitData.TIMES).orElse(new HashMap<>());
        timeData.put(name, System.currentTimeMillis());
        timesData.compute(name, (k, v) -> v == null ? 1 : v + 1);
        player.offer(KitData.TIME, timeData);
        player.offer(KitData.TIMES, timesData);
        this.executeCommands(player);
        return true;
    }

    public boolean checkLimit(CommandCause sender, ServerPlayer player, boolean force)
    {
        if (!force)
        {
            if (limitUsagePerPlayer > 0)
            {
                boolean reached = limitUsagePerPlayer <= player.get(KitData.TIMES).map(m -> m.get(this.name)).orElse(0);
                if (reached)
                {
                    module.getI18n().send(sender.getAudience(), MessageType.NEGATIVE, "Kit limit reached.");
                    return true;
                }
            }
            if (limitUsageDelay > 0)
            {
                boolean inDelay = limitUsageDelay >= System.currentTimeMillis() - player.get(KitData.TIME).map(m -> m.get(this.name)).orElse(0L);
                if (inDelay)
                {
                    module.getI18n().send(sender.getAudience(), MessageType.NEGATIVE, "This kit isn't available at the moment. Try again later!");
                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkPerm(CommandCause sender, boolean force)
    {
        if (!force && this.getPermission() != null)
        {
            if (!sender.hasPermission(getPermission().getId()))
            {
                module.getI18n().send(sender.getAudience(), MessageType.NEGATIVE, "You do not have the permission {name} to grant this kit.", getPermission().getId());
                return true;
            }
        }
        return false;
    }

    public boolean isGiveKitOnFirstJoin()
    {
        return giveKitOnFirstJoin;
    }

    public void setGiveKitOnFirstJoin(boolean giveKitOnFirstJoin)
    {
        this.giveKitOnFirstJoin = giveKitOnFirstJoin;
    }

    private void executeCommands(ServerPlayer player)
    {
        if (this.commands != null && !this.commands.isEmpty())
        {
            for (String cmd : commands)
            {
                cmd = cmd.replace("{PLAYER}", player.getName());
                try
                {
                    Sponge.getCommandManager().process(player, cmd);
                }
                catch (CommandException e)
                {
                    throw new IllegalStateException("Error while running " + cmd, e) ;
                }
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
        config.limitUsageDelay = java.time.Duration.ofMillis(this.limitUsageDelay);
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

    public void setCustomMessage(String customMessage)
    {
        this.customMessage = customMessage;
    }

    public void setPermission(boolean usePermission)
    {
        if (usePermission)
        {
            this.permission = module.getPermissionManager().register(Kits.class, name, "Permission for the kit: " + name, module.perms().KITS);
        }
        else
        {
            this.permission = null;
        }
    }

    public void setLimitUsage(int limitUsage)
    {
        this.limitUsagePerPlayer = limitUsage;
    }

    public void setUsageDelay(long usageDelay)
    {
        this.limitUsageDelay = usageDelay;
    }

    public List<String> getCommands()
    {
        return this.commands;
    }
}
