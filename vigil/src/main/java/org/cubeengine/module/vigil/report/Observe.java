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
package org.cubeengine.module.vigil.report;

import org.cubeengine.module.vigil.report.block.BlockReport;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.cubeengine.module.vigil.report.Report.*;
import static org.cubeengine.module.vigil.report.Report.CauseType.CAUSE_BLOCK_FIRE;
import static org.cubeengine.module.vigil.report.Report.CauseType.CAUSE_PLAYER;
import static org.cubeengine.module.vigil.report.block.BlockReport.BLOCK_META;
import static org.cubeengine.module.vigil.report.block.BlockReport.BLOCK_TYPE;
import static org.spongepowered.api.block.BlockTypes.FIRE;

public class Observe
{
    public static Map<String, Object> causes(Cause causes)
    {
        Map<String, Object> data = new LinkedHashMap<>();
        for (Map.Entry<String, Object> namedCause : causes.getNamedCauses().entrySet())
        {
            Map<String, Object> causeData = cause(namedCause.getValue());
            if (causeData != null)
            {
                data.put(namedCause.getKey().replace(".", "_"), causeData);
            }
        }
        return data;
    }

    public static Map<String, Object> cause(Object cause)
    {
        if (cause instanceof Player)
        {
            return player(((Player) cause));
        }
        else if (cause instanceof BlockSnapshot)
        {
            return block(((BlockSnapshot) cause));
        }
        // TODO other causes that interest us
        return null;
    }

    public static Map<String, Object> block(BlockSnapshot block)
    {
        if (FIRE.equals(block.getState().getType()))
        {
            Map<String, Object> data = new HashMap<>();
            data.put(CAUSE_TYPE, CAUSE_BLOCK_FIRE.toString());
            return data;
        }
        return null;
    }

    public static Map<String, Object> player(Player player)
    {
        Map<String, Object> data = new HashMap<>();
        data.put(CAUSE_TYPE, CAUSE_PLAYER.toString());

        data.put(CAUSE_PLAYER_UUID, player.getUniqueId());
        data.put(CAUSE_PLAYER_NAME, player.getName());
        // TODO configurable data.put("ip", player.getConnection().getAddress().getAddress().getHostAddress());
        return data;
    }

    public static void fromContainter(Map<String, Object> data, DataContainer container, DataQuery query)
    {
        container.get(query).ifPresent(value -> data.put(query.asString('_'), value));
    }

    public static Map<String, Object> location(Location<World> location)
    {
        if (location == null)
        {
            throw new IllegalArgumentException("The location should not be null");
        }

        Map<String, Object> info = new HashMap<>();

        info.put(WORLD.asString("_"), location.getExtent().getUniqueId().toString());
        // TODO worldname also recall it
        info.put(X.asString("_"), location.getBlockX());
        info.put(Y.asString("_"), location.getBlockY());
        info.put(Z.asString("_"), location.getBlockZ());

        return info;
    }

    // Observe
    public static Map<String, Object> blockSnapshot(DataContainer block)
    {
        Map<String, Object> info = new HashMap<>();

        fromContainter(info, block, BLOCK_TYPE);
        fromContainter(info, block, BLOCK_META);

        return info;
    }

    /**
     * Observes a BlockTransaction
     *
     * @param transaction the transaction to observe
     * @return the obeserved data
     */
    public static Map<String, Object> transactions(Transaction<BlockSnapshot> transaction)
    {
        Map<String, Object> data = new HashMap<>();
        BlockSnapshot original = transaction.getOriginal();
        if (original.getLocation().isPresent())
        {
            data.put(LOCATION, location(original.getLocation().get()));
            data.put(BlockReport.ORIGINAL, blockSnapshot(original.toContainer()));
            data.put(BlockReport.REPLACEMENT, blockSnapshot(transaction.getFinal().toContainer()));
        }
        return data;
    }
}
