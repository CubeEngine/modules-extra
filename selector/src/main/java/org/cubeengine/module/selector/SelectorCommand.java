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
package org.cubeengine.module.selector;

import java.util.Arrays;
import java.util.Optional;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.result.CommandResult;
import org.cubeengine.service.command.CommandContext;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.data.key.Keys.DISPLAY_NAME;
import static org.spongepowered.api.data.key.Keys.ITEM_LORE;
import static org.spongepowered.api.item.ItemTypes.WOODEN_AXE;

public class SelectorCommand
{
    private Game game;
    private I18n i18n;

    public SelectorCommand(Game game, I18n i18n)
    {
        this.game = game;
        this.i18n = i18n;
    }

    public void giveSelectionTool(Player user)
    {
        ItemStack found = null;
/* TODO wait for inventory impl
        Inventory axes = user.getInventory().query(ItemTypes.WOODEN_AXE);
        for (Inventory slot : axes.slots())
        {
            ItemStack itemStack = slot.peek();
            Optional<Text> display = itemStack.get(Keys.DISPLAY_NAME);
            if (display.isPresent())
            {
                if (Texts.of(TextColors.BLUE, "Selector-Tool").equals(display.get()))
                {
                    found = itemStack;
                    slot.clear();
                    break;
                }
            }
        }
*/
        Optional<ItemStack> itemInHand = user.getItemInHand();
        if (found == null)
        {
            found = game.getRegistry().createBuilder(ItemStack.Builder.class).itemType(WOODEN_AXE).quantity(1).build();
            found.offer(DISPLAY_NAME, Texts.of(TextColors.BLUE, "Selector-Tool"));
            found.offer(ITEM_LORE, Arrays.asList(Texts.of("created by ", user.getName())));

            user.setItemInHand(found);
            if (itemInHand.isPresent())
            {
                if (user.getInventory().offer(itemInHand.get()).getType() != InventoryTransactionResult.Type.SUCCESS)
                {
                    // TODO drop item
                }
            }
            i18n.sendTranslated(user, POSITIVE, "Received a new region selector tool");
            return;
        }

        user.setItemInHand(found);
        if (itemInHand.isPresent())
        {
            user.getInventory().offer(itemInHand.get());
        }
        i18n.sendTranslated(user, POSITIVE, "Found a region selector tool in your inventory!");
    }

    @Command(desc = "Provides you with a wand to select a cuboid")
    public CommandResult selectiontool(CommandContext context)
    {
        if (context.getSource() instanceof Player)
        {
            giveSelectionTool((Player)context.getSource());
        }
        else
        {
            context.sendTranslated(NEGATIVE, "You cannot hold a selection tool!");
        }
        return null;
    }
}