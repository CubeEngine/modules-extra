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
package org.cubeengine.module.vigil;

import java.util.concurrent.ThreadFactory;
import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.module.bigdata.Bigdata;
import org.cubeengine.module.core.sponge.EventManager;
import org.cubeengine.module.core.util.matcher.StringMatcher;
import org.cubeengine.module.vigil.commands.VigilCommands;
import org.cubeengine.module.vigil.report.ReportManager;
import org.cubeengine.module.vigil.report.block.BlockReport;
import org.cubeengine.module.vigil.storage.QueryManager;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.permission.PermissionManager;
import org.spongepowered.api.Game;

@ModuleInfo(name = "Vigil", description = "Keeps a vigilant eye on your server")
public class Vigil extends Module
{
    @Inject private Game game;
    @Inject private EventManager em;
    @Inject private ThreadFactory tf;
    @Inject private Bigdata bd;
    @Inject private CommandManager cm;
    @Inject private I18n i18n;
    @Inject private StringMatcher sm;
    @Inject private PermissionManager pm;

    private QueryManager qm;

    public Game getGame()
    {
        return game;
    }

    @Enable
    public void onEnable()
    {
        ReportManager reportManager = new ReportManager(this, em);
        qm = new QueryManager(tf, bd.getDatabase().getCollection("vigil"), reportManager);
        cm.addCommands(this, new VigilCommands(sm, i18n, game));
        em.registerListener(this, new ToolListener(this, i18n, pm, qm, game));
    }

    public QueryManager getQueryManager()
    {
        return qm;
    }
}
