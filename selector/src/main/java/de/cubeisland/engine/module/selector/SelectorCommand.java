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
package de.cubeisland.engine.module.selector;

import com.google.common.base.Optional;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.result.CommandResult;
import de.cubeisland.engine.service.command.CommandContext;
import de.cubeisland.engine.service.user.User;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.manipulator.DisplayNameData;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;

import static de.cubeisland.engine.service.i18n.formatter.MessageType.NEGATIVE;
import static de.cubeisland.engine.service.i18n.formatter.MessageType.POSITIVE;

public class SelectorCommand
{
    private Game game;

    public SelectorCommand(Game game)
    {
        this.game = game;
    }

    public void giveSelectionTool(User user)
    {
        ItemStack found = null;
        // TODO wait for implemented InventoryAPI
        /*
        Inventory axes = user.getInventory().query(ItemTypes.WOODEN_AXE);
        for (Inventory slot : axes.slots())
        {
            ItemStack itemStack = slot.peek().get();
            if (itemStack.getData(DisplayNameData.class).isPresent())
            {
                if (SELECTOR_TOOL_NAME.equals(itemStack.getData(DisplayNameData.class).get().getDisplayName()))
                {
                    found = itemStack;
                    slot.clear();
                    break;
                }
            }
        }
        */
        Optional<ItemStack> itemInHand = user.asPlayer().getItemInHand();
        if (found == null)
        {
            found = game.getRegistry().getItemBuilder().itemType(ItemTypes.WOODEN_AXE).quantity(1).build();
            DisplayNameData display = found.getOrCreate(DisplayNameData.class).get();
            display.setDisplayName(Texts.of(TextColors.BLUE, "Selector-Tool"));
            found.offer(display);
            /* TODO wait for impl
            LoreData lore = found.getOrCreate(LoreData.class).get();
            lore.set(Texts.of("created by ", user.getDisplayName()));
            found.offer(lore);
            */

            user.asPlayer().setItemInHand(found);
            if (itemInHand.isPresent())
            {
                if (!user.asPlayer().getInventory().offer(itemInHand.get()))
                {
                    // TODO drop item
                }
            }
            user.sendTranslated(POSITIVE, "Received a new region selector tool");
            return;
        }

        user.asPlayer().setItemInHand(found);
        if (itemInHand.isPresent())
        {
            user.asPlayer().getInventory().offer(itemInHand.get());
        }
        user.sendTranslated(POSITIVE, "Found a region selector tool in your inventory!");
    }

    @Command(desc = "Provides you with a wand to select a cuboid")
    public CommandResult selectiontool(CommandContext context)
    {
        if (context.getSource() instanceof User)
        {
            giveSelectionTool((User)context.getSource());
        }
        else
        {
            context.sendTranslated(NEGATIVE, "You cannot hold a selection tool!");
        }
        return null;
    }
}
