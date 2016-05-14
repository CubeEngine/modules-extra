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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.cubeengine.module.vigil.report.entity.EntityReport;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.MemoryDataContainer;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;

import static org.cubeengine.module.vigil.report.Report.LOCATION;
import static org.cubeengine.module.vigil.report.block.BlockReport.*;
import static org.spongepowered.api.text.format.TextColors.DARK_GREEN;
import static org.spongepowered.api.text.format.TextColors.YELLOW;

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

        toContainer(container, data, BLOCK_STATE);
        toContainer(container, data, BLOCK_DATA);

        toContainer(container, data, BLOCK_UNSAFE_DATA);

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
                (Map<String, Object>) data.get(NamedCause.NOTIFIER),
                     (Map<String, Object>) data.get(NamedCause.SOURCE + "1"));
    }

    public static Text cause(Map<String, Object> source, Map<String, Object> notifier, Map<String, Object> source2)
    {
        Text text = Text.of("?");
        if (source != null)
        {
            CauseType type = CauseType.valueOf(source.get(CAUSE_TYPE).toString());
            text = cause(source, text, type);
        }

        if (notifier != null)
        {
            text = text.toBuilder().append(Text.of("…").toBuilder().onHover(
                    TextActions.showText(Text.of(text, "←", cause(notifier, Text.of(),
                            CauseType.valueOf(notifier.get(CAUSE_TYPE).toString()))))).build()).build();
        }

        if (source2 != null)
        {
            Text source2Text = cause(source2, Text.of(), CauseType.valueOf(source2.get(CAUSE_TYPE).toString()));
            text = source2Text.toBuilder().append(Text.of("…").toBuilder().onHover(
                TextActions.showText(Text.of(source2Text, "←", text))).build()).build();
        }

        return text;
    }

    private static Text cause(Map<String, Object> source, Text text, CauseType type)
    {
        switch (type)
        {
            case CAUSE_PLAYER:
                text = Text.of(DARK_GREEN, source.get(CAUSE_NAME)).toBuilder()
                           .onHover(TextActions.showText(Text.of(YELLOW, source.get(CAUSE_PLAYER_UUID)))).build();
                break;
            case CAUSE_BLOCK_FIRE:
                text = Text.of(TextColors.RED, "Fire"); // TODO translate
                break;
            case CAUSE_BLOCK_AIR:
                text = Text.of(TextColors.GOLD, "Indirect"); // TODO translate
                break;
            case CAUSE_TNT:
                text = Text.of(TextColors.RED, "TNT"); // TODO translatable
                if (source.get(CAUSE_PLAYER_UUID) == null)
                {
                    text = text.toBuilder().append(Text.of(" (", Text.of(TextColors.GOLD, source.get(CAUSE_NAME)), ")")).build();
                }
                else
                {
                    text = text.toBuilder().append(Text.of(" (", Text.of(DARK_GREEN, source.get(CAUSE_NAME)).toBuilder()
                                 .onHover(TextActions.showText(Text.of(YELLOW, source.get(CAUSE_PLAYER_UUID)))).build(), ")")).build();
                }
                break;
        }
        return text;
    }

    public static EntitySnapshot entity(Action action)
    {
        MemoryDataContainer data = new MemoryDataContainer();
        Map<String, Object> map = ((Map<String, Object>) ((Map<String, Object>)action.getData(EntityReport.ENTITY)).get(EntityReport.ENTITY_DATA));
        for (Entry<String, Object> entry : map.entrySet())
        {
            data.set(DataQuery.of(entry.getKey()), entry.getValue());
        }
        return EntitySnapshot.builder().build(data).orElse(null);
    }

    public static Entity restoredEntity(Action action)
    {
        Entity entity = Recall.entity(action).restore().get();
        entity.remove();
        return entity;
    }
}
