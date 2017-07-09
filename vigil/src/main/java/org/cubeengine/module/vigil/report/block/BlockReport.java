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
import java.util.UUID;
import java.util.stream.Collectors;

import com.flowpowered.math.vector.Vector3i;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.BaseReport;
import org.cubeengine.module.vigil.report.Observe;
import org.cubeengine.module.vigil.report.Recall;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.trait.BlockTrait;
import org.spongepowered.api.block.trait.BooleanTraits;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.world.BlockChangeFlag;

public abstract class BlockReport<T extends ChangeBlockEvent> extends BaseReport<T>
{
    public static final String BLOCK_CHANGES = "block-changes";
    public static final String BLOCKS_ORIG = BLOCK_CHANGES + "-orig";
    public static final String BLOCKS_REPL = BLOCK_CHANGES + "-repl";
    public static final DataQuery BLOCK_STATE = DataQuery.of("BlockState");
    public static final DataQuery BLOCK_DATA = DataQuery.of("TileEntityData");
    public static final DataQuery BLOCK_UNSAFE_DATA = DataQuery.of("UnsafeData");
    public static final DataQuery BLOCK_ITEMS = DataQuery.of("UnsafeData", "Items");
    public static final String ORIGINAL = "original";
    public static final String REPLACEMENT = "replacement";

    @Override
    protected Action observe(ChangeBlockEvent event)
    {
        Action action = newReport();
        action.addData(CAUSE, Observe.causes(event.getCause()));
        return action;
    }

    protected void report(ChangeBlockEvent event)
    {
        UUID multi = UUID.randomUUID();
        for (Map.Entry<Vector3i, List<Transaction<BlockSnapshot>>> entry : event.getTransactions().stream().collect(Collectors.groupingBy(t -> t.getOriginal().getPosition())).entrySet())
        {
            // Only get one transaction for one block
            Transaction<BlockSnapshot> trans = entry.getValue().get(0);
            if (!isActive(trans.getOriginal().getLocation().get().getExtent()))
            {
                continue;
            }

            if (isRedstoneChange(trans.getOriginal().getState(), trans.getFinal().getState()))
            {
                continue;
            }

            Action action = observe(event);
            action.addData(BLOCK_CHANGES, Observe.transactions(trans));
            action.addData(LOCATION, Observe.location(trans.getOriginal().getLocation().get()));
            if (event.getTransactions().size() > 1)
            {
                action.addData(MULTIACTION, multi.toString());
            }
            report(action);
        }
    }

    private static boolean isRedstoneChange(BlockState origState, BlockState finalState)
    {
        if (origState.getType().equals(finalState.getType()) && origState.getType().equals(BlockTypes.REDSTONE_WIRE))
        {
            return true;
        }

        return isChangeFromOrTo(origState, finalState, BlockTypes.POWERED_REPEATER, BlockTypes.UNPOWERED_REPEATER)
            || isChangeFromOrTo(origState, finalState, BlockTypes.POWERED_COMPARATOR, BlockTypes.UNPOWERED_COMPARATOR)
            || isChangeFromOrTo(origState, finalState, BlockTypes.REDSTONE_TORCH, BlockTypes.UNLIT_REDSTONE_TORCH)
            || isChangeTrait(origState, finalState, BlockTypes.DROPPER, BooleanTraits.DROPPER_TRIGGERED, false)
            || isChangeTrait(origState, finalState, BlockTypes.DISPENSER, BooleanTraits.DISPENSER_TRIGGERED, false)
            || isChangeTrait(origState, finalState, BlockTypes.HOPPER, BooleanTraits.HOPPER_ENABLED, true);
    }

    private static boolean isChangeFromOrTo(BlockState origState, BlockState finalState, BlockType from, BlockType to)
    {
        return origState.getType().equals(from) && finalState.getType().equals(to)
            || origState.getType().equals(to) && finalState.getType().equals(from);
    }

    private static <T extends Comparable<T>> boolean isChangeTrait(BlockState origState, BlockState finalSate, BlockType type, BlockTrait<T> trait, T traitDefault)
    {
        if (origState.getType().equals(finalSate.getType()) && origState.getType().equals(type))
        {
            return origState.withTrait(trait, traitDefault).equals(finalSate.withTrait(trait, traitDefault));
        }
        return false;
    }

    protected boolean group(Optional<BlockSnapshot> repl1, Optional<BlockSnapshot> repl2)
    {
        if ((repl1.isPresent() && !repl2.isPresent()) || (!repl1.isPresent() && repl2.isPresent()))
        {
            return false;
        }

        if (repl1.isPresent() && repl2.isPresent())
        {
            if (!repl1.get().getWorldUniqueId().equals(repl2.get().getWorldUniqueId()))
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
    public void apply(Action action, boolean noOp)
    {
        // TODO noOp
        action.getCached(BLOCKS_REPL, Recall::replSnapshot).get().restore(true, BlockChangeFlag.NONE);
    }

    @Override
    public void unapply(Action action, boolean noOp)
    {
        // TODO noOp
        action.getCached(BLOCKS_ORIG, Recall::origSnapshot).get().restore(true, BlockChangeFlag.NONE);
    }
}
