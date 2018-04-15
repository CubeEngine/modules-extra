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

import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.parameter.argument.ArgumentParser;
import org.cubeengine.butler.parameter.argument.Completer;
import org.cubeengine.butler.parameter.argument.ParserException;
import org.cubeengine.module.vigil.Vigil;
import org.cubeengine.module.vigil.report.Report;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReportParser implements ArgumentParser<Report>, Completer {

    public static final String REPORT_BASE = "org.cubeengine.module.vigil.report";
    private Vigil module;

    public ReportParser(Vigil module)
    {
        this.module = module;
    }

    @Override
    public Report parse(Class type, CommandInvocation invocation) throws ParserException
    {
        String token = invocation.consume(1);
        Optional<? extends Class<? extends Report>> reportClass = Report.getReport(token);
        if (reportClass.isPresent()) {
            Report report = this.module.getReportManager().getReports().get(reportClass.get().getName());
            return report;
        }
        throw new ParserException("Could not find Report named: " + token);
    }

    @Override
    public List<String> suggest(Class type, CommandInvocation invocation)
    {
        List<String> list = new ArrayList<>();
        String token = invocation.currentToken();
        for (String report : this.module.getReportManager().getReports().keySet())
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
}
