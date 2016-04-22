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
package org.cubeengine.module.signmarket;

import java.util.Optional;
import org.cubeengine.module.core.util.BlockUtil;
import org.cubeengine.module.signmarket.data.ImmutableMarketSignData;
import org.cubeengine.module.signmarket.data.MarketSignData;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.tileentity.ImmutableSignData;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.spongepowered.api.block.BlockTypes.AIR;
import static org.spongepowered.api.block.BlockTypes.STANDING_SIGN;
import static org.spongepowered.api.block.BlockTypes.WALL_SIGN;
import static org.spongepowered.api.util.Direction.DOWN;
import static org.spongepowered.api.util.Direction.UP;

public class MarketSignListener
{
    private final  MarketSignManager manager;

    public MarketSignListener(MarketSignManager manager)
    {
        this.manager = manager;
    }

    @Listener
    public void onBlockBreak(ChangeBlockEvent.Break event, @First Player player)
    {
        for (Transaction<BlockSnapshot> transaction : event.getTransactions())
        {
            BlockSnapshot orig = transaction.getOriginal();
            BlockType type = orig.getState().getType();
            if (type == STANDING_SIGN || type == BlockTypes.WALL_SIGN)
            {
                if (orig.get(ImmutableMarketSignData.class).isPresent())
                {
                    event.setCancelled(true);
                    return;
                }
            }

            Location<World> origLoc = orig.getLocation().get();
            for (Direction blockFace : BlockUtil.BLOCK_FACES)
            {
                if (blockFace == DOWN)
                {
                    continue;
                }
                Location<World> relative = origLoc.getRelative(blockFace);
                if (!relative.get(MarketSignData.class).isPresent())
                {
                    continue;
                }
                if (blockFace == UP)
                {
                    if (relative.getBlockType() == STANDING_SIGN)
                    {
                        event.setCancelled(true);
                        return;
                    }
                }
                else
                {
                    if (relative.getBlockType() == WALL_SIGN
                     && relative.get(Keys.DIRECTION).get().getOpposite() == blockFace) // TODO right direction check?
                    {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @Listener
    public void onPlayerInteract(InteractBlockEvent event, @First Player player)
    {
        if (!event.getTargetBlock().getLocation().isPresent())
        {
            return;
        }

        Optional<ImmutableMarketSignData> mSignData = event.getTargetBlock().get(ImmutableMarketSignData.class);
        if (!mSignData.isPresent())
        {
            return;
        }

        MarketSignData data = mSignData.get().asMutable();
        Location<World> loc = event.getTargetBlock().getLocation().get();
        manager.executeSignAction(data, loc, player, event instanceof InteractBlockEvent.Secondary);
        if (loc.getBlockType() != AIR)
        {
            // TODO sign somehow is retained /w some invalid data
            loc.offer(data);
            manager.updateSignText(data, loc);
            event.setCancelled(true);
        }
    }

}
