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

import static java.util.stream.Collectors.toList;
import static org.cubeengine.module.vigil.report.Report.CAUSE_INDIRECT;
import static org.cubeengine.module.vigil.report.Report.CAUSE_NAME;
import static org.cubeengine.module.vigil.report.Report.CAUSE_PLAYER_UUID;
import static org.cubeengine.module.vigil.report.Report.CAUSE_TYPE;
import static org.cubeengine.module.vigil.report.Report.CauseType.CAUSE_BLOCK;
import static org.cubeengine.module.vigil.report.Report.CauseType.CAUSE_DAMAGE;
import static org.cubeengine.module.vigil.report.Report.CauseType.CAUSE_ENTITY;
import static org.cubeengine.module.vigil.report.Report.CauseType.CAUSE_PLAYER;
import static org.cubeengine.module.vigil.report.Report.CauseType.CAUSE_TNT;
import static org.cubeengine.module.vigil.report.Report.WORLD;
import static org.cubeengine.module.vigil.report.Report.X;
import static org.cubeengine.module.vigil.report.Report.Y;
import static org.cubeengine.module.vigil.report.Report.Z;
import static org.cubeengine.module.vigil.report.block.BlockReport.BLOCK_DATA;
import static org.cubeengine.module.vigil.report.block.BlockReport.BLOCK_STATE;
import static org.cubeengine.module.vigil.report.block.BlockReport.BLOCK_UNSAFE_DATA;
import static org.cubeengine.module.vigil.report.block.BlockReport.CAUSE_TARGET;
import static org.cubeengine.module.vigil.report.block.BlockReport.ORIGINAL;
import static org.cubeengine.module.vigil.report.block.BlockReport.REPLACEMENT;

import org.cubeengine.module.vigil.report.entity.EntityReport;
import org.cubeengine.module.vigil.report.inventory.ChangeInventoryReport;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
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
import org.spongepowered.api.event.cause.EventContextKey;
import org.spongepowered.api.event.cause.entity.damage.source.BlockDamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.IndirectEntityDamageSource;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Observe
{
    public static Map<String, Object> causes(Cause causes)
    {
        Map<String, Object> data = new LinkedHashMap<>();
        List<Object> causeList = new ArrayList<>();
        data.put(Report.FULLCAUSELIST, causeList);
        Set<Object> set = new HashSet<>();
        for (Object namedCause : causes.all())
        {
            Map<String, Object> causeData = cause(namedCause, set);
            if (causeData != null)
            {
                causeList.add(causeData);
            }
        }

        HashMap<Object, Object> context = new HashMap<>();
        data.put(Report.CAUSECONTEXT, context);

        for (EventContextKey<?> key : causes.getContext().keySet())
        {
            context.put(key.getId().replace(".", "_"), cause(causes.getContext().get(key).get(), new HashSet<>()));
        }

        return data;
    }

    public static Map<String, Object> cause(Object cause, Set<Object> set)
    {
        return cause(cause, true, set);
    }

    public static Map<String, Object> cause(Object cause, boolean doRecursion, Set<Object> set)
    {
        if (set.contains(cause))
        {
            return null;
        }
        if (cause instanceof EntityDamageSource)
        {
            Entity source = ((EntityDamageSource)cause).getSource();
            Map<String, Object> sourceCause = Observe.cause(source, set);
            if (cause instanceof IndirectEntityDamageSource)
            {
                Map<String, Object> indirectCause = Observe.cause(((IndirectEntityDamageSource)cause).getIndirectSource(), set);
                if (sourceCause == null)
                {
                    return indirectCause;
                }
                set.add(indirectCause);
                sourceCause.put(CAUSE_INDIRECT, indirectCause);
            }
            set.add(source);
            return sourceCause;
        }
        else if (cause instanceof BlockDamageSource)
        {
            return Observe.cause(((BlockDamageSource)cause).getBlockSnapshot(), set);
        }
        else if (cause instanceof DamageSource)
        {
            return Observe.damageCause(((DamageSource)cause));
        }

        if (cause instanceof Player)
        {
            return playerCause(((Player) cause));
        }
        else if (cause instanceof LocatableBlock)
        {
            return blockCause(((LocatableBlock) cause).getBlockState());
        }
        else if (cause instanceof BlockSnapshot)
        {
            return blockCause(((BlockSnapshot) cause).getState());
        }
        else if (cause instanceof PrimedTNT)
        {
            return tntCause(((PrimedTNT)cause));
        }
        else if (cause instanceof Entity)
        {
            return entityCause(((Entity) cause), doRecursion, set);
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

    private static Map<String, Object> entityCause(Entity cause, boolean doRecursion, Set<Object> set)
    {
        Map<String, Object> data = new HashMap<>();
        data.put(CAUSE_TYPE, CAUSE_ENTITY.toString());
        data.put(CAUSE_NAME, cause.getType().getId());

        if (doRecursion && cause instanceof Agent)
        {
            if (((Agent) cause).getTarget().isPresent())
            {
                data.put(CAUSE_TARGET, cause(((Agent) cause).getTarget().get(), false, set));
            }
        }
        return data;
    }

    public static Map<String, Object> blockCause(BlockState block)
    {
        BlockType type = block.getType();
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
            return ((List<?>)data).stream().map(Observe::toRawData).collect(toList());
        }
        if (data.getClass().isEnum())
        {
            return ((Enum)data).name();
        }
        if (data instanceof int[])
        {
            return Arrays.stream(((int[]) data)).boxed().collect(toList());
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
            ORIGINAL.put(data, blockSnapshot(original));
            REPLACEMENT.put(data, blockSnapshot(transaction.getFinal()));
        }
        return data;
    }

    /**
     * Observes a SlotTransaction
     *
     * @param transactions the transaction to observe
     * @return the observed data
     */
    public static List<Map<String, Object>> transactions(List<SlotTransaction> transactions)
    {
        List<Map<String, Object>> list = new ArrayList<>();
        for (SlotTransaction transaction : transactions)
        {
            Map<String, Object> data = new HashMap<>();
            ItemStackSnapshot originalStack = transaction.getOriginal();
            ItemStackSnapshot finalStack = transaction.getFinal();
            data.put(ChangeInventoryReport.ORIGINAL, toRawData(originalStack.toContainer()));
            data.put(ChangeInventoryReport.REPLACEMENT, toRawData(finalStack.toContainer()));
            data.put(ChangeInventoryReport.SLOT_INDEX, transaction.getSlot().getInventoryProperty(SlotIndex.class).map(SlotIndex::getValue).orElse(-1));
            list.add(data);
        }
        return list;
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
