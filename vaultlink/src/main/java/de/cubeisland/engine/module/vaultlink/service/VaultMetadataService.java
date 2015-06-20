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
import de.cubeisland.engine.service.Metadata;
import de.cubeisland.engine.service.user.User;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.World;

public class VaultMetadataService implements Metadata
{
    private AtomicReference<Chat> vaultChat;

    public VaultMetadataService(AtomicReference<Chat> vaultChat)
    {
        this.vaultChat = vaultChat;
    }

    @Override
    public String setMetadata(User user, String key, String value)
    {
        String old = vaultChat.get().getPlayerInfoString(user, key, null);
        vaultChat.get().setPlayerInfoString(user, key, value);
        return old;
    }

    @Override
    public String getMetadata(User user, String key)
    {
        return vaultChat.get().getPlayerInfoString(user, key, null);
    }

    @Override
    public String setMetadata(User user, World world, String key, String value)
    {
        String old = this.getMetadata(user, key);
        vaultChat.get().setPlayerInfoString(world.getName(), user, key, value);
        return old;
    }

    @Override
    public String getMetadata(User user, World world, String key)
    {
        return vaultChat.get().getPlayerInfoString(world.getName(), user, key, null);
    }

    @Override
    public String getRoleMetadata(String role, World world, String key)
    {
        return vaultChat.get().getGroupInfoString(world == null ? null : world.getName(), role, key, null);
    }

    @Override
    public String setRoleMetadata(String role, World world, String key, String value)
    {
        String old = this.getRoleMetadata(role, world, key);
        vaultChat.get().setGroupInfoString(world == null ? null : world.getName(), role, key, value);
        return old;
    }

    @Override
    public String getName()
    {
        return vaultChat.get().getName();
    }
}
