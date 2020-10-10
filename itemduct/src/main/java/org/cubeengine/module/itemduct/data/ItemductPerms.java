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
package org.cubeengine.module.itemduct.data;

import com.google.inject.Inject;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.itemduct.Itemduct;

public class ItemductPerms extends PermissionContainer {

    @Inject
    public ItemductPerms(PermissionManager pm) {
        super(pm, Itemduct.class);
    }

    public final Permission ACTIVATE_PISTON = register("activate.piston", "Allows activating ItemDuct Piston Endpoints", null);
    public final Permission ACTIVATE_OBSERVER = register("activate.observer", "Allows activating ItemDuct Observer Endpoints", null);
    public final Permission FILTER_VIEW = register("filter.view", "Allows viewing ItemDuct Filters", null);
    public final Permission FILTER_EDIT = register("filter.edit", "Allows editing ItemDuct Filters", null);
}
