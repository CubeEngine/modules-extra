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
import de.cubeisland.engine.modularity.core.Module;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.event.EventManager;

public class Spawn extends Module
{
    private SpawnConfig config;
    @Inject private EventManager em;
    @Inject private CommandManager cm;
    private SpawnPerms perms;

    @Override
    public void onEnable()
    {
        this.config = this.loadConfig(SpawnConfig.class);
        em.registerListener(this, new SpawnListener());
        cm.removeCommand("spawn", true); // unregister basics commands
        cm.addCommands(cm, this, new SpawnCommands(this));
        perms = new SpawnPerms(this); // PermContainer registers itself

        // TODO per world spawn with rotation
    }

    @Override
    public void onDisable()
    {
        // TODO if not complete shutdown reregister basics commands OR do not unregister simply override (let CommandManager handle it)
    }

    public SpawnConfig getConfiguration()
    {
        return config;
    }

    public SpawnPerms perms()
    {
        return this.perms;
    }
}
