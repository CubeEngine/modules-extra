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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bson.BsonDocument;
import org.bson.Document;
import org.cubeengine.module.vigil.Receiver;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.Observe;
import org.cubeengine.module.vigil.report.Recall;
import org.cubeengine.module.vigil.report.Report;
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
        boolean samelocGroup = action.getData("samelocgroup") == null ? false : true;
        Optional<BlockSnapshot> orig = action.getCached(BLOCKS_ORIG, Recall::origSnapshot);
        Optional<BlockSnapshot> repl = action.getCached(BLOCKS_REPL, Recall::replSnapshot);

        if (samelocGroup)
        {
            Component cause = Recall.cause(action);
            List<Component> replacements = new ArrayList<>();
            for (Action action1 : actions)
            {
                final BlockSnapshot repl1 = action1.getCached(BLOCKS_REPL, Recall::replSnapshot).orElse(BlockSnapshot.empty());
                orig = action1.getCached(BLOCKS_ORIG, Recall::replSnapshot);
                replacements.add(name(repl1, receiver));
            }
            Collections.reverse(replacements);
            final TextComponent separator = Component.text("↴", NamedTextColor.GRAY).append(Component.newline());

            if (repl.isPresent() && !repl.get().state().type().isAnyOf(AIR))
            {
                Component replacementText = name(repl.get(), receiver).append(Component.text("...").hoverEvent(HoverEvent.showText(Component.join(separator, replacements))));
                if (orig.isPresent() && !orig.get().state().type().equals(AIR.get()))
                {
                    if (repl.get().equals(orig.get()))
                    {
                        receiver.sendReport(this, actions, "{txt} ended up leaving {txt}", cause, replacementText);
                    }
                    else
                    {
                        receiver.sendReport(this, actions, "{txt} ended up replacing {txt} with {txt}", cause, name(orig.get(), receiver), replacementText);
                    }
                }
                else
                {
                    receiver.sendReport(this, actions, "{txt} ended up placing {txt}", cause, replacementText);
                }
                return;
            }
            if (repl.get().equals(orig.get()))
            {
                final Component origText = name(orig.get(), receiver).append(Component.text("...").hoverEvent(HoverEvent.showText(Component.join(separator, replacements))));
                    receiver.sendReport(this, actions, "{txt} ended up leaving {txt}", cause, origText);
            }
            else
            {
                final Component origText = name(orig.get(), receiver).append(Component.text("...").hoverEvent(HoverEvent.showText(Component.join(separator, replacements))));
                    receiver.sendReport(this, actions, "{txt} ended up breaking {txt}", cause, origText);
            }

            return;
        }

        if (repl.isPresent() && !repl.get().state().type().isAnyOf(AIR))
        {
            if (orig.isPresent() && orig.get().state().type() == repl.get().state().type())
            {
                showReportModify(actions, receiver, action, orig.get(), repl.get(), samelocGroup);
                return;
            }
            showReportPlace(actions, receiver, action, orig, repl.get(), samelocGroup);
            return;
        }
        if (orig.isPresent())
        {
            showReportBreak(actions, receiver, action, orig.get(), samelocGroup);
            return;
        }
        throw new IllegalStateException();
    }

    private void showReportModify(List<Action> actions, Receiver receiver, Action action, BlockSnapshot orig, BlockSnapshot repl, boolean samelocGroup)
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

    private void showReportPlace(List<Action> actions, Receiver receiver, Action action, Optional<BlockSnapshot> orig, BlockSnapshot repl, boolean samelocGroup)
    {
        Component cause = Recall.cause(action);
        if (orig.isPresent() && !orig.get().state().type().equals(AIR.get()))
        {
            receiver.sendReport(this, actions, actions.size(),
                                "{txt} replaced {txt} with {txt}",
                                "{txt} replaced {txt} with {txt} x{}",
                                cause, name(orig.get(), receiver), name(repl, receiver), actions.size());
        }
        else
        {
            receiver.sendReport(this, actions, actions.size(),
                                "{txt} placed {txt}",
                                "{txt} placed {txt} x{}",
                                cause, name(repl, receiver), actions.size());
        }
    }

    private void showReportBreak(List<Action> actions, Receiver receiver, Action action, BlockSnapshot orig, boolean samelocGroup)
    {
        receiver.sendReport(this, actions, actions.size(),
                            "{txt} broke {txt}",
                            "{txt} broke {txt} x{}",
                            Recall.cause(action), name(orig, receiver), actions.size());
    }

    protected void report(ChangeBlockEvent.Post event)
    {
        for (BlockTransactionReceipt receipt : event.receipts())
        {
            final ServerLocation loc = receipt.originalBlock().location().get();
            if (!isActive(loc.world()))
            {
                continue;
            }
            if (isRedstoneChange(receipt.originalBlock().state(), receipt.finalBlock().state()))
            {
                continue;
            }

            final Action action = observe(event);
            action.addData(BLOCK_CHANGES, Observe.transactions(receipt));
            action.addData(LOCATION, Observe.location(loc));
            action.addData(OPERATION, receipt.operation().key(RegistryTypes.OPERATION).asString());

            report(action);
        }
    }

    private static boolean isRedstoneChange(BlockState origState, BlockState finalState)
    {
        if (!origState.type().equals(finalState.type()))
        {
            return false;
        }
        return origState.type().isAnyOf(BlockTypes.REDSTONE_WIRE, BlockTypes.REPEATER, BlockTypes.COMPARATOR,
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
            if (!repl1.get().world().equals(repl2.get().world()))
            {
                return false;
            }
            if (!repl1.get().state().equals(repl2.get().state()))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean group(Object lookup, Action action, Action otherAction, Report otherReport)
    {
        if (!this.getClass().equals(otherReport.getClass()))
        {
            return false;
        }

        if (this.isSameLocationGroup(lookup, action, otherAction, otherReport))
        {
            action.addData("samelocgroup", true);
            otherAction.addData("samelocgroup", true);
            return true;
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

        final Document thisCause = action.getData(CAUSE);
        final Document otherCause = otherAction.getData(CAUSE);
        if (!thisCause.get(FULLCAUSELIST).equals(otherCause.get(FULLCAUSELIST)))
        {
            return false;
        }

        // TODO in short timeframe (minutes? configurable)
        return true;
    }

    private boolean isSameLocationGroup(Object lookup, Action action, Action otherAction, Report otherReport)
    {
        if (!action.getData(LOCATION).equals(otherAction.getData(LOCATION)))
        {
            return false;
        }
        final Document thisCause = action.getData(CAUSE);
        final Document otherCause = otherAction.getData(CAUSE);
        if (!thisCause.get(FULLCAUSELIST).equals(otherCause.get(FULLCAUSELIST)))
        {
            return false;
        }

        if (action.getDate().getTime() - otherAction.getDate().getTime() > 1000 * 60)
        {
            return false;
        }

        Optional<BlockSnapshot> orig1 = action.getCached(BLOCKS_ORIG, Recall::origSnapshot);
        Optional<BlockSnapshot> orig2 = otherAction.getCached(BLOCKS_ORIG, Recall::origSnapshot);
        Optional<BlockSnapshot> repl1 = action.getCached(BLOCKS_ORIG, Recall::origSnapshot);
        Optional<BlockSnapshot> repl2 = otherAction.getCached(BLOCKS_ORIG, Recall::origSnapshot);


        return orig1.isPresent() && repl2.isPresent() && orig1.get().equals(repl2.get());
    }


    @Listener(order = Order.POST)
    public void listen(ChangeBlockEvent.Post event)
    {
        report(event);
    }
}
