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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import net.kyori.adventure.text.Component;
import org.cubeengine.module.vigil.Receiver;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.Observe;
import org.cubeengine.module.vigil.report.Recall;
import org.cubeengine.module.vigil.report.Report;
import org.cubeengine.module.vigil.report.ReportUtil;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.container.ClickContainerEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.BlockCarrier;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.query.QueryTypes;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.item.inventory.type.CarriedInventory;

import static org.spongepowered.api.item.inventory.ItemStackComparators.*;

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
    private static final Comparator<ItemStack> COMPARATOR = Ordering.compound(ImmutableList.of(TYPE.get(), ITEM_DATA.get()));

    @Override
    public void showReport(List<Action> actions, Receiver receiver)
    {
        Component cause = Recall.cause(actions.get(0));

        LinkedList<Transaction<ItemStack>> transactions = new LinkedList<>();

        for (Action action : actions)
        {
            List<Map<String, Object>> changes = action.getData(INVENTORY_CHANGES);
            for (Map<String, Object> change : changes)
            {
                ItemStack originStack = Recall.item(((Map<String, Object>) change.get(ORIGINAL))).get().createStack();
                ItemStack finalStack = Recall.item(((Map<String, Object>) change.get(REPLACEMENT))).get().createStack();

                if (COMPARATOR.compare(originStack, finalStack) == 0)
                {
                    if (originStack.quantity() > finalStack.quantity())
                    {
                        ItemStack stack = originStack;
                        stack.setQuantity(originStack.quantity() - finalStack.quantity());
                        originStack = stack;
                        finalStack = ItemStack.empty();
                    }
                    else
                    {
                        ItemStack stack = finalStack;
                        stack.setQuantity(finalStack.quantity() - originStack.quantity());
                        finalStack = stack;
                        originStack = ItemStack.empty();
                    }
                }

                boolean added = false;
                for (Transaction<ItemStack> trans : transactions)
                {
                    if (originStack.isEmpty())
                    {
                        if (COMPARATOR.compare(trans.finalReplacement(), finalStack) == 0 && trans.original().isEmpty())
                        {
                            trans.finalReplacement().setQuantity(trans.finalReplacement().quantity() + finalStack.quantity());
                            added = true;
                            break;
                        }
                        else if (trans.finalReplacement().isEmpty() && COMPARATOR.compare(trans.original(), finalStack) == 0)
                        {
                            trans.original().setQuantity(trans.original().quantity() - finalStack.quantity());
                            added = true;
                            break;
                        }
                        else if (COMPARATOR.compare(trans.original(), finalStack) == 0)
                        {
                            break;
                        }
                    }
                    if (finalStack.isEmpty())
                    {
                        if (COMPARATOR.compare(trans.original(), originStack) == 0)
                        {
                            trans.original().setQuantity(trans.original().quantity() + originStack.quantity());
                            added = true;
                            break;
                        }
                        else if (trans.original().isEmpty() && COMPARATOR.compare(trans.finalReplacement(), originStack) == 0)
                        {
                            trans.finalReplacement().setQuantity(trans.finalReplacement().quantity() - originStack.quantity());
                            added = true;
                            break;
                        }
                        else if (COMPARATOR.compare(trans.finalReplacement(), originStack) == 0)
                        {
                            break;
                        }
                    }
                }
                if (!added)
                {
                    transactions.addFirst(new Transaction<>(originStack, finalStack));
                }

            }
        }

        Collections.reverse(transactions);

        for (Transaction<ItemStack> trans : transactions)
        {
            ItemStack stack1 = trans.original();
            ItemStack stack2 = trans.finalReplacement();
            if (stack1.isEmpty() && stack2.isEmpty())
            {
                continue;
            }
            if (stack1.type().isAnyOf(ItemTypes.AIR))
            {
                receiver.sendReport(this, actions, "{txt} inserted {txt}", cause, ReportUtil.name(stack2.createSnapshot()));
            }
            else if (stack2.type().isAnyOf(ItemTypes.AIR))
            {
                receiver.sendReport(this, actions, "{txt} took {txt}", cause, ReportUtil.name(stack1.createSnapshot()));
            }
            else
            {
                receiver.sendReport(this, actions, "{txt} swapped {txt} with {txt}", cause, ReportUtil.name(stack1.createSnapshot()), ReportUtil.name(stack2.createSnapshot()));
            }
        }
    }

    @Override
    public boolean group(Object lookup, Action action, Action otherAction, Report otherReport)
    {
        if (!this.equals(otherReport))
        {
            return false;
        }
        if (Recall.location(action).equals(Recall.location(otherAction))
            && Recall.cause(action).equals(Recall.cause(otherAction)))
        {
            return true;
        }
        // TODO group by location
        return false;
    }

    @Override
    public void apply(Action action, boolean noOp)
    {

    }

    @Listener
    public void listen(ClickContainerEvent event)
    {
        List<SlotTransaction> upperTransactions = new ArrayList<>();
        int upperSize = event.inventory().viewed().get(0).capacity();
        for (SlotTransaction transaction : event.transactions())
        {
            Integer affectedSlot = transaction.slot().get(Keys.SLOT_INDEX).orElse(-1);
            boolean upper = affectedSlot != -1 && affectedSlot < upperSize;
            if (upper)
            {
                upperTransactions.add(transaction);
            }
        }

        final Container inventory = event.inventory();
        ((CarriedInventory)inventory).carrier().ifPresent(carrier -> {
            if (carrier instanceof BlockCarrier)
            {
                Action action = this.observe(event);
                action.addData(INVENTORY_CHANGES, Observe.transactions(upperTransactions));
                action.addData(Report.LOCATION, Observe.location(((BlockCarrier) carrier).serverLocation()));
                this.report(action);
            }
        });
    }

    @Override
    public Action observe(ChangeInventoryEvent event)
    {
        Action action = newReport();
        action.addData(CAUSE, Observe.causes(event.cause()));
        return action;
    }
}
