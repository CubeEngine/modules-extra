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
package de.cubeisland.engine.module.vaultlink;

import java.util.concurrent.atomic.AtomicReference;
import de.cubeisland.engine.service.command.CommandSender;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.spongepowered.api.entity.player.Player;

public class VaultPermissionService implements de.cubeisland.engine.core.module.service.Permission
{
    private AtomicReference<Permission> vaultPermRef;

    public VaultPermissionService(AtomicReference<Permission> vaultPermRef)
    {
        this.vaultPermRef = vaultPermRef;
    }

    @Override
    public String getName()
    {
        return vaultPermRef.get().getName();
    }

    @Override
    public boolean isEnabled()
    {
        return vaultPermRef.get().isEnabled();
    }

    @Override
    public boolean hasSuperPermsCompat()
    {
        return vaultPermRef.get().hasSuperPermsCompat();
    }

    @Override
    public boolean has(World world, OfflinePlayer player, String permission)
    {
        return vaultPermRef.get().playerHas(world == null ? null : world.getName(), player, permission);
    }

    @Override
    public boolean has(CommandSender sender, String permission)
    {
        return vaultPermRef.get().has(sender, permission);
    }

    @Override
    public boolean add(World world, OfflinePlayer player, String permission)
    {
        return vaultPermRef.get().playerAdd(world == null ? null : world.getName(), player, permission);
    }

    @Override
    public boolean addTemporary(World world, Player player, String permission)
    {
        return vaultPermRef.get().playerAddTransient(world == null ? null : world.getName(), player, permission);
    }

    @Override
    public boolean remove(World world, OfflinePlayer player, String permission)
    {
        return vaultPermRef.get().playerRemove(world == null ? null : world.getName(), player, permission);
    }

    @Override
    public boolean removeTemporary(World world, OfflinePlayer player, String permission)
    {
        return vaultPermRef.get().playerRemoveTransient(world == null ? null : world.getName(), player, permission);
    }

    @Override
    public boolean has(World world, String role, String permission)
    {
        return vaultPermRef.get().groupHas(world, role, permission);
    }

    @Override
    public boolean add(World world, String role, String permission)
    {
        return vaultPermRef.get().groupAdd(world, role, permission);
    }

    @Override
    public boolean remove(World world, String role, String permission)
    {
        return vaultPermRef.get().groupRemove(world, role, permission);
    }

    @Override
    public boolean hasRole(World world, OfflinePlayer player, String role)
    {
        return vaultPermRef.get().playerInGroup(world == null ? null : world.getName(), player, role);
    }

    @Override
    public boolean addRole(World world, OfflinePlayer player, String role)
    {
        return vaultPermRef.get().playerAddGroup(world == null ? null : world.getName(), player, role);
    }

    @Override
    public boolean removeRole(World world, OfflinePlayer player, String role)
    {
        return vaultPermRef.get().playerRemoveGroup(world == null ? null : world.getName(), player, role);
    }

    @Override
    public String[] getRoles(World world, OfflinePlayer player)
    {
        return vaultPermRef.get().getPlayerGroups(world == null ? null : world.getName(), player);
    }

    @Override
    public String getDominantRole(World world, OfflinePlayer player)
    {
        return vaultPermRef.get().getPrimaryGroup(world == null ? null : world.getName(), player);
    }

    @Override
    public boolean hasRoleSupport()
    {
        return vaultPermRef.get().hasGroupSupport();
    }

    @Override
    public String[] getRoles(World world)
    {
        if (world == null)
        {
            return vaultPermRef.get().getGroups();
        }
        throw new UnsupportedOperationException("Vault does not support listing roles for a world");
    }

    @Override
    public String[] getGlobalRoles()
    {
        throw new UnsupportedOperationException("Vault does not support listing roles for a world");
    }
}
