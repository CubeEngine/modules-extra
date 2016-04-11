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
package org.cubeengine.module.backpack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import org.cubeengine.module.core.util.ChatFormat;
import org.cubeengine.service.ContextUtil;
import org.cubeengine.service.inventoryguard.InventoryGuardFactory;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.custom.CustomInventory;
import org.spongepowered.api.service.context.Context;

public class BackpackInventory
{
    private final Backpack module;
    protected BackpackData data;

    private final Map<Player, Integer> viewers = new HashMap<>();
    private final Map<Integer, BackpackHolder> views = new HashMap<>();
    
    private static final String pageString = ChatFormat.GOLD + "Page";

    public BackpackInventory(Backpack module, BackpackData data)
    {
        this.module = module;
        this.data = data;
    }

    public void openInventory(Player player)
    {
        this.openInventory(0, player);
    }

    private Inventory getInventory(int index)
    {
        CustomInventory.builder()
            // TODO create custom inventory that is a CarriedInventory

        BackpackHolder holder = this.views.get(index);
        if (holder == null)
        {
            this.views.put(index, holder = new BackpackHolder(this, index, data.size * 9, pageString + (index + 1) + "/" + this.data.pages));
        }
        ItemStack[] contents = new ItemStack[data.size * 9];
        int offset = index * data.size * 9;
        for (int i = 0; i < data.size * 9; i++)
        {
            ItemStack itemStack = data.contents.get(i + offset);
            contents[i] = itemStack == null ? null : itemStack.clone();
        }
        holder.getInventory().setContents(contents);
        return holder.getInventory();
    }

    private void saveData(int index, Inventory inventory)
    {
        Iterable<Inventory> slots = inventory.slots();
        int offset = index * data.size * 9;
        for (int i = 0; i < data.size * 9; i++)
        {
            if (contents[i] == null)
            {
                data.contents.remove(i + offset);
            }
            else
            {
                data.contents.put(i + offset, contents[i].clone());
            }
        }
        data.save();
    }

    private void openInventory(int index, Player player)
    {
        this.viewers.put(player, index);
        if (data.allowItemsIn)
        {
            player.openInventory(this.getInventory(index), Cause.of(NamedCause.source(player)));
        }
        else
        {
            module.getInventoryGuardFactory().prepareInv(this.getInventory(index), player.getUniqueId()).
                blockPutInAll().submitInventory(module, true);
        }
    }

    public void showNextPage(Player player)
    {
        showPage(player, 1);
    }

    private void showPage(Player player, boolean next)
    {
        Integer index = viewers.get(player);
        if (data.pages == 1)
        {
            return;
        }
        player.closeInventory(Cause.of(NamedCause.source(player)));
        int newIndex;
        if (next)
        {
            if (index == data.pages -1)
            {
                newIndex = 0;
            }
            else
            {
                newIndex = index + 1;
            }
        }
        else
        {
            if (index == 0)
            {
                newIndex = data.pages - 1;
            }
            else
            {
                newIndex = index - 1;
            }
        }
        this.openInventory(newIndex, player);
    }

    public void showPrevPage(Player player)
    {
        showPage(player, false);
    }

    public void closeInventory(Player player)
    {
        Integer index = viewers.remove(player);
        BackpackHolder holder = views.get(index);
        if (index == null || holder == null)
        {
            return;
        }
        this.saveData(index, holder.getInventory());
        if (holder.getInventory().getViewers().isEmpty()
            || (holder.getInventory().getViewers().size() == 1
            && holder.getInventory().getViewers().get(0) == player))
        {
            this.views.remove(index);
        }
    }

    public void closeInventory()
    {
        viewers.keySet().forEach(p -> p.closeInventory(Cause.of(NamedCause.source(this)))); // TODO better cause

        // TODO save changes
        for (InventoryHolder holder : new ArrayList<>(this.views.values()))
        {
            for (HumanEntity humanEntity : new ArrayList<>(holder.getInventory().getViewers()))
            {
                this.saveData(this.viewers.remove((Player)humanEntity), holder.getInventory());
                humanEntity.closeInventory();
            }
        }
    }

    public void addItem(ItemStack toGive)
    {
        closeInventory();

        LinkedList<ItemStack> itemStacks = new LinkedList<>(Arrays.asList(InventoryUtil.splitIntoMaxItems(toGive, toGive.getMaxStackSize())));
        for (int i = 0 ; itemStacks.size() > 0; i++)
        {
            if (this.data.contents.get(i) == null)
            {
                this.data.contents.put(i, itemStacks.poll());
                if (i > this.data.pages * data.size * 9)
                {
                    this.data.pages = this.data.pages + 1;
                }
            }
        }
        this.data.save();
    }

    public boolean hasContext(Set<Context> set)
    {
        for (Context context : data.activeIn)
        {
            if (context.equals(ContextUtil.GLOBAL)) // TODO global context in service
            {
                return true;
            }
            if (set.contains(context))
            {
                return true;
            }
        }
        return false;
    }

}
