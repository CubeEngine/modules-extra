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
package org.cubeengine.module.vigil.report.entity.player;

import java.util.List;
import org.cubeengine.module.vigil.Receiver;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.BaseReport;
import org.cubeengine.module.vigil.report.Observe;
import org.cubeengine.module.vigil.report.Recall;
import org.cubeengine.module.vigil.report.Report;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;

public class QuitReport extends BaseReport<ServerSideConnectionEvent.Disconnect> implements Report.Readonly, Report.ReportGrouping
{
    @Override
    protected Action observe(ServerSideConnectionEvent.Disconnect event)
    {
        Action action = newReport();
        action.addData(CAUSE, Observe.causes(Cause.of(EventContext.empty(), event.getPlayer())));
        action.addData(LOCATION, Observe.location(event.getPlayer().getServerLocation()));
        return action;
    }


    @Override
    public List<Class<? extends Report>> getReportsList() {
        return JoinReport.groupings;
    }

    @Listener
    public void onQuit(ServerSideConnectionEvent.Disconnect event)
    {
        report(observe(event));
    }

    @Override
    public void showReport(List<Action> actions, Receiver receiver)
    {
        Action action = actions.get(0);
        receiver.sendReport(this, actions, actions.size(), "{txt} left the game", "{txt} left the game x{}",
                            Recall.cause(action), actions.size());
    }
}
