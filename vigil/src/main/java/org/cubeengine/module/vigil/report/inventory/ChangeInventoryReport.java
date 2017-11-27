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
package org.cubeengine.module.vigil.report.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.cubeengine.module.vigil.Receiver;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.Observe;
import org.cubeengine.module.vigil.report.Recall;
import org.cubeengine.module.vigil.report.Report;
import org.cubeengine.module.vigil.report.ReportUtil;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.text.Text;

/* TODO
inventory
-insert
-remove
-move
-item-pickup
 */
public class ChangeInventoryReport extends InventoryReport<ChangeInventoryEvent> implements Report.Readonly
{
    public static final String INVENTORY_CHANGES = "inventory-changes";
    public static final String ORIGINAL = "original";
    public static final String REPLACEMENT = "replacement";
    public static final String SLOT_INDEX = "slot-index";

    @Override
    public void showReport(List<Action> actions, Receiver receiver)
    {
        Text cause = Recall.cause(actions.get(0));
        for (Action action : actions)
        {
            List<Map<String, Object>> changes = action.getData(INVENTORY_CHANGES);
            for (Map<String, Object> change : changes)
            {
                ItemStackSnapshot originStack = Recall.item(((Map<String, Object>) change.get(ORIGINAL))).get();
                ItemStackSnapshot finalStack = Recall.item(((Map<String, Object>) change.get(REPLACEMENT))).get();
                if (originStack.getType() == ItemTypes.AIR)
                {
                    receiver.sendReport(actions, "{txt} placed {txt}", cause, ReportUtil.name(finalStack));
                }
                else if (finalStack.getType() == ItemTypes.AIR)
                {
                    receiver.sendReport(actions, "{txt} took {txt}", cause, ReportUtil.name(originStack));
                }
                else
                {
                    receiver.sendReport(actions, "{txt} replaced {txt} with {txt}", cause, ReportUtil.name(originStack), ReportUtil.name(finalStack));
                }
            }
        }
    }

    @Override
    public boolean group(Object lookup, Action action, Action otherAction, Report otherReport)
    {
        // TODO group by location
        return false;
    }

    @Override
    public void apply(Action action, boolean noOp)
    {

    }

    @Listener
    public void listen(ClickInventoryEvent event)
    {
        List<SlotTransaction> upperTransactions = new ArrayList<>();
        int upperSize = event.getTargetInventory().iterator().next().capacity();
        for (SlotTransaction transaction : event.getTransactions())
        {
            Integer affectedSlot = transaction.getSlot().getInventoryProperty(SlotIndex.class).map(SlotIndex::getValue).orElse(-1);
            boolean upper = affectedSlot != -1 && affectedSlot < upperSize;
            if (upper)
            {
                upperTransactions.add(transaction);
            }
        }

        Inventory te = event.getTargetInventory().query(TileEntity.class);
        if (!(te instanceof TileEntity))
        {
            te = te.first();
        }
        if (te instanceof TileEntity)
        {
            Action action = this.observe(event);
            action.addData(INVENTORY_CHANGES, Observe.transactions(upperTransactions));
            action.addData(Report.LOCATION, Observe.location(((TileEntity) te).getLocation()));
            this.report(action);
        }
    }

    @Override
    public Action observe(ChangeInventoryEvent event)
    {
        Action action = newReport();
        action.addData(CAUSE, Observe.causes(event.getCause()));
        return action;
    }
}
