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
package org.cubeengine.module.vigil.report.inventory;

import java.util.List;
import org.cubeengine.module.vigil.Receiver;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.Observe;
import org.cubeengine.module.vigil.report.Recall;
import org.cubeengine.module.vigil.report.Report;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.text.Text;

public class InventoryOpenReport extends InventoryReport<InteractInventoryEvent.Open> implements Report.Readonly
{
    @Override
    public void showReport(List<Action> actions, Receiver receiver)
    {
        Action action = actions.get(0);
        receiver.sendReport(actions, actions.size(),
                "{txt} open {txt}",
                "{txt} open {txt} x{}",
                Recall.cause(action), Text.of("?"), actions.size());
    }

    @Override
    public boolean group(Object lookup, Action action, Action otherAction, Report otherReport)
    {
        if (!this.equals(otherReport))
        {
            return false;
        }

        if (!action.getData(CAUSE).equals(otherAction.getData(CAUSE)))
        {
            // TODO check same cause better
            return false;
        }

        return true;
    }

    @Override
    public Action observe(InteractInventoryEvent.Open event)
    {
        Action action = newReport();
        action.addData(CAUSE, Observe.causes(event.getCause()));
        // TODO get TargetInventory
        return action;
    }

    @Listener(order = Order.POST)
    public void listen(InteractInventoryEvent.Open event, @First Player player)
    {
        report(observe(event));
    }

    @Override
    public void apply(Action action, boolean noOp)
    {}
}
