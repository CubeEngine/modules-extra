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
package org.cubeengine.module.log.action.player.item.container;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import com.mongodb.BasicDBObject;
import org.cubeengine.module.core.sponge.BukkitUtils;
import org.cubeengine.module.log.Log;
import org.cubeengine.module.log.action.LogListener;
import org.spongepowered.api.world.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Furnace;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.player.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.api.event.inventory.InventoryClickEvent;

import static org.bukkit.Material.*;

/**
 * A Listener for Player Actions with Items
 * <p>Events:
 * {@link InventoryCloseEvent}
 * {@link InventoryOpenEvent}
 * {@link InventoryClickEvent}
 * {@link InventoryDragEvent}
 * <p>All Actions:
 * {@link ItemInsert}
 * {@link ItemRemove}
 */
public class ListenerContainerItem extends LogListener
{
    private final Map<UUID, Map<ItemData, Integer>> inventoryChanges = new HashMap<>();

    public ListenerContainerItem(Log module)
    {
        super(module, ItemInsert.class, ItemRemove.class);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event)
    {
        if (event.getPlayer() instanceof Player)
        {
            Map<ItemData, Integer> itemDataMap = this.inventoryChanges.get(event.getPlayer().getUniqueId());
            if (itemDataMap != null)
            {
                Location location = this.getLocationForHolder(event.getInventory().getHolder());
                if (location == null)
                {
                    return;
                }
                for (ItemData itemData : itemDataMap.keySet())
                {
                    int amount = itemDataMap.get(itemData);
                    if (amount == 0)
                    {
                        continue;
                    }
                    ActionContainerItem action;
                    if (amount < 0)
                    {
                        amount *= -1;
                        action = this.newAction(ItemRemove.class);
                    }
                    else
                    {
                        action = this.newAction(ItemInsert.class);
                    }
                    ItemStack itemStack = itemData.toItemStack();
                    itemStack.setAmount(amount);

                    action.setLocation(this.getLocationForHolder(event.getInventory().getHolder()));
                    action.setItemstack(itemStack);
                    action.setPlayer((Player)event.getPlayer());
                    action.type = new ContainerType(event.getInventory().getHolder());

                    action.setTarget(new BasicDBObject());
                    action.save();
                    this.logAction(action);
                }
            }
            this.inventoryChanges.remove(event.getPlayer().getUniqueId());
        }
    }

