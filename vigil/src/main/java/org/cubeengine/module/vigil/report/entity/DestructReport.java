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
import java.util.Optional;
import org.cubeengine.module.vigil.Receiver;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.Observe;
import org.cubeengine.module.vigil.report.Recall;
import org.cubeengine.module.vigil.report.Report;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.value.mutable.MutableBoundedValue;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.text.Text;

import static java.util.stream.Collectors.toList;
import static org.cubeengine.module.vigil.report.ReportUtil.name;

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
public class DestructReport extends EntityReport<DestructEntityEvent>
{
    @Override
    public void showReport(List<Action> actions, Receiver receiver)
    {
        Action action = actions.get(0);
        //Optional<BlockSnapshot> orig = action.getCached(BLOCKS_ORIG, Recall::origSnapshot).get(0);

        Text cause = Recall.cause(action);
        receiver.sendReport(actions, actions.size(),
                            "{txt} killed ?",
                            "{txt} killed ? x{}",
                            cause);
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
    public Action observe(DestructEntityEvent event)
    {
        Action action = newReport();
        action.addData(ENTITY, Observe.entity(event.getTargetEntity().createSnapshot()));
        action.addData(CAUSE, Observe.causes(event.getCause()));
        action.addData(LOCATION, Observe.location(event.getTargetEntity().getLocation()));
        return action;
    }

    @Listener
    public void onDesctruct(DestructEntityEvent event)
    {
        report(observe(event));
    }
}
