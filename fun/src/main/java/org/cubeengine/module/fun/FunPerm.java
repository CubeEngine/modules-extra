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
package org.cubeengine.module.fun;

import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;

import javax.inject.Inject;

@SuppressWarnings("all")
public class FunPerm extends PermissionContainer
{
    @Inject
    public FunPerm(PermissionManager pm, Fun module)
    {
        super(pm, Fun.class);
    }

    private final Permission COMMAND = register("command", "");

    private final Permission COMMAND_EXPLOSION = register("explosion", "", COMMAND);
    public final Permission COMMAND_EXPLOSION_OTHER = register("other", "", COMMAND_EXPLOSION);
    public final Permission COMMAND_EXPLOSION_PLAYER_DAMAGE = register("player.damage", "", COMMAND_EXPLOSION);
    public final Permission COMMAND_EXPLOSION_BLOCK_DAMAGE = register("block.damage", "", COMMAND_EXPLOSION);
    public final Permission COMMAND_EXPLOSION_FIRE = register("fire", "", COMMAND_EXPLOSION);

    private final Permission COMMAND_HAT = register("hat", "", COMMAND);
    public final Permission COMMAND_HAT_OTHER = register("other", "", COMMAND_HAT);
    public final Permission COMMAND_HAT_ITEM = register("item", "", COMMAND_HAT);
    public final Permission COMMAND_HAT_MORE_ARMOR = register("more-armor", "", COMMAND_HAT);
    public final Permission COMMAND_HAT_QUIET = register("quit", "", COMMAND_HAT);
    public final Permission COMMAND_HAT_NOTIFY = register("notify", "", COMMAND_HAT);

    private final Permission COMMAND_LIGHTNING = register("lightning", "", COMMAND);
    public final Permission COMMAND_LIGHTNING_PLAYER_DAMAGE = register("player.damage", "", COMMAND_LIGHTNING);
    public final Permission COMMAND_LIGHTNING_UNSAFE = register("unsafe", "", COMMAND_LIGHTNING);

    public final Permission COMMAND_THROW = register("throw", "", COMMAND);
    public final Permission COMMAND_THROW_UNSAFE = register("unsafe", "", COMMAND_THROW);

    private final Permission COMMAND_NUKE = register("nuke", "", COMMAND);
    public final Permission COMMAND_NUKE_CHANGE_RANGE = register("change-range", "", COMMAND_NUKE);
    public final Permission COMMAND_NUKE_OTHER = register("other", "", COMMAND_NUKE);
}
