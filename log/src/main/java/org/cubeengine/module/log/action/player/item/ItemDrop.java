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
package org.cubeengine.module.log.action.player.item;

import org.cubeengine.libcube.service.user.User;
import org.cubeengine.module.log.LoggingConfiguration;
import org.cubeengine.module.log.action.BaseAction;
import org.cubeengine.module.log.action.block.entity.ActionEntityBlock.EntitySection;
import org.bukkit.entity.Item;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.text.Text;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.module.log.action.ActionCategory.ITEM;

/**
 * Represents a player dropping an item
 */
public class ItemDrop extends ActionItem
{
    public EntitySection entity;

    public ItemDrop()
    {
        super("drop", ITEM);
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return action instanceof ItemDrop && this.player.equals(((ItemDrop)action).player)
            && ((ItemDrop)action).item.isSimilar(this.item);
    }

    @Override
    public Text translateAction(User user)
    {
        int amount = this.item.getQuantity();
        if (this.hasAttached())
        {
            for (BaseAction action : this.getAttached())
            {
                amount += ((ItemDrop)action).item.getQuantity();
            }
        }
        return user.getTranslation(POSITIVE, "{user} dropped {name#item} x{amount}", this.player.name,
                                   this.item.getItem().getName(), amount);
    }

    public void setItem(Item item)
    {
        this.setItemstack(item.getItemData().getValue());
        this.entity = new EntitySection(item);
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.item.drop_manual;
    }

    // TODO chestDrop Action
    /*
    public void logDropsFromChest(InventoryHolder containerBlock, Location location, Player player)
    {
        ItemStack[] contents;
        if (containerBlock.getInventory() instanceof DoubleChestInventory)
        {
            DoubleChestInventory inventory = (DoubleChestInventory) containerBlock.getInventory();
            if (((Chest)inventory.getLeftSide().getHolder()).getLocation().equals(location))
            {
                contents = inventory.getLeftSide().getContents();
            }
            else
            {
                contents = inventory.getRightSide().getContents();
            }
        }
        else
        {
            contents = containerBlock.getInventory().getContents();
        }
        for (ItemStack itemStack : contents)
        {
            if (itemStack == null || itemStack.getType().equals(AIR))
            {
                continue;
            }
            String itemData = new ItemData(itemStack).serialize(this.om);
            this.logSimple(location,player,new ContainerType(containerBlock), itemData);
        }
    }
     */
    // TODO EntityDrop Action

    /*
        if (logEntry.getBlock() != null)
        {
            user.sendTranslated(MessageType.POSITIVE, "{}{user} let drop {amount} {name#item} from {name#container}{}", time, logEntry.getCauserUser().getDisplayName(), amount, logEntry.getItemData(), logEntry.getContainerTypeFromBlock(), loc);
        }
     */
    /*
        int amount;
        if (logEntry.hasAttached())
        {
            amount = logEntry.getItemData().amount;
            for (LogEntry entry : logEntry.getAttached())
            {
                amount += entry.getItemData().amount;
            }
        }
        else
        {
            amount = logEntry.getItemData().amount;
        }
     */
    /*
        else
        {
            if (logEntry.getBlock() != null)
            {
                user.sendTranslated(MessageType.POSITIVE, "{}{name#entity} let drop {amount} {name#item} from {name#container}{}", time, logEntry.getCauserEntity(), amount, logEntry.getItemData(), logEntry.getContainerTypeFromBlock(), loc);
            }
            else
            {
                user.sendTranslated(MessageType.POSITIVE, "{}{name#entity} dropped {amount} {name#item}{}", time, logEntry.getCauserEntity(), amount, logEntry.getItemData(), loc);
            }

        }
     */
}
