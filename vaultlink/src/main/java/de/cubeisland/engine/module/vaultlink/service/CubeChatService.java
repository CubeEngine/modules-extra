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
package de.cubeisland.engine.module.vaultlink.service;

import java.util.concurrent.atomic.AtomicReference;
import de.cubeisland.engine.module.service.Metadata;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.service.user.UserManager;
import de.cubeisland.engine.module.service.world.WorldManager;
import de.cubeisland.engine.module.vaultlink.Vaultlink;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.World;

public class CubeChatService extends Chat
{
    private final Vaultlink module;
    private final UserManager um;
    private final AtomicReference<Metadata> backingService;
    private final WorldManager wm;

    public CubeChatService(Vaultlink module, AtomicReference<Metadata> metadata, Permission perms)
    {
        super(perms);
        this.module = module;
        this.backingService = metadata;
        this.wm = module.getCore().getWorldManager();
        this.um = module.getCore().getUserManager();
    }

    @Override
    public String getName()
    {
        if (backingService.get() == null)
        {
            return "CubeEngine:Vaultlink";
        }
        return backingService.get().getName();
    }

    @Override
    public boolean isEnabled()
    {
        return backingService.get() != null;
    }

    private String getUserMetadata(String worldName, String player, String key)
    {
        User user = um.findExactUser(player);
        if (user == null)
        {
            return null;
        }
        World world;
        if (worldName == null)
        {
            world = user.getWorld();
        }
        else
        {
            world = this.wm.getWorld(worldName);
        }
        if (world == null)
        {
            return null;
        }
        return backingService.get().getMetadata(user, world, key);
    }

    private String getRoleMetadata(String worldName, String group, String key)
    {
        World world = null;
        if (worldName != null)
        {
            world = this.wm.getWorld(worldName);
            if (world == null)
            {
                return null;
            }

        }
        return backingService.get().getRoleMetadata(group, world, key);
    }

    @Override
    public String getPlayerPrefix(String world, String player)
    {
        return getPlayerInfoString(world, player, "prefix", "");
    }

    @Override
    public void setPlayerPrefix(String world, String player, String prefix)
    {
        setPlayerInfoString(world, player, "prefix", prefix);
    }

    @Override
    public String getPlayerSuffix(String world, String player)
    {
        return getPlayerInfoString(world, player, "suffix", "");
    }

    @Override
    public void setPlayerSuffix(String world, String player, String suffix)
    {
        setPlayerInfoString(world, player, "suffix", suffix);
    }

    @Override
    public String getGroupPrefix(String world, String group)
    {
        return getGroupInfoString(world, group, "prefix", "");
    }

    @Override
    public void setGroupPrefix(String world, String group, String prefix)
    {
        setGroupInfoString(world, group, "prefix", prefix);
    }

    @Override
    public String getGroupSuffix(String world, String group)
    {
        return getGroupInfoString(world, group, "suffix", "");
    }

    @Override
    public void setGroupSuffix(String world, String group, String suffix)
    {
        setGroupInfoString(world, group, "suffix", suffix);
    }

    @Override
    public int getPlayerInfoInteger(String world, String player, String node, int defaultValue)
    {
        String data = getUserMetadata(world, player, node);
        try
        {
            if (data != null)
            {
                return Integer.parseInt(data);
            }
        }
        catch (NumberFormatException ignore)
        {}
        return defaultValue;
    }

    @Override
    public void setPlayerInfoInteger(String world, String player, String node, int value)
    {
        setPlayerInfoString(world, player, node, String.valueOf(value));
    }

    @Override
    public int getGroupInfoInteger(String world, String group, String node, int defaultValue)
    {
        String data = getRoleMetadata(world, group, node);
        try
        {
            if (data != null)
            {
                return Integer.parseInt(data);
            }
        }
        catch (NumberFormatException ignore)
        {}
        return defaultValue;
    }

    @Override
    public void setGroupInfoInteger(String world, String group, String node, int value)
    {
        setGroupInfoString(world, group, node, String.valueOf(value));
    }

    @Override
    public double getPlayerInfoDouble(String world, String player, String node, double defaultValue)
    {
        String data = getUserMetadata(world, player, node);
        try
        {
            if (data != null)
            {
                return Double.parseDouble(data);
            }
        }
        catch (NumberFormatException ignore)
        {}
        return defaultValue;
    }

    @Override
    public void setPlayerInfoDouble(String world, String player, String node, double value)
    {
        setPlayerInfoString(world, player, node, String.valueOf(value));
    }

    @Override
    public double getGroupInfoDouble(String world, String group, String node, double defaultValue)
    {
        String data = getRoleMetadata(world, group, node);
        try
        {
            if (data != null)
            {
                return Double.parseDouble(data);
            }
        }
        catch (NumberFormatException ignore)
        {}
        return defaultValue;
    }

    @Override
    public void setGroupInfoDouble(String world, String group, String node, double value)
    {
        setGroupInfoString(world, group, node, String.valueOf(value));
    }

    @Override
    public boolean getPlayerInfoBoolean(String world, String player, String node, boolean defaultValue)
    {
        String data = getUserMetadata(world, player, node);
        try
        {
            if (data != null)
            {
                return Boolean.parseBoolean(data);
            }
        }
        catch (NumberFormatException ignore)
        {}
        return defaultValue;
    }

    @Override
    public void setPlayerInfoBoolean(String world, String player, String node, boolean value)
    {
        setPlayerInfoString(world, player, node, String.valueOf(value));
    }

    @Override
    public boolean getGroupInfoBoolean(String world, String group, String node, boolean defaultValue)
    {
        String data = getRoleMetadata(world, group, node);
        try
        {
            if (data != null)
            {
                return Boolean.parseBoolean(data);
            }
        }
        catch (NumberFormatException ignore)
        {}
        return defaultValue;
    }

    @Override
    public void setGroupInfoBoolean(String world, String group, String node, boolean value)
    {
        setGroupInfoString(world, group, node, String.valueOf(value));
    }

    @Override
    public String getPlayerInfoString(String world, String player, String node, String defaultValue)
    {
        String data = getUserMetadata(world, player, node);
        if (data == null)
        {
            data = defaultValue;
        }
        return data;
    }

    @Override
    public void setPlayerInfoString(String worldName, String player, String node, String value)
    {
        User user = um.findExactUser(player);
        if (user == null)
        {
            return;
        }
        World world = this.module.getCore().getWorldManager().getWorld(worldName);
        if (world == null)
        {
            return;
        }
        backingService.get().setMetadata(user, world, node, value);
    }

    @Override
    public String getGroupInfoString(String world, String group, String node, String defaultValue)
    {
        String data = getRoleMetadata(world, group, node);
        if (data == null)
        {
            data = defaultValue;
        }
        return data;
    }

    @Override
    public void setGroupInfoString(String worldName, String group, String node, String value)
    {
        World world = this.module.getCore().getWorldManager().getWorld(worldName);
        if (world == null)
        {
            return;
        }
        backingService.get().setRoleMetadata(group, world, node, value);
    }
}
