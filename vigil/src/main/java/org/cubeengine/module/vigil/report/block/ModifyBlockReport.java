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
package org.cubeengine.module.vigil.report.block;

import java.util.List;
import java.util.Optional;
import org.cubeengine.module.vigil.Receiver;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.Recall;
import org.cubeengine.module.vigil.report.Report;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.value.mutable.MutableBoundedValue;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.text.Text;

import static org.cubeengine.module.vigil.report.ReportUtil.name;
import static org.spongepowered.api.block.BlockTypes.AIR;

/* TODO change
sign
noteblock
jukebox
repeater
plate
lever
door
comparatpr
cake
button

bonemeal?
 */
public class ModifyBlockReport extends BlockReport<ChangeBlockEvent.Modify>
{
    @Override
    public void showReport(List<Action> actions, Receiver receiver)
    {
        Action action = actions.get(0);
        Optional<BlockSnapshot> orig = action.getCached(BLOCKS_ORIG, Recall::origSnapshot).get(0);
        Optional<BlockSnapshot> repl = action.getCached(BLOCKS_REPL, Recall::replSnapshot).get(0);

        Text cause = Recall.cause(action);
        if (!repl.isPresent() || !orig.isPresent())
        {
            throw new IllegalStateException();
        }
        Optional<MutableBoundedValue<Integer>> growth = repl.get().getValue(Keys.GROWTH_STAGE);
        if (growth.isPresent())
        {
            if (growth.get().get().equals(growth.get().getMaxValue()))
            {
                receiver.sendReport(actions, actions.size(),
                                    "{txt} let {txt} grow to maturity",
                                    "{txt} let {txt} grow to maturity x{}",
                                    cause, name(orig.get()), actions.size());
                return;
            }
            receiver.sendReport(actions, actions.size(),
                                "{txt} let {txt} grow",
                                "{txt} let {txt} grow x{}",
                                cause, name(orig.get()), actions.size());
            return;
        }
        // TODO other modifyables
        receiver.sendReport(actions, actions.size(),
                            "{txt} modified {txt}",
                            "{txt} modified {txt} x{}",
                            cause, name(orig.get()), actions.size());
    }

    @Override
    public boolean group(Object lookup, Action action, Action otherAction, Report otherReport)
    {
        // TODO
        return false;
    }

    @Listener(order = Order.POST)
    public void listen(ChangeBlockEvent.Modify event, @First Player player)
    {
        // TODO cause filtering
        report(observe(event));
    }
}