    private Location getLocationForHolder(InventoryHolder holder)
    {
        if (holder instanceof Entity)
        {
            return ((Entity)holder).getLocation();
        }
        else if (holder instanceof DoubleChest)
        {
            //((Chest)inventory.getLeftSide().getHolder()).getLocation()
            return ((DoubleChest)holder).getLocation();
            //TODO get the correct chest
        }
        else if (holder instanceof BlockState)
        {
            return ((BlockState)holder).getLocation();
        }
        if (holder != null)
        {
            this.module.getLog().debug("Unknown InventoryHolder: {}", holder.getClass().getName());
        }
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event)
    {
        if (event.getPlayer() instanceof Player)
        {
            if (this.isActive(ItemInsert.class, event.getPlayer().getWorld())
             || this.isActive(ItemRemove.class, event.getPlayer().getWorld()))
            {
                /* TODO
                ContainerType type = new ContainerType(event.getInventory().getHolder());
                if (!config.container.CONTAINER_ignore.contains(type))
                 */
                this.inventoryChanges.put(event.getPlayer().getUniqueId(), new HashMap<ItemData, Integer>());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event)
    {
        if (event.getWhoClicked() instanceof Player)
        {
            if (!this.inventoryChanges.containsKey(event.getWhoClicked().getUniqueId()))
            {
                return;
            }
            Inventory inventory = event.getInventory();
            int amount = 0;
            for (Entry<Integer, ItemStack> entry : event.getNewItems().entrySet())
            {
                if (entry.getKey() < inventory.getSize())
                {
                    amount += entry.getValue().getAmount();
                }
            }
            this.prepareForLogging((Player)event.getWhoClicked(), new ItemData(event.getOldCursor()), amount);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event)
    {
        // TODO major refactoring needed
        if (event.getSlot() == -999)
        {
            return;
        }

        if (event.getWhoClicked() instanceof Player)
        {
            Player player = (Player)event.getWhoClicked();
            if (!this.inventoryChanges.containsKey(player.getUniqueId()))
            {
                return;
            }
            Inventory inventory = event.getInventory();
            InventoryHolder holder = inventory.getHolder();
            org.spongepowered.api.item.inventory.ItemStack inventoryItem = event.getCurrentItem();
            ItemStack cursorItem = event.getCursor();
            if ((inventoryItem == null || inventoryItem.getType() == AIR) && (cursorItem == null
                || cursorItem.getType() == AIR))
            {
                return; // nothing to log
            }
            if (event.getRawSlot() < event.getView().getTopInventory().getSize()) // click in top inventory
            {
                if (event.isShiftClick()) // top & shift -> remove items
                {
                    if (inventoryItem == null || inventoryItem.getType() == AIR)
                    {
                        return;
                    }
                    int missingSpace = getMissingSpace(event.getView().getTopInventory(), inventoryItem);
                    int amountTake = inventoryitem.getQuantity() - missingSpace;
                    if (amountTake > 0)
                    {
                        this.prepareForLogging(player, new ItemData(inventoryItem), -amountTake);
                    }
                }
                else
                {
                    if (cursorItem == null || cursorItem.getType() == AIR) // remove items
                    {
                        int remove = event.isLeftClick() ? inventoryitem.getQuantity() :
                                     (inventoryitem.getQuantity() + 1) / 2;
                        this.prepareForLogging(player, new ItemData(inventoryItem), -remove);
                    }
                    else if (inventoryItem == null || inventoryItem.getType() == AIR) // put items
                    {
                        int put = event.isLeftClick() ? cursoritem.getQuantity() : 1;
                        if (holder instanceof BrewingStand) // handle BrewingStands separatly
                        {
                            if (event.getRawSlot() == 3)
                            {
                                if (!BukkitUtils.canBePlacedInBrewingstand(cursorItem)) // can be put
                                {
                                    return;
                                }
                            }
                            else if (cursorItem.getType() == POTION
                                || cursorItem.getType() == GLASS_BOTTLE) // bottle slot
                            {
                                put = 1;
                            }
                            else
                            {
                                return;
                            }
                        }
                        this.prepareForLogging(player, new ItemData(cursorItem), put);
                    }
                    else
                    {
                        if (inventoryItem.isSimilar(cursorItem))
                        {
                            int put = event.isLeftClick() ? inventoryitem.getQuantity() : 1;
                            if (put > inventoryItem.getMaxStackSize() - inventoryitem.getQuantity()) //if stack to big
                            {
                                put = inventoryItem.getMaxStackSize()
                                    - inventoryitem.getQuantity(); //set to missing to fill
                            }
                            if (put == 0)
                            {
                                return;
                            }
                            this.prepareForLogging(player, new ItemData(inventoryItem), put);
                        }
                        else
                        {
                            if (holder instanceof BrewingStand) // handle BrewingStands separatly
                            {
                                if (event.getRawSlot() == 3)
                                {
                                    if (!BukkitUtils.canBePlacedInBrewingstand(cursorItem)) // can be put
                                    {
                                        return;
                                    }
                                }
                                else if (cursorItem.getType() == POTION
                                    || cursorItem.getType() == GLASS_BOTTLE) // bottle slot
                                {
                                    if (cursoritem.getQuantity() > 1)
                                    {
                                        return; // nothing happens when more than 1
                                    }
                                    // else swap items
                                }
                                else
                                {
                                    return;
                                }
                            }
                            this.prepareForLogging(player, new ItemData(cursorItem), cursoritem.getQuantity());
                            this.prepareForLogging(player, new ItemData(inventoryItem), -inventoryitem.getQuantity());
                        }
                    }
                }
            }
            else if (event.isShiftClick())// click in bottom inventory AND shift -> put | ELSE no container change
            {
                if (inventoryItem == null || inventoryItem.getType() == AIR)
                {
                    return;
                }
                if (holder instanceof BrewingStand)
                {
                    BrewerInventory brewerInventory = (BrewerInventory)event.getView().getTopInventory();
                    if (BukkitUtils.canBePlacedInBrewingstand(inventoryItem))
                    {
                        if (inventoryItem.isSimilar(brewerInventory.getIngredient())) // could fit into inventory
                        {
                            ItemStack brewerItem = brewerInventory.getIngredient();
                            int amountPutIn = inventoryitem.getQuantity();
                            if (breweritem.getQuantity() + inventoryitem.getQuantity() > inventoryItem.getMaxStackSize())
                            {
                                amountPutIn = inventoryItem.getMaxStackSize() - breweritem.getQuantity();
                                if (amountPutIn <= 0)
                                {
                                    return;
                                }
                            }
                            this.prepareForLogging(player, new ItemData(inventoryItem), amountPutIn);
                        }
                    }
                    else if (inventoryItem.getType() == POTION)
                    {
                        for (int i = 0; i <= 2; ++i)
                        {
                            ItemStack item = brewerInventory.getItem(i);
                            if (item == null) // space for a potion?
                            {
                                this.prepareForLogging(player, new ItemData(inventoryItem), inventoryitem.getQuantity());
                                return;
                            }
                            // else no space found
                        }
                    }
                    else if (inventoryItem.getType() == GLASS_BOTTLE)
                    {
                        int bottlesFound = 0;
                        int bottleSlots = 0;
                        for (int i = 0; i <= 2; ++i)
                        {
                            ItemStack item = brewerInventory.getItem(i);
                            if (item == null) // space for the stack ?
                            {
                                this.prepareForLogging(player, new ItemData(inventoryItem), inventoryitem.getQuantity());
                                return;
                            }
                            else if (item.getType() == GLASS_BOTTLE)
                            {
                                bottleSlots++;
                                bottlesFound += item.getQuantity();
                            }
                        }
                        if (bottleSlots > 0)
                        {
                            int space = GLASS_BOTTLE.getMaxStackSize() * bottleSlots - bottlesFound;
                            if (space <= 0)
                            {
                                return;
                            }
                            int putInto = inventoryitem.getQuantity();
                            if (putInto > space)
                            {
                                putInto = space;
                            }
                            this.prepareForLogging(player, new ItemData(inventoryItem), putInto);
                        }
                    }
                }
                else if (holder instanceof Furnace)
                {
                    FurnaceInventory furnaceInventory = (FurnaceInventory)event.getView().getTopInventory();
                    int putInto = 0;
                    if (BukkitUtils.isSmeltable(inventoryItem))
                    {
                        ItemStack item = furnaceInventory.getSmelting();
                        if (item == null)
                        {
                            putInto = inventoryitem.getQuantity();
                        }
                        else if (inventoryItem.isSimilar(item))
                        {
                            int space = inventoryItem.getMaxStackSize() - item.getQuantity();
                            if (space <= 0)
                            {
                                return;
                            }
                            putInto = inventoryitem.getQuantity();
                            if (putInto > space)
                            {
                                putInto = space;
                            }
                        }
                    }
                    else if (inventoryItem.getItem().getDefaultProperty(BurningFuelProperty.class).isPresent())
                    {
                        ItemStack item = furnaceInventory.getFuel();
                        if (item == null)
                        {
                            putInto = inventoryitem.getQuantity();
                        }
                        else if (inventoryItem.isSimilar(item))
                        {
                            int space = inventoryItem.getMaxStackSize() - item.getQuantity();
                            if (space <= 0)
                            {
                                return;
                            }
                            putInto = inventoryitem.getQuantity();
                            if (putInto > space)
                            {
                                putInto = space;
                            }
                        }
                    }
                    if (putInto == 0)
                    {
                        return;
                    }
                    this.prepareForLogging(player, new ItemData(inventoryItem), putInto);
                }
                else
                {
                    event.getView().getTopInventory().getContents();

                    int missingSpace = getMissingSpace(event.getView().getTopInventory(), inventoryItem);
                    int amountPut = inventoryitem.getQuantity() - missingSpace;
                    if (amountPut > 0)
                    {
                        this.prepareForLogging(player, new ItemData(inventoryItem), amountPut);
                    }
                }
            }
        }
    }

    private void prepareForLogging(Player player, ItemData itemData, int amount)
    {
        Map<ItemData, Integer> itemDataMap = this.inventoryChanges.get(player.getUniqueId());
        if (itemDataMap == null)
        {
            itemDataMap = new HashMap<>();
            this.inventoryChanges.put(player.getUniqueId(), itemDataMap);
        }
        Integer oldAmount = itemDataMap.get(itemData); // if not yet set this returns 0
        itemDataMap.put(itemData, oldAmount == null ? 0 : oldAmount + amount);
    }
}
