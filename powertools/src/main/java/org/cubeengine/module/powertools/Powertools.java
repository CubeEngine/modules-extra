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
import org.cubeengine.module.powertools.data.ImmutablePowertoolData;
import org.cubeengine.module.powertools.data.PowertoolData;
import org.cubeengine.module.powertools.data.PowertoolDataBuilder;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.matcher.MaterialMatcher;
import org.spongepowered.api.Sponge;

@ModuleInfo(name = "Powertools", description = "Empower your tools")
public class Powertools extends Module
{
    @Inject private PowertoolsPerm perm;

    @Inject private CommandManager cm;
    @Inject private EventManager em;
    @Inject private MaterialMatcher materialMatcher;
    @Inject private I18n i18n;

    @Enable
    public void onEnable()
    {
        Sponge.getDataManager().register(PowertoolData.class, ImmutablePowertoolData.class, new PowertoolDataBuilder());
        PowertoolCommand ptCommands = new PowertoolCommand(this, materialMatcher, i18n);
        cm.addCommand(ptCommands);
        em.registerListener(Powertools.class, ptCommands);
    }

    public PowertoolsPerm perms()
    {
        return perm;
    }
}
