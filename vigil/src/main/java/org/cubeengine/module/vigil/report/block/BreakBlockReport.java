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

import static org.cubeengine.module.vigil.report.ReportUtil.name;

import org.cubeengine.module.vigil.Receiver;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.Recall;
import org.cubeengine.module.vigil.report.Report;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.entity.explosive.Explosive;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.cause.EventContextKey;
import org.spongepowered.api.world.LocatableBlock;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
public class BreakBlockReport extends BlockReport<ChangeBlockEvent.Break>
{
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
        if (!orig.isPresent())
        {
            throw new IllegalStateException();
        }
        showReport(actions, receiver, action, orig.get());
    }

    private void showReport(List<Action> actions, Receiver receiver, Action action, BlockSnapshot orig)
    {
        receiver.sendReport(this, actions, actions.size(),
                            "{txt} break {txt}",
                            "{txt} break {txt} x{}",
                            Recall.cause(action), name(orig, receiver), actions.size());
    }

    @Listener(order = Order.POST)
    public void listen(ChangeBlockEvent.Break event)
    {
        if (event.getCause().root() instanceof LocatableBlock)
        {
            report(event); // Fire
            return;
        }
        Optional<Player> causePlayer = event.getCause().first(Player.class);
        causePlayer.ifPresent(player -> {
                    // TODO indirect falling sand has no player in Notifier cause
                    // TODO player is source when destroying block /w hanging blocks on it but should be notifier? source is the block

                    // TODO cause filtering ?
                    if (!(event.getCause().root() instanceof Explosive)) // Handle Explosions later
                    {
                        report(event);
                    }

                    // TODO remove
        });
    }

    @Listener(order = Order.POST)
    public void listen(ChangeBlockEvent.Post event)
    {


        //System.out.println(event.getCause());
        if (event.getCause().first(Explosive.class).isPresent())
        {
            report(event); // Handle Explosions etc.
            System.out.print("####\n");
            for (Object o : event.getCause().all()) {
                System.out.print(o + "\n");
            }
            for (Map.Entry<EventContextKey<?>, Object> entry : event.getContext().asMap().entrySet()) {
                System.out.print(entry.getKey() + ": " + entry.getValue() +"\n");
            }

        }
    }
}
