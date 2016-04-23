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
package org.cubeengine.module.spawn;

import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.event.EventManager;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.option.OptionSubjectData;

@ModuleInfo(name = "spawn", description ="Modifies the default spawn behaviour")
public class Spawn extends Module
{
    @Inject private EventManager em;
    @Inject private CommandManager cm;
    @Inject private PermissionService pm;
    @Inject private I18n i18n;

    @Enable
    public void onEnable()
    {
        em.registerListener(this, new SpawnListener(pm));
        cm.removeCommand("spawn", true); // unregister basics commands
        cm.addCommands(cm, this, new SpawnCommands(this, i18n, pm));

        if (!(pm.getDefaultData() instanceof OptionSubjectData))
        {
            throw new IllegalStateException("Module cannot be used without OptionSubjects");
        }
    }
}
