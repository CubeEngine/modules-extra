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
package org.cubeengine.module.squelch;

import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;

import javax.inject.Inject;

@SuppressWarnings("all")
public class SquelchPerm extends PermissionContainer
{
    @Inject
    public SquelchPerm(PermissionManager pm)
    {
        super(pm, Squelch.class);
    }

    private final Permission COMMAND = register("command", "Base Commands Permission", null);
    public final Permission COMMAND_IGNORE_PREVENT = register("ignore.prevent", "Prevents adding the player with this permission to an ignore-list", COMMAND);
}
