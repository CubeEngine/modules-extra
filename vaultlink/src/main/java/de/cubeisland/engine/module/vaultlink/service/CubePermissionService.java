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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.service.user.UserManager;
import de.cubeisland.engine.module.service.world.WorldManager;
import de.cubeisland.engine.module.vaultlink.Vaultlink;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class CubePermissionService extends Permission
{
    private final Vaultlink module;
    private AtomicReference<de.cubeisland.engine.core.module.service.Permission> backingService;
    private final WorldManager wm;
    private final UserManager um;

    public CubePermissionService(Vaultlink module, AtomicReference<de.cubeisland.engine.core.module.service.Permission> roles)
    {
        this.module = module;
        this.backingService = roles;
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
        return backingService.get() != null && backingService.get().isEnabled();
    }

    @Override
    public boolean hasSuperPermsCompat()
    {
        return true;
    }

    @Override
    public boolean playerHas(String worldName, String player, String permission)
    {
        User user = um.findExactUser(player);
        if (user == null)
        {
            return false;
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
        return world != null && backingService.get().has(world, user, permission);
    }

    @Override
    public boolean playerAdd(String worldName, String player, String permission)
    {
        User user = um.findExactUser(player);
        if (user == null)
        {
            return false;
        }
        World world;
        if (worldName == null)
        {
            world = user.getWorld();
        }
        else
        {
            world = wm.getWorld(worldName);
        }
        if (world == null)
        {
            return false;
        }
        return this.backingService.get().add(world, user, permission);
    }

    @Override
    public boolean playerAddTransient(Player player, String permission)
    {
        User user = um.getExactUser(player.getUniqueId());
        if (user == null)
        {
            return false;
        }
        return this.backingService.get().addTemporary(player.getWorld(), player, permission);
    }

    @Override
    public boolean playerRemove(String worldName, String player, String permission)
    {
        User user = um.findExactUser(player);
        if (user == null)
        {
            return false;
        }
        World world;
        if (worldName == null)
        {
            world = user.getWorld();
        }
        else
        {
            world = wm.getWorld(worldName);
        }
        if (world == null)
        {
            return false;
        }
        return this.backingService.get().remove(world, user, permission);
    }

    @Override
    public boolean playerRemoveTransient(Player player, String permission)
    {
        User user = um.getExactUser(player.getUniqueId());
        if (user == null)
        {
            return false;
        }
        return this.backingService.get().removeTemporary(player.getWorld(), player, permission);
    }

    @Override
    public boolean groupHas(String worldName, String group, String permission)
    {
        if (group == null)
        {
            this.module.getLog().warn(new IllegalArgumentException(), "The group name should never be null!");
            return false;
        }
        return this.backingService.get().has(wm.getWorld(worldName), group, permission);
    }

    @Override
    public boolean groupAdd(String worldName, String group, String permission)
    {
        if (group == null)
        {
            this.module.getLog().warn(new IllegalArgumentException(), "The group name should never be null!");
            return false;
        }
        return this.backingService.get().add(wm.getWorld(worldName), group, permission);
    }

    @Override
    public boolean groupRemove(String worldName, String group, String permission)
    {
        if (group == null)
        {
            this.module.getLog().warn(new IllegalArgumentException(), "The group name should never be null!");
            return false;
        }
        return this.backingService.get().remove(wm.getWorld(worldName), group, permission);
    }

    @Override
    public boolean playerInGroup(String worldName, String player, String group)
    {
        if (group == null)
        {
            this.module.getLog().warn(new IllegalArgumentException(), "The group name should never be null!");
            return false;
        }
        User user = um.findExactUser(player);
        if (user == null)
        {
            return false;
        }
        World world;
        if (worldName == null)
        {
            world = user.getWorld();
        }
        else
        {
            world = wm.getWorld(worldName);
        }
        return world != null && this.backingService.get().hasRole(world, user, group);
    }

    @Override
    public boolean playerAddGroup(String worldName, String player, String group)
    {
        if (group == null)
        {
            this.module.getLog().warn(new IllegalArgumentException(), "The group name should never be null!");
            return false;
        }
        User user = um.findExactUser(player);
        if (user == null)
        {
            return false;
        }
        World world;
        if (worldName == null)
        {
            world = user.getWorld();
        }
        else
        {
            world = wm.getWorld(worldName);
        }
        return world != null && this.backingService.get().addRole(world, user, group);
    }

    @Override
    public boolean playerRemoveGroup(String worldName, String player, String group)
    {
        if (group == null)
        {
            this.module.getLog().warn(new IllegalArgumentException(), "The group name should never be null!");
            return false;
        }
        User user = um.findExactUser(player);
        if (user == null)
        {
            return false;
        }
        World world;
        if (worldName == null)
        {
            world = user.getWorld();
        }
        else
        {
            world = wm.getWorld(worldName);
        }
        return world != null && this.backingService.get().removeRole(world, user, group);
    }

    @Override
    public String[] getPlayerGroups(String worldName, String player)
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
            world = wm.getWorld(worldName);
        }
        return world == null ? null : this.backingService.get().getRoles(world, user);
    }

    @Override
    public String getPrimaryGroup(String worldName, String player)
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
            world = wm.getWorld(worldName);
        }
        return world == null ? null : this.backingService.get().getDominantRole(world, user);
    }

    @Override
    public String[] getGroups()
    {
        Set<String> roles = new HashSet<>();
        roles.addAll(Arrays.asList(this.backingService.get().getRoles(null)));
        for (World world : wm.getWorlds())
        {
            roles.addAll(Arrays.asList(this.backingService.get().getRoles(world)));
        }
        return roles.toArray(new String[roles.size()]);
    }

    @Override
    public boolean hasGroupSupport()
    {
        return this.backingService.get().hasRoleSupport();
    }
}
