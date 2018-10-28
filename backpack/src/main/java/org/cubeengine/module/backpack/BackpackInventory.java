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
package org.cubeengine.module.backpack;

import org.cubeengine.libcube.util.ContextUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.context.Context;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BackpackInventory
{
    protected final Backpack module;
    protected BackpackData data;
    private String name;

    private final Set<UUID> viewers = new HashSet<>();
    private BackpackHolder view;
    
    public BackpackInventory(Backpack module, BackpackData data, String name)
    {
        this.module = module;
        this.data = data;
        this.name = name;
    }

    private Inventory getInventory()
    {
        if (view == null)
        {
            view = new BackpackHolder(this, "Backpack " + name);
        }

        int i = 0;
        for (Inventory slot : view.getInventory().slots())
        {
            ItemStack itemStack = data.contents.get(i);
            if (itemStack != null)
            {
                slot.set(itemStack.copy());
            }
            i++;
        }

        return view.getInventory();
    }

    private void saveData(Inventory inventory)
    {
        int i = 0;
        for (Inventory slot : inventory.slots())
        {
            if (slot.peek().isPresent())
            {
                 data.contents.put(i, slot.peek().get());
            }
            else
            {
                data.contents.remove(i);
            }
            i++;
        }
        data.save();
    }

    public void openInventory(Player player)
    {
        this.viewers.add(player.getUniqueId());
        if (data.allowItemsIn)
        {
            Sponge.getCauseStackManager().pushCause(player);
            player.openInventory(this.getInventory());
            Sponge.getCauseStackManager().popCause();
        }
        else
        {
            module.getInventoryGuardFactory().prepareInv(this.getInventory(), player.getUniqueId()).
                blockPutInAll().submitInventory(Backpack.class, true);
        }
    }

    public void closeInventory(Container container, Player player)
    {
        viewers.remove(player.getUniqueId());
        if (view != null)
        {
            this.saveData(view.getInventory());
        }
        if (viewers.isEmpty())
        {
            view = null;
        }
    }

    public void closeInventory()
    {
        viewers.stream().map(id -> Sponge.getServer().getPlayer(id)).forEach(p -> p.ifPresent(Player::closeInventory));

        for (Player player : ((Container) view.getInventory()).getViewers())
        {
            player.closeInventory();
        }
        this.saveData(view.getInventory());
    }

    public void addItem(ItemStack toGive)
    {
        closeInventory();

        Inventory inventory = getInventory();
        inventory.offer(toGive);
        saveData(inventory);
        if (toGive.getQuantity() != 0)
        {
            throw new IllegalStateException();
        }
        this.data.save();
    }

    public String getName()
    {
        return name;
    }
}
