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
import de.cubeisland.engine.modularity.core.marker.Disable;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import org.cubeengine.module.core.sponge.EventManager;
import org.cubeengine.module.core.util.matcher.MaterialMatcher;
import org.cubeengine.service.command.CommandManager;

@ModuleInfo(name = "Powertools", description = "Empower your tools")
public class Powertools extends Module
{
    private PowertoolsPerm perm;

    @Inject private CommandManager cm;
    @Inject private EventManager em;
    @Inject private MaterialMatcher materialMatcher;

    @Enable
    public void onEnable()
    {
        this.perm = new PowertoolsPerm(this);
        PowerToolCommand ptCommands = new PowerToolCommand(this, materialMatcher);
        cm.addCommand(ptCommands);
        em.registerListener(this, ptCommands);
    }

    @Disable
    public void onDisable()
    {
        cm.removeCommands(this);
        em.removeListeners(this);
    }

    public PowertoolsPerm perms()
    {
        return perm;
    }
}
