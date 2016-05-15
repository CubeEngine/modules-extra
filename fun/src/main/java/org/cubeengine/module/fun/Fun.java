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
package org.cubeengine.module.fun;

import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.module.fun.commands.DiscoCommand;
import org.cubeengine.module.fun.commands.ThrowCommands;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.module.fun.commands.InvasionCommand;
import org.cubeengine.module.fun.commands.NukeCommand;
import org.cubeengine.module.fun.commands.PlayerCommands;
import org.cubeengine.module.fun.commands.RocketCommand;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.matcher.EntityMatcher;
import org.cubeengine.libcube.service.task.TaskManager;

@ModuleInfo(name = "Fun", description = "A collection of fun commands")
public class Fun extends Module
{
    @ModuleConfig private FunConfiguration config;
    @Inject private FunPerm perms;

    @Inject private TaskManager tm;
    @Inject private I18n i18n;
    @Inject private CommandManager cm;
    @Inject private EventManager em;
    @Inject private EntityMatcher entityMatcher;

    @Enable
    public void onEnable()
    {
        cm.addCommands(this, new ThrowCommands(this));
        cm.addCommands(this, new NukeCommand(this, i18n, em));
        cm.addCommands(this, new PlayerCommands(this, em, i18n, tm));
        cm.addCommands(this, new DiscoCommand(this, i18n, tm));
        cm.addCommands(this, new InvasionCommand(this, i18n, entityMatcher));
        cm.addCommands(this, new RocketCommand(this, em, tm));
    }

    public FunConfiguration getConfig()
    {
        return this.config;
    }

    public FunPerm perms()
    {
        return perms;
    }
}
