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

    public final Permission COMMAND_EXPLOSION_OTHER = register("command.explosion.other", "", null);
    public final Permission COMMAND_EXPLOSION_PLAYER_DAMAGE = register("command.explosion.player.damage", "", null);
    public final Permission COMMAND_EXPLOSION_BLOCK_DAMAGE = register("command.explosion.block.damage", "", null);
    public final Permission COMMAND_EXPLOSION_FIRE = register("command.explosion.fire", "", null);

    public final Permission COMMAND_HAT_OTHER = register("command.hat.other", "", null);
    public final Permission COMMAND_HAT_ITEM = register("command.hat.item", "", null);
    public final Permission COMMAND_HAT_MORE_ARMOR = register("command.hat.more-armor", "", null);
    public final Permission COMMAND_HAT_QUIET = register("command.hat.quit", "", null);
    public final Permission COMMAND_HAT_NOTIFY = register("command.hat.notify", "", null);

    public final Permission COMMAND_LIGHTNING_PLAYER_DAMAGE = register("command.lightning.player-damage", "", null);
    public final Permission COMMAND_LIGHTNING_UNSAFE = register("command.lightning.unsafe", "", null);

    public final Permission COMMAND_THROW_UNSAFE = register("command.throw.unsafe", "", null);

    public final Permission COMMAND_NUKE_CHANGE_RANGE = register("command.nuke.change-range", "", null);
    public final Permission COMMAND_NUKE_OTHER = register("command.nuke.other", "", null);
}
