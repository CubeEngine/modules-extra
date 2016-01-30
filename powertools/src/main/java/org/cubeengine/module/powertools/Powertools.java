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
package org.cubeengine.module.powertools;

import javax.inject.Inject;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.event.EventManager;
import org.cubeengine.service.matcher.MaterialMatcher;
import org.cubeengine.service.permission.ModulePermissions;

@ModuleInfo(name = "Powertools", description = "Empower your tools")
public class Powertools extends Module
{
    @ModulePermissions private PowertoolsPerm perm;

    @Inject private CommandManager cm;
    @Inject private EventManager em;
    @Inject private MaterialMatcher materialMatcher;

    @Enable
    public void onEnable()
    {
        PowerToolCommand ptCommands = new PowerToolCommand(this, materialMatcher, i18n);
        cm.addCommand(ptCommands);
        em.registerListener(this, ptCommands);
    }

    public PowertoolsPerm perms()
    {
        return perm;
    }
}
