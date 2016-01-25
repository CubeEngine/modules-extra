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
package org.cubeisland.module.fun;

import org.cubeengine.service.command.CommandManager;
import de.cubeisland.engine.module.core.module.Module;
import org.cubeisland.module.fun.commands.DiscoCommand;
import org.cubeisland.module.fun.commands.InvasionCommand;
import org.cubeisland.module.fun.commands.NukeCommand;
import org.cubeisland.module.fun.commands.PlayerCommands;
import org.cubeisland.module.fun.commands.RocketCommand;
import org.cubeisland.module.fun.commands.ThrowCommands;

public class Fun extends Module
{
    private FunConfiguration config;
    private FunPerm perms;

    @Override
    public void onEnable()
    {
        this.config = this.loadConfig(FunConfiguration.class);
        // this.getCore().getFileManager().dropResources(FunResource.values());
        perms = new FunPerm(this);

        final CommandManager cm = this.getCore().getCommandManager();
        cm.addCommands(cm, this, new ThrowCommands(this));
        cm.addCommands(cm, this, new NukeCommand(this));
        cm.addCommands(cm, this, new PlayerCommands(this));
        cm.addCommands(cm, this, new DiscoCommand(this));
        cm.addCommands(cm, this, new InvasionCommand(this));
        cm.addCommands(cm, this, new RocketCommand(this));
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
