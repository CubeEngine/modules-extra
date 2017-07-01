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
package org.cubeengine.module.spawn;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.InjectService;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.service.permission.PermissionService;

// TODO integrate in teleport module reading subject option is not that advanced of a feature?
@Singleton
@Module(id = "spawn", name = "Spawn", version = "1.0.0",
        description = "Modifies the default spawn behaviour",
        dependencies = @Dependency("cubeengine-core"),
        url = "http://cubeengine.org",
        authors = {"Anselm 'Faithcaio' Brehme", "Phillip Schichtel"})
public class Spawn extends CubeEngineModule
{
    @Inject private EventManager em;
    @Inject private CommandManager cm;
    @InjectService private PermissionService pm;
    @Inject private I18n i18n;

    @Listener
    public void onEnable(GamePostInitializationEvent event)
    {
        em.registerListener(Spawn.class, new SpawnListener(pm));
        cm.removeCommand("spawn", true); // unregister basics commands
        cm.addCommands(cm, this, new SpawnCommands(this, i18n, pm));
    }
}
