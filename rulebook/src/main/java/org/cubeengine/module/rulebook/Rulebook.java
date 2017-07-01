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
package org.cubeengine.module.rulebook;

import java.nio.file.Path;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.rulebook.bookManagement.RulebookCommands;
import org.cubeengine.module.rulebook.bookManagement.RulebookManager;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;

@Singleton
@Module
public class Rulebook extends CubeEngineModule
{
    private RulebookManager rulebookManager;

    @Inject private PermissionManager pm;
    @Inject private CommandManager cm;
    @Inject private EventManager em;
    @Inject private I18n i18n;
    @Inject private ModuleManager mm;
    private Path folder;

    @Listener
    public void onEnable(GamePreInitializationEvent event)
    {
        this.folder = mm.getPathFor(Rulebook.class);
        // this.getCore().getFileManager().dropResources(RulebookResource.values());
        Permission getOtherPerm = pm.register(Rulebook.class, "command.get.other", "Allows adding a rulebook to another players inventory", null);
        this.rulebookManager = new RulebookManager(this, i18n);

        cm.addCommand(new RulebookCommands(cm, this, getOtherPerm, i18n));
        em.registerListener(Rulebook.class, new RulebookListener(this, i18n));
    }

    public RulebookManager getRuleBookManager()
    {
        return this.rulebookManager;
    }

    public Path getFolder()
    {
        return folder;
    }
}
