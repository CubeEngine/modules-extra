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
package org.cubeengine.module.vigil.report;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.module.vigil.report.entity.EntityReport;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;

import static org.cubeengine.module.vigil.report.Report.*;
import static org.cubeengine.module.vigil.report.block.BlockReport.CAUSE;
import static org.cubeengine.module.vigil.report.block.BlockReport.CAUSE_NAME;
import static org.cubeengine.module.vigil.report.block.BlockReport.CAUSE_PLAYER_UUID;
import static org.cubeengine.module.vigil.report.block.BlockReport.CAUSE_TARGET;
import static org.cubeengine.module.vigil.report.block.BlockReport.CAUSE_TYPE;
import static org.cubeengine.module.vigil.report.block.BlockReport.CauseType;
import static org.cubeengine.module.vigil.report.block.BlockReport.WORLD;
import static org.cubeengine.module.vigil.report.block.BlockReport.X;
import static org.cubeengine.module.vigil.report.block.BlockReport.Y;
import static org.cubeengine.module.vigil.report.block.BlockReport.Z;
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

    public static Optional<BlockSnapshot> block(Map<String, Object> data, Map<String, Object> locationData)
    {
        DataContainer container = DataContainer.createNew();
        toContainer(container, locationData, Report.WORLD);
        toContainer(container, locationData, Report.X);
        toContainer(container, locationData, Report.Y);
        toContainer(container, locationData, Report.Z);

        toContainer(container, data, BLOCK_STATE);
        toContainer(container, data, BLOCK_DATA);

        toContainer(container, data, BLOCK_UNSAFE_DATA);

        return BlockSnapshot.builder().build(container);
    }

    public static Optional<ItemStackSnapshot> item(Map<String, Object> data)
    {
        DataContainer container = ((DataContainer) toContainer(data));
        return Optional.of(ItemStack.builder().fromContainer(container).build().createSnapshot());
    }

    public static Optional<BlockSnapshot> origSnapshot(Action action)
    {
        Map<String, Object> changes = action.getData(BLOCK_CHANGES);
        return block(ORIGINAL.get(changes), action.getData(LOCATION));
    }

    public static Optional<BlockSnapshot> replSnapshot(Action action)
    {
        Map<String, Object> changes = action.getData(BLOCK_CHANGES);
        return block(REPLACEMENT.get(changes), action.getData(LOCATION));
    }

    @SuppressWarnings("unchecked")
    public static Component cause(Action action)
    {
        Map<String, Object> data = action.getData(CAUSE);
        List<Map<String, Object>> list = (List<Map<String, Object>>)data.get(FULLCAUSELIST);
        Map<String, Object> context = ((Map<String, Object>) data.get(CAUSECONTEXT));
        return cause(list, context);
    }

    public static Component cause(List<Map<String, Object>> list, Map<String, Object> context)
    {
        if (list.size() > 6) {
            list = list.subList(0, 6);
        }
        final Builder builder = Component.text();
        boolean first = true;
        for (Map<String, Object> elem : list)
        {
            CauseType type = CauseType.valueOf(elem.get(CAUSE_TYPE).toString());
            if (first)
            {
                builder.append(cause(elem, Component.text("?"), type));
            }
            else
            {
                final Component cause = cause(elem, Component.empty(), type);
                final TextComponent hover = builder.build().append(Component.text("←")).append(cause);
                builder.append(Component.text("…").hoverEvent(HoverEvent.showText(hover)));
            }
            first = false;
        }

        // Notifier
        if (context != null && context.containsKey(EventContextKeys.NOTIFIER.getKey().asString())) {
            final Map<String, Object> notifierContext = (Map<String, Object>)context.get(EventContextKeys.NOTIFIER.getKey().asString());
            builder.append(Component.text("←", NamedTextColor.GRAY)).append(cause(notifierContext, Component.empty(), CauseType.CAUSE_PLAYER));
        }
        return builder.build();
    }

    private static Component cause(Map<String, Object> source, Component defText, CauseType type)
    {
        if (source == null) {
            return defText;
        }
        final String causeName = source.get(CAUSE_NAME).toString();
        final Object causePlayerUUID = source.get(CAUSE_PLAYER_UUID);
        switch (type)
        {
            case CAUSE_PLAYER:
                return Component.text(causeName, NamedTextColor.DARK_GREEN).hoverEvent(HoverEvent.showText(Component.text(causePlayerUUID.toString(), NamedTextColor.YELLOW)));
            case CAUSE_BLOCK:
                final Optional<BlockType> bType = RegistryTypes.BLOCK_TYPE.get().findValue(ResourceKey.resolve(causeName));
                if (!bType.isPresent())
                {
                    return Component.text("unknown Block", NamedTextColor.GOLD); // TODO translate
                }
                if (bType.get().isAnyOf(BlockTypes.LAVA, BlockTypes.FIRE))
                {
                    return bType.get().asComponent().color(NamedTextColor.RED);
                }
                return bType.get().asComponent().color(NamedTextColor.GOLD);
            case CAUSE_TNT:

                final TextComponent tntCause;
                if (causePlayerUUID == null)
                {
                    tntCause = Component.text(causeName, NamedTextColor.GOLD);
                }
                else
                {
                    tntCause = Component.text(causeName, NamedTextColor.DARK_GREEN).hoverEvent(HoverEvent.showText(Component.text(causePlayerUUID.toString(), NamedTextColor.YELLOW)));
                }
                return Component.text("TNT", NamedTextColor.RED) // TODO translatable
                        .append(Component.space()).append(Component.text("(")).append(tntCause).append(Component.text(")"));
            case CAUSE_DAMAGE:
                return Component.text(causeName, NamedTextColor.GOLD);
            case CAUSE_ENTITY:
                final Component entityCause = RegistryTypes.ENTITY_TYPE.get().findValue(ResourceKey.resolve(causeName)).map(EntityType::asComponent)
                                                                       .orElse(Component.text(causeName)).color(NamedTextColor.GOLD);

                // TODO translation
                if (source.containsKey(CAUSE_TARGET) && source.get(CAUSE_TARGET) != null)
                {
                    Map<String, Object> sourceTarget = ((Map<String, Object>) source.get(CAUSE_TARGET));
                    CauseType targetType = CauseType.valueOf(sourceTarget.get(CAUSE_TYPE).toString());
                    entityCause.append(Component.text("◎", NamedTextColor.GRAY)).append(cause(sourceTarget, Component.text("?"), targetType));
                }

                if (source.containsKey(CAUSE_INDIRECT))
                {
                    Map<String, Object> indirect = ((Map<String, Object>) source.get(CAUSE_INDIRECT));
                    CauseType targetType = CauseType.valueOf(indirect.get(CAUSE_TYPE).toString());
                    entityCause.append(Component.text("↶", NamedTextColor.GRAY)).append(cause(indirect, Component.text("?"), targetType));
                }
                return entityCause;
            default:
                return defText;
        }
    }

    public static Object toContainer(Object data)
    {
        if (data instanceof Map)
        {
            DataContainer container = DataContainer.createNew();
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

    public static ServerLocation location(Action action)
    {
        final Map<String, Object> data = action.getData(LOCATION);
        final ServerWorld world = Sponge.getServer().getWorldManager().world(ResourceKey.resolve(data.get(WORLD).toString())).get();
        final Integer x = (Integer)data.get(X.asString("_"));
        final Integer y = (Integer)data.get(Y.asString("_"));
        final Integer z = (Integer)data.get(Z.asString("_"));
        return world.getLocation(x, y, z);
    }
}
