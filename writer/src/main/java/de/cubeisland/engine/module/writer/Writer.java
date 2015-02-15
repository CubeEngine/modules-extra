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
package de.cubeisland.engine.module.writer;

import java.util.Collections;
import de.cubeisland.engine.command.filter.Restricted;
import de.cubeisland.engine.command.parametric.Command;
import de.cubeisland.engine.command.parametric.Label;
import de.cubeisland.engine.command.parametric.Named;
import de.cubeisland.engine.core.module.Module;
import de.cubeisland.engine.core.user.User;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import static de.cubeisland.engine.core.util.formatter.MessageType.NEGATIVE;
import static de.cubeisland.engine.core.util.formatter.MessageType.POSITIVE;
import static org.bukkit.Material.SIGN_POST;
import static org.bukkit.Material.WALL_SIGN;

/**
 * A module to edit signs and/or unsign written books
 */
public class Writer extends Module
{
    @Override
    public void onEnable()
    {
        this.getCore().getCommandManager().addCommands(this.getCore().getCommandManager(), this, this);
    }

    @Command(alias = "rewrite", desc = "Edit a sign or unsign a book")
    @Restricted(value = User.class, msg = "Edit what?")
    public void edit(User context,
                     @Named({"1", "Line1"}) @Label("1st line") String line1,
                     @Named({"2", "Line2"}) @Label("2st line") String line2,
                     @Named({"3", "Line3"}) @Label("3st line") String line3,
                     @Named({"4", "Line4"}) @Label("4st line") String line4)

    {
        if (!this.unsignBook(context))
        {
            if (line1 == null && line2 == null && line3 == null && line4 == null)
            {
                context.sendTranslated(NEGATIVE, "You need to specify at least one parameter to edit a sign!");
                context.sendTranslated(NEGATIVE, "Or hold a signed book in your hand to edit it.");
                return;
            }
            if (!this.editSignInSight(context, line1, line2, line3, line4))
            {
                context.sendTranslated(NEGATIVE,
                                    "You need to have a signed book in hand or be looking at a sign less than 10 blocks away!");
            }
        }
    }

    /**
     * Edits the sign the user is looking at
     *
     * @param user the user
     * @param line1 the 1st line
     * @param line2 the 2nd line
     * @param line3 the 3rd line
     * @param line4 the 4th line
     * @return false if there is no sign
     */
    public boolean editSignInSight(User user, String line1, String line2, String line3, String line4)
    {
        Block target = user.getTargetBlock(Collections.<Material>emptySet(), 10);
        if (target.getType() == WALL_SIGN || target.getType() == SIGN_POST)
        {
            Sign sign = (Sign)target.getState();
            String[] lines = sign.getLines();
            lines[0] = line1 == null ? lines[0] : line1;
            lines[1] = line2 == null ? lines[1] : line2;
            lines[2] = line3 == null ? lines[2] : line3;
            lines[3] = line4 == null ? lines[3] : line4;
            SignChangeEvent event = new SignChangeEvent(sign.getBlock(), user, lines);
            user.getCore().getEventManager().fireEvent(event);
            if (event.isCancelled())
            {
                user.sendTranslated(NEGATIVE, "Could not change the sign!");
                return true;
            }
            for (int i = 0; i < 4; ++i)
            {
                sign.setLine(i, lines[i]);
            }
            sign.update();
            user.sendTranslated(POSITIVE, "The sign has been changed!");
            return true;
        }
        // No Sign in sight
        return false;
    }

    /**
     * Unsigns a written book in the hand of given user
     *
     * @param user the user
     * @return false if there is no written book in the hand of given user
     */
    public boolean unsignBook(User user)
    {
        if (user.getItemInHand().getType() == Material.WRITTEN_BOOK)
        {
            ItemStack item = user.getItemInHand();
            BookMeta meta = ((BookMeta)item.getItemMeta());
            meta.setAuthor("");
            meta.setTitle("");
            item.setItemMeta(meta);
            item.setType(Material.BOOK_AND_QUILL);
            user.sendTranslated(POSITIVE, "Your book is now unsigned and ready to be edited.");
            return true;
        }
        return false;
    }
}
