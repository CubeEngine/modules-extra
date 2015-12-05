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

import java.util.*;

import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.BaseReport;
import org.cubeengine.module.vigil.report.ReportUtil;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.translation.Translation;
import org.spongepowered.api.command.CommandSource;

import static java.util.Locale.ENGLISH;
import static java.util.stream.Collectors.toList;
import static org.cubeengine.module.vigil.report.ReportUtil.*;
import static org.spongepowered.api.block.BlockTypes.AIR;
import static org.spongepowered.api.text.format.TextColors.GOLD;
import static org.spongepowered.api.text.format.TextColors.YELLOW;

public abstract class BlockReport<T extends ChangeBlockEvent> extends BaseReport<T>
{
    public static final String BLOCK_CHANGES = "block-changes";
    public static final DataQuery BLOCK_TYPE = DataQuery.of("BlockState", "BlockType");
    public static final DataQuery BLOCK_META = DataQuery.of("BlockState", "UnsafeMeta");
    public static final String ORIGINAL = "original";
    public static final String REPLACEMENT = "replacement";

    @Override
    public Action observe(T event)
    {
        Action action = newReport();
        action.addData(BLOCK_CHANGES, event.getTransactions().stream().map(BlockReport::observe).collect(toList()));
        action.addData(CAUSE, observeCause(event.getCause()));

        // TODO remove this is for debugging
        event.getCause().first(Player.class).ifPresent(p -> showReport(action, p));

        return action;
    }

    @Override
    public boolean group(Action action, Action other)
    {

        return false;
    }

    @Override
    public void apply(Action action, boolean rollback)
    {

    }

    public static class Break extends BlockReport<ChangeBlockEvent.Break>
    {

        @Override
        public void showReport(Action action, CommandSource cmdSource)
        {
            List<Map<String, Object>> changes = action.getData(BLOCK_CHANGES);
            for (Map<String, Object> data : changes)
            {
                @SuppressWarnings("unchecked")
                Optional<BlockSnapshot> recalled = recallBlockSnapshot(vigil.getGame(), ((Map<String, Object>) data.get(ORIGINAL)), ((Map<String, Object>) data.get(LOCATION)));
                if (recalled.isPresent())
                {
                    cmdSource.sendMessage(Texts.of(YELLOW, "Break ", ReportUtil.name(recalled.get().getState().getType())));
                }
            }
        }

        @Listener(order = Order.POST)
        public void listen(ChangeBlockEvent.Break event)
        {
            if (event.getCause().first(Player.class).isPresent()) // TODO cause filtering
            {
                report(observe(event));
            }
        }
    }

    public static class Place extends BlockReport<ChangeBlockEvent.Place>
    {
        @Override
        public void showReport(Action action, CommandSource cmdSource)
        {
            List<Map<String, Object>> changes = action.getData(BLOCK_CHANGES);
            for (Map<String, Object> data : changes)
            {
                @SuppressWarnings("unchecked")
                Map<String, Object> locationData = (Map<String, Object>) data.get(LOCATION);
                @SuppressWarnings("unchecked")
                Optional<BlockSnapshot> repl = recallBlockSnapshot(vigil.getGame(), ((Map<String, Object>) data.get(REPLACEMENT)), locationData);
                @SuppressWarnings("unchecked")
                Optional<BlockSnapshot> orig = recallBlockSnapshot(vigil.getGame(), ((Map<String, Object>) data.get(ORIGINAL)), locationData);


                if (orig.isPresent() && !orig.get().getState().getType().equals(AIR))
                {
                    if (repl.isPresent())
                    {
                        // TODO Translations in here
                        cmdSource.sendMessage(Texts.of(YELLOW, "Changed ", ReportUtil.name(orig.get().getState().getType()),
                                YELLOW, " to ", ReportUtil.name(repl.get().getState().getType())));
                    }
                }
                else
                {
                    if (repl.isPresent())
                    {
                        cmdSource.sendMessage(Texts.of(YELLOW, "Place ", ReportUtil.name(repl.get().getState().getType())));
                    }
                }
            }
        }

        @Listener(order = Order.POST)
        public void listen(ChangeBlockEvent.Place event)
        {
            if (event.getCause().first(Player.class).isPresent()) // TODO cause filtering
            {
                report(observe(event));
            }
        }

        /*
        @Listener(order = Order.POST)
        public void listen(ChangeBlockEvent event)
        {
            if (!event.getCause().first(Player.class).isPresent())
            {
                return;
            }
            if (event instanceof ChangeBlockEvent.Place)
            {
                System.out.print("\nChangeBlockEvent.Place");
            }
            else
            {
                System.out.print("\nChangeBlockEvent");
            }

            for (Transaction<BlockSnapshot> trans : event.getTransactions())
            {
                System.out.print("\nFrom " + trans.getOriginal());
                System.out.print("\nto " + trans.getFinal());
            }

            for (Object o : event.getCause().all())
            {
                System.out.print("\nby " + o);
            }

            System.out.print("\n");
        }
        // */
    }

    /**
     * Observes a BlockTransaction
     *
     * @param transaction the transaction to observe
     * @return the obeserved data
     */
    public static Map<String, Object> observe(Transaction<BlockSnapshot> transaction)
    {
        Map<String, Object> data = new HashMap<>();
        BlockSnapshot original = transaction.getOriginal();
        if (original.getLocation().isPresent())
        {
            data.put(LOCATION, observeLocation(original.getLocation().get()));
            data.put(ORIGINAL, observeBlockSnapshot(original.toContainer()));
            data.put(REPLACEMENT, observeBlockSnapshot(transaction.getFinal().toContainer()));
        }
        return data;
    }


}
