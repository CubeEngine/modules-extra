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

import java.util.Optional;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.BaseReport;
import org.cubeengine.module.vigil.report.Observe;
import org.cubeengine.module.vigil.report.Recall;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.event.block.ChangeBlockEvent;

import static java.util.stream.Collectors.toList;

public abstract class BlockReport<T extends ChangeBlockEvent> extends BaseReport<T>
{
    public static final String BLOCK_CHANGES = "block-changes";
    public static final String BLOCKS_ORIG = BLOCK_CHANGES + "-orig";
    public static final String BLOCKS_REPL = BLOCK_CHANGES + "-repl";
    public static final DataQuery BLOCK_TYPE = DataQuery.of("BlockState", "BlockType");
    public static final DataQuery BLOCK_META = DataQuery.of("BlockState", "UnsafeMeta");
    public static final String ORIGINAL = "original";
    public static final String REPLACEMENT = "replacement";

    @Override
    protected Action observe(T event)
    {
        Action action = newReport();
        action.addData(BLOCK_CHANGES, event.getTransactions().stream().map(Observe::transactions).collect(toList()));
        action.addData(CAUSE, Observe.causes(event.getCause()));
        return action;
    }

    @Override
    public void apply(Action action, boolean noOp)
    {
        // TODO noOp
        for (Optional<BlockSnapshot> snapshot : action.getCached(BLOCKS_REPL, Recall::replSnapshot))
        {
            if (snapshot.isPresent())
            {
                snapshot.get().restore(true, false);
            }
        }
    }

    @Override
    public void unapply(Action action, boolean noOp)
    {
        // TODO noOp
        for (Optional<BlockSnapshot> snapshot : action.getCached(BLOCKS_ORIG, Recall::origSnapshot))
        {
            if (snapshot.isPresent())
            {
                snapshot.get().restore(true, false);
            }
        }
    }
}
