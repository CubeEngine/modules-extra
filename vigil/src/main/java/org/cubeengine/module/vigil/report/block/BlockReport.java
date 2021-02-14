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
package org.cubeengine.module.vigil.report.block;

import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.cubeengine.module.vigil.Receiver;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.Observe;
import org.cubeengine.module.vigil.report.Recall;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.transaction.BlockTransactionReceipt;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.server.ServerLocation;

import static org.cubeengine.module.vigil.report.ReportUtil.name;
import static org.spongepowered.api.block.BlockTypes.AIR;

/* TODO Break
Sign
Trample ?
Bucket
Inventory
Jukebox item
tnt ignite?
decay
fade
fall

sheep eat

entitybreak
endermanpickup
 */
/* TODO Place
Bucket
grow

ignite (fire spreads)
-fireball
-lava
-lighter
-lightning
-other
-spread

flow
-lava
-water

form
grow

pistonmove
spread

entityform
entitychange
endermanplace
 */
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
public class BlockReport extends BaseBlockReport<ChangeBlockEvent.Post>
{
    @Override
    public void showReport(List<Action> actions, Receiver receiver)
    {
        Action action = actions.get(0);

        Optional<BlockSnapshot> orig = action.getCached(BLOCKS_ORIG, Recall::origSnapshot);
        Optional<BlockSnapshot> repl = action.getCached(BLOCKS_REPL, Recall::replSnapshot);
        if (repl.isPresent())
        {
            if (orig.isPresent() && orig.get().getState().getType() == repl.get().getState().getType())
            {
                showReportModify(actions, receiver, action, orig.get(), repl.get());
                return;
            }
            showReportPlace(actions, receiver, action, orig, repl.get());
            return;
        }
        if (orig.isPresent())
        {
            showReportBreak(actions, receiver, action, orig.get());
            return;
        }
        throw new IllegalStateException();
    }

    private void showReportModify(List<Action> actions, Receiver receiver, Action action, BlockSnapshot orig, BlockSnapshot repl)
    {
        Component cause = Recall.cause(action);

        final Optional<Integer> growth = repl.get(Keys.GROWTH_STAGE);
        if (growth.isPresent())
        {
// TODO max growth is gone?
//                        if (growth.get().equals(growth.get().getMaxValue()))
//                        {
//                            receiver.sendReport(this, actions, actions.size(),
//                                                "{txt} let {txt} grow to maturity",
//                                                "{txt} let {txt} grow to maturity x{}",
//                                                cause, name(orig.get(), receiver), actions.size());
//                            return;
//                        }
            receiver.sendReport(this, actions, actions.size(),
                                "{txt} let {txt} grow",
                                "{txt} let {txt} grow x{}",
                                cause, name(orig, receiver), actions.size());
            return;
        }
        // TODO other modifyables
        receiver.sendReport(this, actions, actions.size(),
                            "{txt} modified {txt}",
                            "{txt} modified {txt} x{}",
                            cause, name(orig, receiver), actions.size());
    }

    private void showReportPlace(List<Action> actions, Receiver receiver, Action action, Optional<BlockSnapshot> orig, BlockSnapshot repl)
    {
        Component cause = Recall.cause(action);

        if (orig.isPresent() && !orig.get().getState().getType().equals(AIR))
        {
            receiver.sendReport(this, actions, actions.size(),
                                "{txt} replace {txt} with {txt}",
                                "{txt} replace {txt} with {txt} x{}",
                                cause, name(orig.get(), receiver), name(repl, receiver), actions.size());
        }
        else
        {
            receiver.sendReport(this, actions, actions.size(),
                                "{txt} place {txt}",
                                "{txt} place {txt} x{}",
                                cause, name(repl, receiver), actions.size());
        }
    }

    private void showReportBreak(List<Action> actions, Receiver receiver, Action action, BlockSnapshot orig)
    {
        receiver.sendReport(this, actions, actions.size(),
                            "{txt} break {txt}",
                            "{txt} break {txt} x{}",
                            Recall.cause(action), name(orig, receiver), actions.size());
    }

    protected void report(ChangeBlockEvent.Post event)
    {
        for (BlockTransactionReceipt receipt : event.getReceipts())
        {
            final ServerLocation loc = receipt.getOriginal().getLocation().get();
            if (!isActive(loc.getWorld()))
            {
                continue;
            }
            if (isRedstoneChange(receipt.getOriginal().getState(), receipt.getFinal().getState()))
            {
                continue;
            }

            final Action action = observe(event);
            action.addData(BLOCK_CHANGES, Observe.transactions(receipt));
            action.addData(LOCATION, Observe.location(loc));
            action.addData(OPERATION, receipt.getOperation().key(RegistryTypes.OPERATION).asString());

            report(action);
        }
    }

    private static boolean isRedstoneChange(BlockState origState, BlockState finalState)
    {
        if (!origState.getType().equals(finalState.getType()))
        {
            return false;
        }
        return origState.getType().isAnyOf(BlockTypes.REDSTONE_WIRE, BlockTypes.REPEATER, BlockTypes.COMPARATOR,
                                           BlockTypes.REDSTONE_TORCH, BlockTypes.REDSTONE_WALL_TORCH,
                                           BlockTypes.DROPPER, BlockTypes.DISPENSER, BlockTypes.HOPPER);
    }

    @Listener(order = Order.POST)
    public void listen(ChangeBlockEvent.Post event)
    {
        report(event);
    }
}
