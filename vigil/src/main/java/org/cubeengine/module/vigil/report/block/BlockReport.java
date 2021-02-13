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
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.cubeengine.module.vigil.Receiver;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.BaseReport;
import org.cubeengine.module.vigil.report.Observe;
import org.cubeengine.module.vigil.report.Recall;
import org.cubeengine.module.vigil.report.Report;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.transaction.BlockTransactionReceipt;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.BlockChangeFlags;
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
public abstract class BlockReport extends BaseReport<ChangeBlockEvent.Post>
{
    public static final Action.DataKey<Map<String, Object>> BLOCK_CHANGES = new Action.DataKey<>("block-changes");
    public static final Action.DataKey<Optional<BlockSnapshot>> BLOCKS_ORIG = new Action.DataKey<>(BLOCK_CHANGES.name + "-orig");
    public static final Action.DataKey<Optional<BlockSnapshot>> BLOCKS_REPL = new Action.DataKey<>(BLOCK_CHANGES.name + "-repl");
    public static final Action.DataKey<String> OPERATION = new Action.DataKey<>("operation");
    public static final DataQuery BLOCK_STATE = DataQuery.of("BlockState");
    public static final DataQuery BLOCK_DATA = DataQuery.of("TileEntityData");
    public static final DataQuery BLOCK_UNSAFE_DATA = DataQuery.of("UnsafeData");
    public static final DataQuery BLOCK_ITEMS = DataQuery.of("UnsafeData", "Items");
    public static final Action.DataKey<Map<String, Object>> ORIGINAL = new Action.DataKey<>("original");
    public static final Action.DataKey<Map<String, Object>> REPLACEMENT = new Action.DataKey<>("replacement");

    @Override
    protected Action observe(ChangeBlockEvent.Post event)
    {
        Action action = newReport();
        action.addData(CAUSE, Observe.causes(event.getCause()));
        return action;
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

    protected boolean group(Optional<BlockSnapshot> repl1, Optional<BlockSnapshot> repl2)
    {
        if ((repl1.isPresent() && !repl2.isPresent()) || (!repl1.isPresent() && repl2.isPresent()))
        {
            return false;
        }

        if (repl1.isPresent() && repl2.isPresent())
        {
            if (!repl1.get().getWorld().equals(repl2.get().getWorld()))
            {
                return false;
            }
            if (!repl1.get().getState().equals(repl2.get().getState()))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean group(Object lookup, Action action, Action otherAction, Report otherReport)
    {
        if (!this.equals(otherReport))
        {
            return false;
        }

        Optional<BlockSnapshot> orig1 = action.getCached(BLOCKS_ORIG, Recall::origSnapshot);
        Optional<BlockSnapshot> orig2 = otherAction.getCached(BLOCKS_ORIG, Recall::origSnapshot);

        if (!group(orig1, orig2))
        {
            return false;
        }

        Optional<BlockSnapshot> repl1 = action.getCached(BLOCKS_ORIG, Recall::origSnapshot);
        Optional<BlockSnapshot> repl2 = otherAction.getCached(BLOCKS_ORIG, Recall::origSnapshot);

        if (!group(repl1, repl2))
        {
            return false;
        }

        if (!action.getData(CAUSE).equals(otherAction.getData(CAUSE)))
        {
            // TODO check same cause better
            return false;
        }

        // TODO in short timeframe (minutes? configurable)
        return true;
    }

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

    @Listener(order = Order.POST)
    public void listen(ChangeBlockEvent.Post event)
    {
        report(event);
    }

    @Override
    public void apply(Action action, boolean noOp)
    {
        // TODO noOp
        action.getCached(BLOCKS_REPL, Recall::replSnapshot).get().restore(true, BlockChangeFlags.NONE);
    }

    @Override
    public void unapply(Action action, boolean noOp)
    {
        // TODO noOp
        action.getCached(BLOCKS_ORIG, Recall::origSnapshot).get().restore(true, BlockChangeFlags.NONE);
    }
}
