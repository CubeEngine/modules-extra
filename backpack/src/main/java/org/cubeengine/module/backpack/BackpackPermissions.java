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
package org.cubeengine.module.backpack;

import javax.inject.Inject;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.spongepowered.api.service.permission.PermissionDescription;

@SuppressWarnings("all")
public class BackpackPermissions extends PermissionContainer
{
    @Inject
    public BackpackPermissions(PermissionManager pm)
    {
        super(pm, Backpack.class);
    }

    public final Permission COMMAND_OPEN_OTHER_PLAYER = register("command.backpack.open.other", "Allows using the open command as another player", null);
    public final Permission COMMAND_CREATE_OTHER = register("command.backpack.create.other", "Allows creating backpacks for other players", null);
    public final Permission COMMAND_CREATE_NAMED = register("command.backpack.create.named", "Allows creating backpacks with names", null);
}
