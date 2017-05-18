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
import java.util.UUID;
import java.util.stream.Collectors;
import org.cubeengine.module.vigil.report.entity.EntityReport;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.MemoryDataContainer;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.ImmutableRepresentedItemData;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.translation.Translation;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

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

    public static Optional<BlockSnapshot> origSnapshot(Action action)
    {
        Map<String, Object> changes = action.getData(BLOCK_CHANGES);
        return blockSnapShot(((Map<String, Object>)changes.get(ORIGINAL)), action.getData(LOCATION));
    }

    public static Optional<BlockSnapshot> replSnapshot(Action action)
    {
        Map<String, Object> changes = action.getData(BLOCK_CHANGES);
        return blockSnapShot(((Map<String, Object>)changes.get(REPLACEMENT)), action.getData(LOCATION));
    }

    @SuppressWarnings("unchecked")
    public static Text cause(Action action)
    {
        Map<String, Object> data = action.getData(CAUSE);
        Map<String, Object> source = (Map<String, Object>)data.get(NamedCause.SOURCE);
        Map<String, Object> attacker = (Map<String, Object>)data.get("Attacker");
        Map<String, Object> playerSource = (Map<String, Object>)data.get("PlayerSource");
        Map<String, Object> notifier = (Map<String, Object>)data.get(NamedCause.NOTIFIER);
        Map<String, Object> source2 = (Map<String, Object>)data.get(NamedCause.SOURCE + "1");
        return cause(source == null ? attacker : source, notifier, playerSource == null ? source2 : playerSource);
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
        Object causeName = source.get(CAUSE_NAME);
        switch (type)
        {
            case CAUSE_PLAYER:
                text = Text.of(DARK_GREEN, causeName).toBuilder()
                           .onHover(TextActions.showText(Text.of(YELLOW, source.get(CAUSE_PLAYER_UUID)))).build();
                break;
            case CAUSE_BLOCK:
                Optional<BlockType> bType = Sponge.getRegistry().getType(BlockType.class, causeName.toString());
                if (!bType.isPresent())
                {
                    text = Text.of(TextColors.GOLD, "unknown Block"); // TODO translate
                }
                else
                {
                    if (bType.get() == BlockTypes.LAVA || bType.get() == BlockTypes.FLOWING_LAVA || bType.get() == BlockTypes.FIRE)
                    {
                        text = Text.of(TextColors.RED, bType.get().getTranslation());
                    }
                    else
                    {
                        text = Text.of(TextColors.GOLD, bType.get().getTranslation());
                    }
                }
                break;
            case CAUSE_TNT:
                text = Text.of(TextColors.RED, "TNT"); // TODO translatable
                if (source.get(CAUSE_PLAYER_UUID) == null)
                {
                    text = text.toBuilder().append(Text.of(" (", Text.of(TextColors.GOLD, causeName), ")")).build();
                }
                else
                {
                    text = text.toBuilder().append(Text.of(" (", Text.of(DARK_GREEN, causeName).toBuilder()
                                                                     .onHover(TextActions.showText(Text.of(YELLOW, source.get(CAUSE_PLAYER_UUID)))).build(), ")")).build();
                }
                break;
            case CAUSE_DAMAGE:
                text = Text.of(TextColors.GOLD, causeName);
                break;
            case CAUSE_ENTITY:
                text = Text.of(TextColors.GOLD, Sponge.getRegistry().getType(EntityType.class, causeName.toString())
                        .map(EntityType::getTranslation).map(Translation::get).orElse(causeName.toString()));
                // TODO translation
                if (source.containsKey(CAUSE_TARGET))
                {
                    Map<String, Object> sourceTarget = ((Map<String, Object>) source.get(CAUSE_TARGET));
                    CauseType targetType = CauseType.valueOf(sourceTarget.get(CAUSE_TYPE).toString());
                    text = text.toBuilder().append(Text.of("…").toBuilder().onHover(
                            TextActions.showText(Text.of(text, "◎", cause(sourceTarget, Text.of("?"), targetType)))).build()).build();
                }
                break;
        }
        return text;
    }

    public static Object toContainer(Object data)
    {
        if (data instanceof Map)
        {
            MemoryDataContainer container = new MemoryDataContainer();
            for (Entry<String, Object> entry : ((Map<String, Object>)data).entrySet())
            {
                container.set(DataQuery.of(entry.getKey()), toContainer(entry.getValue()));
            }
            return container;
        }
        if (data instanceof List)
        {
            return ((List)data).stream().map(Recall::toContainer).collect(Collectors.toList());
        }
        return data;
    }

    public static EntitySnapshot entity(Action action)
    {
        Map<String, Object> map = ((Map<String, Object>) ((Map<String, Object>)action.getData(EntityReport.ENTITY)).get(EntityReport.ENTITY_DATA));
        return EntitySnapshot.builder().build(((DataView)toContainer(map))).orElse(null);
    }

    public static Location<World> location(Action action)
    {
        Map<String, Object> data = action.getData(LOCATION);
        World world = Sponge.getServer().getWorld(UUID.fromString(data.get(WORLD.asString("_")).toString())).get();
        Integer x = (Integer)data.get(X.asString("_"));
        Integer y = (Integer)data.get(Y.asString("_"));
        Integer z = (Integer)data.get(Z.asString("_"));
        return new Location<>(world, x, y, z);
    }
}
