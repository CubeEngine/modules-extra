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
package org.cubeengine.module.vigil;

import java.util.concurrent.ThreadFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.matcher.StringMatcher;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.bigdata.Bigdata;
import org.cubeengine.module.vigil.commands.LookupDataParser;
import org.cubeengine.module.vigil.commands.ReportParser;
import org.cubeengine.module.vigil.commands.VigilAdminCommands;
import org.cubeengine.module.vigil.commands.VigilCommands;
import org.cubeengine.module.vigil.commands.VigilLookupCommands;
import org.cubeengine.module.vigil.data.ImmutableLookupData;
import org.cubeengine.module.vigil.data.LookupData;
import org.cubeengine.module.vigil.data.LookupDataBuilder;
import org.cubeengine.module.vigil.report.Report;
import org.cubeengine.module.vigil.report.ReportManager;
import org.cubeengine.module.vigil.storage.QueryManager;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.plugin.PluginContainer;

import static org.cubeengine.module.bigdata.PluginBigdata.BIGDATA_ID;
import static org.cubeengine.module.bigdata.PluginBigdata.BIGDATA_VERSION;

@Singleton
@Module(dependencies = @Dependency(value = BIGDATA_ID, version = BIGDATA_VERSION))
public class Vigil
{
    @ModuleConfig private VigilConfig config;
    @Inject
    private EventManager em;
    private ThreadFactory tf;
    @Inject private Bigdata bd;
    @Inject private CommandManager cm;
    @Inject private I18n i18n;
    @Inject private StringMatcher sm;
    @Inject private PermissionManager pm;
    @Inject private PluginContainer plugin;
    @Inject private ModuleManager mm;

    private QueryManager qm;
    private ReportManager rm;

    @Listener
    public void onEnable(GameInitializationEvent event)
    {
        this.tf = mm.getThreadFactory(Vigil.class);
        rm = new ReportManager(this, em, i18n);
        qm = new QueryManager(tf, bd.getDatabase().getCollection("vigil"), rm, i18n, plugin);
        this.cm.getProviders().register(this, new LookupDataParser(i18n), LookupData.class);;
        this.cm.getProviders().register(this, new ReportParser(this), Report.class);
        VigilCommands vc = new VigilCommands(sm, i18n, cm);
        cm.addCommand(vc);
        vc.addCommand(new VigilAdminCommands(cm, i18n, this));
        cm.addCommands(vc, this, new VigilLookupCommands(this, cm, i18n, qm));

        em.registerListener(Vigil.class, new ToolListener(pm, qm));

        DataRegistration<LookupData, ImmutableLookupData> dr =
                DataRegistration.<LookupData, ImmutableLookupData>builder()
                        .dataClass(LookupData.class).immutableClass(ImmutableLookupData.class)
                        .builder(new LookupDataBuilder()).manipulatorId("vigil-lookup")
                        .dataName("CubeEngine vigil Lookup Data")
                        .buildAndRegister(plugin);



    }

    public VigilConfig getConfig()
    {
        return config;
    }

    public QueryManager getQueryManager()
    {
        return qm;
    }

    public ReportManager getReportManager()
    {
        return rm;
    }
}
