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
package org.cubeengine.module.powertools;

import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.matcher.MaterialMatcher;
import org.cubeengine.module.powertools.data.IPowertoolData;
import org.cubeengine.module.powertools.data.ImmutablePowertoolData;
import org.cubeengine.module.powertools.data.PowertoolData;
import org.cubeengine.module.powertools.data.PowertoolDataBuilder;
import org.cubeengine.processor.Module;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.plugin.PluginContainer;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Module
public class Powertools extends CubeEngineModule
{
    @Inject private PowertoolsPerm perm;

    @Inject private CommandManager cm;
    @Inject private EventManager em;
    @Inject private MaterialMatcher materialMatcher;
    @Inject private I18n i18n;

    @Inject private PluginContainer plugin;

    @Listener
    public void onEnable(GamePreInitializationEvent event)
    {
        DataRegistration.<PowertoolData, ImmutablePowertoolData>builder()
                .dataClass(PowertoolData.class).immutableClass(ImmutablePowertoolData.class)
                .builder(new PowertoolDataBuilder()).manipulatorId("powertools")
                .dataName("CubeEngine PowerTools Data")
                .buildAndRegister(plugin);

        IPowertoolData.POWERS.getQuery();

        PowertoolCommand ptCommands = new PowertoolCommand(cm, this, materialMatcher, i18n);
        cm.addCommand(ptCommands);
        em.registerListener(Powertools.class, ptCommands);
    }

    public PowertoolsPerm perms()
    {
        return perm;
    }
}
