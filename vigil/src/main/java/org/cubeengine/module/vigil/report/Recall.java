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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.MemoryDataContainer;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import static org.cubeengine.module.vigil.report.Report.LOCATION;
import static org.cubeengine.module.vigil.report.block.BlockReport.*;

public class Recall
{
    // Recall
    public static void toContainer(DataContainer container, Map<String, Object> data, DataQuery query)
    {
        Object value = data.get(query.asString("_"));
        if (value != null)
        {
            container.set(query, value);
        }
    }

    public static Optional<BlockSnapshot> blockSnapShot(Map<String, Object> data, Map<String, Object> locationData)
    {
        DataContainer container = new MemoryDataContainer();
        toContainer(container, locationData, Report.WORLD);
        toContainer(container, locationData, Report.X);
        toContainer(container, locationData, Report.Y);
        toContainer(container, locationData, Report.Z);
        toContainer(container, data, BLOCK_TYPE);
        toContainer(container, data, BLOCK_META);

        return Sponge.getGame().getRegistry().createBuilder(BlockSnapshot.Builder.class).build(container);
    }

    public static List<Optional<BlockSnapshot>> origSnapshot(Action action)
    {
        List<Map<String, Object>> changes = action.getData(BLOCK_CHANGES);

        @SuppressWarnings("unchecked")
        List<Optional<BlockSnapshot>> collect = changes.stream()
                .map(data -> blockSnapShot(((Map<String, Object>) data.get(ORIGINAL)),
                        ((Map<String, Object>) data.get(LOCATION))))
                .collect(Collectors.toList());
        return collect;
    }

    public static List<Optional<BlockSnapshot>> replSnapshot(Action action)
    {
        List<Map<String, Object>> changes = action.getData(BLOCK_CHANGES);

        @SuppressWarnings("unchecked")
        List<Optional<BlockSnapshot>> collect = changes.stream()
                .map(data -> blockSnapShot(((Map<String, Object>) data.get(REPLACEMENT)),
                        ((Map<String, Object>) data.get(LOCATION))))
                .collect(Collectors.toList());
        return collect;
    }

    @SuppressWarnings("unchecked")
    public static Text cause(Action action)
    {
        Map<String, Object> data = action.getData(CAUSE);
        return cause((Map<String, Object>) data.get(NamedCause.SOURCE),
                (Map<String, Object>) data.get(NamedCause.NOTIFIER));
    }

    public static Text cause(Map<String, Object> source, Map<String, Object> notifier)
    {
        Text text = Text.of("?");
        if (source != null)
        {
            CauseType type = CauseType.valueOf(source.get(CAUSE_TYPE).toString());
            text = cause(source, text, type);
        }

        if (notifier != null)
        {
            text = text.toBuilder().append(Text.of("…")).onHover(
                    TextActions.showText(Text.of(text, "←", cause(notifier, Text.of(),
                            CauseType.valueOf(notifier.get(CAUSE_TYPE).toString()))))).build();
        }

        return text;
    }

    private static Text cause(Map<String, Object> source, Text text, CauseType type)
    {
        switch (type)
        {
            case CAUSE_PLAYER:
                text = Text.of(TextColors.DARK_GREEN, source.get(CAUSE_PLAYER_NAME));
                break;
            case CAUSE_BLOCK_FIRE:
                text = Text.of(TextColors.RED, "Fire"); // TODO translate
                break;
        }
        return text;
    }
}
