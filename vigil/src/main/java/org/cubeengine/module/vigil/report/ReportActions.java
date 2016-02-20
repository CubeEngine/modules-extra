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
package org.cubeengine.module.vigil.report;

import java.util.LinkedList;
import org.cubeengine.module.vigil.Receiver;

public class ReportActions
{
    private final Report report;
    private final LinkedList<Action> actions = new LinkedList<>();

    public ReportActions(Report report)
    {
        this.report = report;
    }

    public boolean add(Action action, Report report, Object lookup)
    {
        if (actions.isEmpty() || this.report.group(lookup, actions.getLast(), action, report))
        {
            actions.add(action);
            return true;
        }
        return false;
    }

    public void showReport(Receiver receiver)
    {
        report.showReport(actions, receiver);
    }

}
