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
package org.cubeengine.module.writer;

import java.util.Optional;
import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Disable;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.manipulator.mutable.item.PagedData;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.block.BlockTypes.STANDING_SIGN;
import static org.spongepowered.api.block.BlockTypes.WALL_SIGN;
import static org.spongepowered.api.item.ItemTypes.WRITABLE_BOOK;
import static org.spongepowered.api.item.ItemTypes.WRITTEN_BOOK;

/**
 * A module to edit signs and signed books
 */
@ModuleInfo(name = "Writer", description = "Edit signs and books")
public class Writer extends Module
{
    @Inject private CommandManager cm;
    @Inject private I18n i18n;

    @Enable
    public void onEnable()
    {
        cm.addCommands(cm, this, this);
    }

    @Disable
    public void onDisable()
    {
    }

    @Command(alias = "rewrite", desc = "Edit a sign or a signed book")
    @Restricted(value = Player.class, msg = "Edit what?")
    public void edit(Player context,
                     @Named({"1", "Line1"}) @Label("1st line") String line1,
                     @Named({"2", "Line2"}) @Label("2nd line") String line2,
                     @Named({"3", "Line3"}) @Label("3rd line") String line3,
                     @Named({"4", "Line4"}) @Label("4th line") String line4)
    {
        if (line1 == null && line2 == null && line3 == null && line4 == null)
        {
            if (!this.editBookInHand(context))
            {
                i18n.sendTranslated(context, NEGATIVE, "You need to specify at least one parameter to edit a sign!");
                i18n.sendTranslated(context, NEGATIVE, "Or hold a signed book in your hand to edit it.");
            }
            return;
        }
        if (!this.editSignInSight(context, line1, line2, line3, line4))
        {
            i18n.sendTranslated(context, NEGATIVE, "You need to be looking at a sign less than {amount} blocks away!", 10);
            i18n.sendTranslated(context, NEGATIVE, "Or hold a signed book in your hand to edit it.");
        }
    }

    /**
     * Makes a signed book in the hand of given user editable
     *
     * @param user the user
     *
     * @return false if there is no written book in the hand of given user
     */
    public boolean editBookInHand(Player user)
    {
        Optional<ItemStack> oItem = user.getItemInHand(HandTypes.MAIN_HAND);
        if (!oItem.isPresent() || oItem.get().getItem() != WRITTEN_BOOK)
        {
            return false;
        }

        ItemStack item = oItem.get();
        PagedData pages = item.get(PagedData.class).get();
        item = ItemStack.builder().itemType(WRITABLE_BOOK).itemData(pages).build();
        user.setItemInHand(HandTypes.MAIN_HAND, item);
        i18n.sendTranslated(user, POSITIVE, "Your book is now unsigned and ready to be edited.");
        return true;
    }

    /**
     * Edits the sign the user is looking at
     *
     * @param user  the user
     * @param line1 the 1st line
     * @param line2 the 2nd line
     * @param line3 the 3rd line
     * @param line4 the 4th line
     *
     * @return false if there is no sign to edit in sight
     */
    public boolean editSignInSight(Player user, String line1, String line2, String line3, String line4)
    {
        Optional<BlockRayHit<World>> end = BlockRay.from(user.getLocation().add(0,1.62,0)).filter(BlockRay.onlyAirFilter()).blockLimit(10).end();
        if (!end.isPresent())
        {
            return false;
        }
        Location<World> block = end.get().getLocation();
        BlockType type = block.getBlockType();
        if (type != WALL_SIGN && type != STANDING_SIGN)
        {
            return false;
        }
        SignData signData = block.get(SignData.class).get();
        ListValue<Text> lines = signData.lines();
        lines.set(0, line1 == null ? lines.get(0) : Text.of(line1));
        lines.set(1, line2 == null ? lines.get(1) : Text.of(line2));
        lines.set(2, line3 == null ? lines.get(2) : Text.of(line3));
        lines.set(3, line4 == null ? lines.get(3) : Text.of(line4));
        block.offer(signData);

        i18n.sendTranslated(user, POSITIVE, "The sign has been changed!");
        return true;
    }
}
