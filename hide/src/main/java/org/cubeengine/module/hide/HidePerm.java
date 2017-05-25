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
package org.cubeengine.module.hide;

import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;

@SuppressWarnings("all")
public class HidePerm extends PermissionContainer
{
    public HidePerm(PermissionManager pm)
    {
        super(pm, Hide.class);
    }

    private final Permission AUTO = register("auto", "");
    public final Permission AUTO_HIDE = register("hide", "", AUTO);
    public final Permission AUTO_SEEHIDDENS = register("see-hidden", "", AUTO);

    public final Permission INTERACT = register("interact", "");
    public final Permission PICKUP = register("pickup", "");
    public final Permission DROP = register("drop", "");
    public final Permission CHAT = register("chat", "");
}
