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

import org.cubeengine.module.vigil.Receiver;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.event.Event;

import java.util.List;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;

public class UnkownReport<T extends Event> implements Report<T>
{
    private I18n i18n;
    private String type;

    public UnkownReport(I18n i18n, String type)
    {
        this.i18n = i18n;
        this.type = type;
    }

    @Override
    public void showReport(List<Action> actions, Receiver receiver)
    {
        receiver.sendReport(actions, actions.size(),
                "Unknown Report {input}","Unknown Report {input} {size}",
                type, actions.size());
    }

    @Override
    public boolean group(Object lookup, Action action, Action otherAction, Report otherReport)
    {
        return this.equals(otherReport);
    }

    @Override
    public void apply(Action action, boolean rollback)
    {
    }

    @Override
    public Action observe(Event event)
    {
        return null;
    }
}
