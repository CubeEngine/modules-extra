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
package org.cubeengine.module.module.kits;

import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.reflect.Reflector;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.database.ModuleTables;
import org.cubeengine.service.permission.ModulePermissions;

@ModuleInfo(name = "Kits", description = "Hand kits to your players")
@ModuleTables(TableKitsGiven.class)
public class Kits extends Module
{
    private KitManager kitManager;

    @ModulePermissions private KitsPerm perms;

    @Inject private CommandManager cm;

    @Inject
    public Kits(Reflector reflector)
    {
        reflector.getDefaultConverterManager().registerConverter(new KitItemConverter(), KitItem.class);
    }

    @Enable
    public void onEnable()
    {
        this.kitManager = new KitManager(this);
        this.kitManager.loadKits();
        cm.addCommand(new KitCommand(this));
    }

    public KitManager getKitManager()
    {
        return this.kitManager;
    }
    public KitsPerm perms()
    {
        return perms;
    }
}
