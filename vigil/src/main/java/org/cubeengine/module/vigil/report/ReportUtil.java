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

import java.util.*;

import org.spongepowered.api.Game;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockSnapshotBuilder;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.MemoryDataContainer;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.action.HoverAction;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.module.vigil.report.block.BlockReport.*;

public class ReportUtil
{
    public static final DataQuery WORLD = DataQuery.of("WorldUuid");
    public static final DataQuery X = DataQuery.of("Position", "X");
    public static final DataQuery Y = DataQuery.of("Position", "Y");
    public static final DataQuery Z = DataQuery.of("Position", "Z");
    public static final String CAUSE = "cause";
    public static final String PLAYER = "player";
    public static final String UUID = "UUID";
    public static final String NAME = "name";
    public static final String LOCATION = "location";

    // Observe
    public static Map<String, Object> observeBlockSnapshot(DataContainer block)
    {
        Map<String, Object> info = new HashMap<>();

        observe(info, block, BLOCK_TYPE);
        observe(info, block, BLOCK_META);

        return info;
    }

    public static Map<String, Object> observeLocation(Location<World> location)
    {
        if (location == null)
        {
            throw new IllegalArgumentException("The location should not be null");
        }

        Map<String, Object> info = new HashMap<>();

        info.put(WORLD.asString("_"), location.getExtent().getUniqueId().toString());
        info.put(X.asString("_"), location.getBlockX());
        info.put(Y.asString("_"), location.getBlockY());
        info.put(Z.asString("_"), location.getBlockZ());

        return info;
    }

    public static void observe(Map<String, Object> data, DataContainer container, DataQuery query)
    {
        container.get(query).ifPresent(value -> data.put(query.asString('_'), value));
    }

    // Recall
    public static void recall(DataContainer container, Map<String, Object> data, DataQuery query)
    {
        Object value = data.get(query.asString("_"));
        if (value != null)
        {
            container.set(query, value);
        }
    }

    public static Optional<BlockSnapshot> recallBlockSnapshot(Game game, Map<String, Object> data, Map<String, Object> locationData)
    {
        DataContainer container = new MemoryDataContainer();
        recall(container, locationData, WORLD);
        recall(container, locationData, X);
        recall(container, locationData, Y);
        recall(container, locationData, Z);
        recall(container, data, BLOCK_TYPE);
        recall(container, data, BLOCK_META);

        return game.getRegistry().createBuilder(BlockSnapshotBuilder.class).build(container);
    }

    public static Map<String, Object> observeCause(Cause causes)
    {
        Map<String, Object> data = new LinkedHashMap<>();
        for (Object cause : causes.all())
        {
            if (cause instanceof Player)
            {
                data.put(PLAYER, observePlayer(((Player)cause)));
            }
            else
            {
                data.get("otherstuffs");
                // TODO other causes that interest us
            }
        }
        return data;
    }

    public static Map<String, Object> observePlayer(Player player)
    {
        Map<String, Object> data = new HashMap<>();
        data.put(UUID, player.getUniqueId());
        data.put(NAME, player.getName());
        // TODO configurable data.put("ip", player.getConnection().getAddress().getAddress().getHostAddress());
        return data;
    }

    public static Text name(BlockType type)
    {
        return Texts.of(TextColors.GOLD, type.getTranslation()).builder().onHover(TextActions.showText(Texts.of(type.getName()))).build();
    }
}
