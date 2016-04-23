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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.cubeengine.module.vigil.report.block.BlockReport;
import org.cubeengine.module.vigil.report.entity.EntityReport;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.EnderCrystal;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.entity.ExperienceOrb;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.explosive.PrimedTNT;
import org.spongepowered.api.entity.hanging.Hanging;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.vehicle.Boat;
import org.spongepowered.api.entity.vehicle.minecart.Minecart;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.module.vigil.report.Report.*;
import static org.cubeengine.module.vigil.report.Report.CauseType.*;
import static org.cubeengine.module.vigil.report.block.BlockReport.*;
import static org.spongepowered.api.block.BlockTypes.AIR;
import static org.spongepowered.api.block.BlockTypes.FIRE;

public class Observe
{
    public static Map<String, Object> causes(Cause causes)
    {
        // TODO EntityDamageSource as Cause
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
            return playerCause(((Player) cause));
        }
        else if (cause instanceof BlockSnapshot)
        {
            return blockCause(((BlockSnapshot) cause));
        }
        else if (cause instanceof PrimedTNT)
        {
            return tntCause(((PrimedTNT)cause));
        }
        // TODO other causes that interest us
        return null;
    }

    private static Map<String, Object> tntCause(PrimedTNT cause)
    {
        Map<String, Object> data = new HashMap<>();
        if (cause.getDetonator().isPresent())
        {
            if (cause.getDetonator().get() instanceof Player)
            {
                data.put(CAUSE_NAME, ((Player)cause.getDetonator().get()).getName());
                data.put(CAUSE_PLAYER_UUID, cause.getDetonator().get().getUniqueId());
            }
            else
            {
                data.put(CAUSE_NAME, cause.getDetonator().get().getType().getId());
            }
        }

        data.put(CAUSE_TYPE, CAUSE_TNT.toString());
        return data;
    }

    public static Map<String, Object> blockCause(BlockSnapshot block)
    {
        if (FIRE.equals(block.getState().getType()))
        {
            Map<String, Object> data = new HashMap<>();
            data.put(CAUSE_TYPE, CAUSE_BLOCK_FIRE.toString());
            return data;
        }
        if (AIR.equals(block.getState().getType()))
        {
            Map<String, Object> data = new HashMap<>();
            data.put(CAUSE_TYPE, CAUSE_BLOCK_AIR.toString());
            return data;
        }
        return null;
    }

    public static Map<String, Object> playerCause(Player player)
    {
        Map<String, Object> data = new HashMap<>();
        data.put(CAUSE_TYPE, CAUSE_PLAYER.toString());

        data.put(CAUSE_PLAYER_UUID, player.getUniqueId());
        data.put(CAUSE_NAME, player.getName());
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
    public static Map<String, Object> blockSnapshot(BlockSnapshot block)
    {
        Map<String, Object> info = new HashMap<>();

        info.put(BLOCK_STATE.asString("_"), toRawData(block.getState().toContainer()));

        DataContainer blockContainer = block.toContainer();
        Optional<List<DataView>> data = blockContainer.getViewList(BLOCK_DATA);
        if (data.isPresent())
        {
            info.put(BLOCK_DATA.asString("_"), toRawData(data.get()));
        }

        Optional<Object> unsafe = blockContainer.get(BLOCK_UNSAFE_DATA);
        if (unsafe.isPresent())
        {
            info.put(BLOCK_UNSAFE_DATA.asString("_"), toRawData(unsafe.get()));
        }

        return info;
    }

    /**
     * Converts potentialDataViews into corresponding String -> Object Maps
     * @param data the data to convert
     * @return the converted DataView or the original data
     */
    public static Object toRawData(Object data)
    {
        if (data instanceof DataQuery)
        {
            return ((DataQuery)data).asString("_");
        }
        if (data instanceof DataView)
        {
            return ((DataView)data).getValues(false).entrySet().stream()
                .collect(Collectors.toMap(e -> toRawData(e.getKey()),
                                          e -> toRawData(e.getValue())));
        }
        if (data instanceof Map)
        {
            return ((Map<?,?>)data).entrySet().stream()
                                   .collect(Collectors.toMap(e -> toRawData(e.getKey()),
                                                             e -> toRawData(e.getValue())));
        }
        if (data instanceof List)
        {
            return ((List<?>)data).stream().map(Observe::toRawData).collect(Collectors.toList());
        }
        if (data.getClass().isEnum())
        {
            return ((Enum)data).name();
        }
        return data;
    }

    /**
     * Observes a BlockTransaction
     *
     * @param transaction the transaction to observe
     * @return the observed data
     */
    public static Map<String, Object> transactions(Transaction<BlockSnapshot> transaction)
    {
        Map<String, Object> data = new HashMap<>();
        BlockSnapshot original = transaction.getOriginal();
        if (original.getLocation().isPresent())
        {
            //System.out.print(transaction.getFinal().getLocation().get().getPosition() +  " " + transaction.getFinal().getState().getType() + "\n");
            data.put(LOCATION, location(transaction.getFinal().getLocation().get()));
            data.put(BlockReport.ORIGINAL, blockSnapshot(original));
            data.put(BlockReport.REPLACEMENT, blockSnapshot(transaction.getFinal()));
        }
        return data;
    }

    /**
     * Observes an EntitySnapshot
     * @param entity the entity
     * @return the observed data
     */
    public static Map<String, Object> entity(EntitySnapshot entity)
    {
        Map<String, Object> data = new HashMap<>();

        Class<? extends Entity> clazz = entity.getType().getEntityClass();
        if (Living.class.isAssignableFrom(clazz))
        {
            data.put(EntityReport.ENTITY_DATA, toRawData(entity.toContainer()));
        }
        else if (Item.class.isAssignableFrom(clazz))
        {
            data.put(EntityReport.ENTITY_DATA, toRawData(entity.toContainer()));
        }
        else if (Boat.class.isAssignableFrom(clazz) || Minecart.class.isAssignableFrom(clazz))
        {
            data.put(EntityReport.ENTITY_DATA, toRawData(entity.toContainer()));
        }
        else if (Hanging.class.isAssignableFrom(clazz))
        {
            data.put(EntityReport.ENTITY_DATA, toRawData(entity.toContainer()));
        }
        else if (ExperienceOrb.class.isAssignableFrom(clazz))
        {
            data.put(EntityReport.ENTITY_DATA, toRawData(entity.toContainer()));
        }
        else if (EnderCrystal.class.isAssignableFrom(clazz))
        {
            data.put(EntityReport.ENTITY_DATA, toRawData(entity.toContainer()));
        }
        else // FallingBlock, WeatherEffect, Projectile, Explosive, Projectile
        {
            data.put(EntityReport.ENTITY_DATA + "_EntityType", entity.getType().getId());
        }

        return data;
    }
}
