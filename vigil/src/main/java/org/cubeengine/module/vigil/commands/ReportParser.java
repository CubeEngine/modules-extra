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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.ParserFor;
import org.cubeengine.module.vigil.report.Report;
import org.cubeengine.module.vigil.report.ReportManager;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader.Mutable;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.CommandContext.Builder;
import org.spongepowered.api.command.parameter.Parameter.Key;
import org.spongepowered.api.command.parameter.managed.ValueCompleter;
import org.spongepowered.api.command.parameter.managed.ValueParser;

@ParserFor(Report.class)
@Singleton
public class ReportParser implements ValueParser<Report>, ValueCompleter
{

    public static final String REPORT_BASE = "org.cubeengine.module.vigil.report";
    private ReportManager manager;

    @Inject
    public ReportParser(ReportManager manager)
    {
        this.manager = manager;
    }

    @Override
    public List<String> complete(CommandContext context, String currentInput)
    {
        List<String> list = new ArrayList<>();
        String token = currentInput;
        for (String report : this.manager.getReports().keySet())
        {
            if (report.startsWith(REPORT_BASE))
            {
                report = report.substring(REPORT_BASE.length() + 1);
            }
            if (report.toLowerCase().startsWith(token.toLowerCase()))
            {
                list.add(report);
            }
        }
        return list;
    }

    @Override
    public Optional<? extends Report> parseValue(Key<? super Report> parameterKey, Mutable reader, Builder context) throws ArgumentParseException
    {
        final String token = reader.parseString();
        Optional<? extends Class<? extends Report>> reportClass = Report.getReport(token);
        if (reportClass.isPresent()) {
            Report report = this.manager.getReports().get(reportClass.get().getName());
            return Optional.of(report);
        }
        return Optional.empty();
        // TODO error msg throw new ParserException("Could not find Report named: " + token);
    }
}
