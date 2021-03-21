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
package org.cubeengine.module.vigil.commands;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.libcube.util.ConfirmManager.requestConfirmation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.audience.Audience;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Parser;
import org.cubeengine.libcube.service.command.annotation.Using;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.vigil.Vigil;
import org.cubeengine.module.vigil.report.Report;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Singleton
@Command(name = "admin", desc = "Vigil-Admin Commands")
@Using(ReportParser.class)
public class VigilAdminCommands extends DispatcherCommand
{
    private I18n i18n;
    private Vigil module;

    @Inject
    public VigilAdminCommands(I18n i18n, Vigil module)
    {
        this.i18n = i18n;
        this.module = module;
    }

    @Command(desc = "purges all logs")
    public void purge(CommandCause ctx)
    {
        requestConfirmation(i18n, i18n.translate(ctx, NEUTRAL, "Do you really want do delete ALL logs?"), ctx.audience(), () -> runPurge(ctx.audience()));
    }

    @Command(desc = "enables or disables reports in a world")
    public void setReportActive(CommandCause ctx, ServerWorld world, @Parser(completer = ReportParser.class) String name, @Default boolean enable) // TODO completer
    {
        Class<? extends Report> report = "*".equals(name) ? Report.class : Report.getReport(name).orElse(null);
        if (report == null)
        {
            i18n.send(ctx, NEGATIVE, "Unknown Report: {name}", name); // TODO suggest?
            return;
        }
        List<String> list = module.getConfig().disabledReports.computeIfAbsent(new ConfigWorld(world), w -> new ArrayList<>());
        if (enable)
        {
            i18n.send(ctx, POSITIVE, "Report {name} is now enabled in {world}", name, world);
            list.remove(name);
        }
        else
        {
            i18n.send(ctx, POSITIVE, "Report {name} is now disabled in {world}", name, world);
            list.add(name);
        }
        module.getConfig().markDirty(world);
        module.getConfig().save();
    }

    private void runPurge(Audience ctx)
    {
        CompletableFuture.runAsync(() -> module.getQueryManager().purge()).thenRun(() -> i18n.send(ctx, POSITIVE, "Purged all logs from database!"));
    }
}
