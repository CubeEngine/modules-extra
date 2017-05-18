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
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.entity.explosive.PrimedTNT;
import org.spongepowered.api.entity.living.Agent;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.damage.source.BlockDamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.IndirectEntityDamageSource;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.module.vigil.report.Report.CAUSE_NAME;
import static org.cubeengine.module.vigil.report.Report.CAUSE_PLAYER_UUID;
import static org.cubeengine.module.vigil.report.Report.CAUSE_TYPE;
import static org.cubeengine.module.vigil.report.Report.CauseType.*;
import static org.cubeengine.module.vigil.report.Report.LOCATION;
import static org.cubeengine.module.vigil.report.Report.WORLD;
import static org.cubeengine.module.vigil.report.Report.X;
import static org.cubeengine.module.vigil.report.Report.Y;
import static org.cubeengine.module.vigil.report.Report.Z;
import static org.cubeengine.module.vigil.report.block.BlockReport.*;
import static org.spongepowered.api.block.BlockTypes.*;

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
        return cause(cause, true);
    }

    public static Map<String, Object> cause(Object cause, boolean doRecursion)
    {
        if (cause instanceof EntityDamageSource)
        {
            Entity source = ((EntityDamageSource)cause).getSource();
            Map<String, Object> sourceCause = Observe.cause(source);
            Map<String, Object> indirectCause = null;
            if (cause instanceof IndirectEntityDamageSource)
            {
                indirectCause = Observe.cause(((IndirectEntityDamageSource)cause).getIndirectSource());
                if (sourceCause == null)
                {
                    return indirectCause;
                }
            }
            // TODO indirectCause
            return sourceCause;
        }
        else if (cause instanceof BlockDamageSource)
        {
            return Observe.cause(((BlockDamageSource)cause).getBlockSnapshot());
        }
        else if (cause instanceof DamageSource)
        {
            return Observe.damageCause(((DamageSource)cause));
        }

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
        else if (cause instanceof Entity)
        {
            return entityCause(((Entity) cause), doRecursion);
        }
        // TODO other causes that interest us
        return null;
    }

    private static Map<String, Object> damageCause(DamageSource cause)
    {
        Map<String, Object> data = new HashMap<>();
        data.put(CAUSE_TYPE, CAUSE_DAMAGE.toString());
        data.put(CAUSE_NAME, cause.getType().getId());
        return data;
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

    private static Map<String, Object> entityCause(Entity cause, boolean doRecursion)
    {
        Map<String, Object> data = new HashMap<>();
        data.put(CAUSE_TYPE, CAUSE_ENTITY.toString());
        data.put(CAUSE_NAME, cause.getType().getId());

        if (doRecursion && cause instanceof Agent)
        {
            if (((Agent) cause).getTarget().isPresent())
            {
                data.put(CAUSE_TARGET, cause(((Agent) cause).getTarget().get(), false));
            }
        }
        return data;
    }

    public static Map<String, Object> blockCause(BlockSnapshot block)
    {
        BlockType type = block.getState().getType();
        Map<String, Object> data = new HashMap<>();
        data.put(CAUSE_TYPE, CAUSE_BLOCK.toString());
        data.put(CAUSE_NAME, type.getId());
        return data;
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
            //data.put(LOCATION, location(transaction.getFinal().getLocation().get()));
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
        data.put(EntityReport.ENTITY_DATA, toRawData(entity.toContainer()));
        return data;
    }
}
