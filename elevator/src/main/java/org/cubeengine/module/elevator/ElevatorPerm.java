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
package org.cubeengine.module.elevator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;

@Singleton
public class ElevatorPerm extends PermissionContainer
{

    @Inject
    public ElevatorPerm(PermissionManager pm)
    {
        super(pm, Elevator.class);
    }

    public final Permission USE = register("use", "Allows using elevators");
    public final Permission ADJUST = register("adjust", "Allows adjusting elevators");
    public final Permission CREATE = register("create", "Allows creating elevators");
    public final Permission RENAME = register("rename", "Allows renaming elevators");
}
