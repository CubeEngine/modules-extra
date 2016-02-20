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
package org.cubeengine.module.vigil.report.entity;

import java.util.List;
import org.cubeengine.module.vigil.Receiver;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.Report;
import org.spongepowered.api.event.entity.DestructEntityEvent;
/* TODO
death
-animal
-boss
-kill
-monster
-npc
-other
-pet
-player?

-hanging-break
-vehicle-break

 */
public class DeathReport extends EntityReport<DestructEntityEvent.Death>
{
    @Override
    public void showReport(List<Action> actions, Receiver receiver)
    {

    }

    @Override
    public boolean group(Object lookup, Action action, Action otherAction, Report otherReport)
    {
        return false;
    }

    @Override
    public void apply(Action action, boolean noOp)
    {

    }

    @Override
    public Action observe(DestructEntityEvent.Death event)
    {
        return null;
    }
}
