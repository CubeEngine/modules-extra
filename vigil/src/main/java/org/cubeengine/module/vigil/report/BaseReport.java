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
package org.cubeengine.module.vigil.report;

import org.cubeengine.module.vigil.Vigil;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.world.World;

public abstract class BaseReport<T extends Event> implements Report
{
    protected Vigil vigil;

    public void init(Vigil vigil)
    {
        this.vigil = vigil;
    }

    protected Action newReport()
    {
        return new Action(getClass().getName());
    }

    protected void report(Action action)
    {
        if (action != null)
        {
            vigil.getQueryManager().report(action);
        }
    }

    /**
     * Observes an event an creates an action for it
     *
     * @param event the event to observe
     * @return the events action
     */
    protected abstract Action observe(T event);

    protected boolean isActive(World world)
    {
        for (Class<? extends Report> disabled : vigil.getConfig().getDisabledReports(world))
        {
            if (disabled.isAssignableFrom(this.getClass()))
            {
                return false;
            }
        }
        return true;
    }
}
