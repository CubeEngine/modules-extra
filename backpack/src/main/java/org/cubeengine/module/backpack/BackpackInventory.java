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

import static org.spongepowered.api.item.inventory.InventoryArchetypes.CHEST;

import org.cubeengine.libcube.util.ChatFormat;
import org.cubeengine.libcube.util.ContextUtil;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.context.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BackpackInventory
{
    protected final Backpack module;
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
        Inventory inv = Inventory.builder().of(CHEST).build(module.getPlugin());
        // TODO create custom inventory that is a CarriedInventory .withCarrier()
        // TODO size

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
            contents[i] = itemStack == null ? null : itemStack.copy();
        }

        int i = 0;
        for (Inventory slot : holder.getInventory())
        {
            if (contents[i] != null)
            {
                slot.set(contents[i]);
            }
        }

        return holder.getInventory();
    }

    private void saveData(int index, Inventory inventory)
    {
        int offset = index * data.size * 9;
        ItemStack[] contents = new ItemStack[data.size];
        int i = 0;
        for (Inventory slot : inventory)
        {
            i++;
            if (slot.peek().isPresent())
            {
                 data.contents.put(offset + i, slot.peek().get());
            }
            else
            {
                data.contents.remove(offset + i);
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
                blockPutInAll().submitInventory(Backpack.class, true);
        }
    }

    public void showNextPage(Player player)
    {
        showPage(player, true);
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

        if (((Container)holder.getInventory()).getViewers().isEmpty()
            || (((Container)holder.getInventory()).getViewers().size() == 1
            && ((Container)holder.getInventory()).getViewers().iterator().next() == player))
        {
            this.views.remove(index);
        }
    }

    public void closeInventory()
    {
        Cause cause = Cause.of(NamedCause.source(this));// TODO better cause
        viewers.keySet().forEach(p -> p.closeInventory(cause));

        for (BackpackHolder holder : views.values())
        {
            for (Player player : ((Container)holder).getViewers())
            {
                player.closeInventory(cause);
                saveData(viewers.remove(player), holder.getInventory());
            }
        }
    }

    public void addItem(ItemStack toGive)
    {
        closeInventory();

        for (int i = 0; i < data.pages; i++)
        {
            Inventory inventory = getInventory(0);
            inventory.offer(toGive);
            saveData(0, inventory);
            if (toGive.getQuantity() == 0)
            {
                break;
            }
        }
        if (toGive.getQuantity() != 0)
        {
            throw new IllegalStateException();
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
