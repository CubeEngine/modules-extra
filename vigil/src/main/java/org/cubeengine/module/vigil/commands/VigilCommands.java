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
package org.cubeengine.module.vigil.commands;

import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.matcher.StringMatcher;
import org.cubeengine.module.vigil.Vigil;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import static java.util.Arrays.asList;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Command(name = "vigil", alias = "log", desc = "Vigil-Module Commands")
public class VigilCommands extends ContainerCommand
{
    public static final Text toolName = Text.of(TextColors.DARK_AQUA, "Logging-ToolBlock");
    public static final Text selectorToolName = Text.of(TextColors.DARK_AQUA, "Selector-Tool");

    private StringMatcher sm;
    private I18n i18n;
    private Game game;

    public VigilCommands(StringMatcher sm, I18n i18n, Game game, CommandManager cm)
    {
        super(cm, Vigil.class);
        this.sm = sm;
        this.i18n = i18n;
        this.game = game;
    }

    @Alias(value = "lb")
    @Command(desc = "Gives you a block to check logs with." +
        "no log-type: Shows everything\n" +
        "chest: Shows chest-interactions only\n" +
        "player: Shows player-interacions only\n" +
        "kills: Shows kill-interactions only\n" +
        "block: Shows block-changes only")
    @Restricted(value = Player.class, msg = "Why don't you check in your log-file? You won't need a block there!")
    public void block(Player context, @Optional @Label("log-type") String logType)
    {
        //TODO tabcompleter for logBlockTypes
        ItemType blockMaterial = this.matchType(logType, true);
        if (blockMaterial == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "{input} is not a valid log-type. Use chest, container, player, block or kills instead!", logType);
            
            return;
        }
        findLogTool(context, blockMaterial);
    }

    @Alias(value = "lt")
    @Command(desc = "Gives you an item to check logs with.\n" +
        "no log-type: Shows everything\n" +
        "chest: Shows chest-interactions only\n" +
        "player: Shows player-interacions only\n" +
        "kills: Shows kill-interactions only\n" +
        "block: Shows block-changes only")
    @Restricted(value = Player.class, msg = "Why don't you check in your log-file? You won't need an item there!")
    public void tool(Player context, @Optional @Label("log-type") String logType)
    {
        //TODO tabcompleter for logToolTypes
        ItemType blockMaterial = this.matchType(logType, false);
        if (blockMaterial == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "{input} is not a valid log-type. Use chest, container, player, block or kills instead!", logType);
            return;
        }
        findLogTool(context, blockMaterial);
    }

    private ItemType matchType(String type, boolean block)// or item
    {
        if (type == null)
        {
            return block ? ItemTypes.BEDROCK : ItemTypes.BOOK;
        }
        String match = sm.matchString(type, "chest", "player", "kills", "block");
        if (match != null)
        {
            switch (match)
            {
                case "chest":
                case "container":
                    return block ? ItemTypes.CHEST : ItemTypes.BRICK;
                case "player":
                    return block ? ItemTypes.PUMPKIN : ItemTypes.CLAY_BALL;
                case "kills":
                    return block ? ItemTypes.SOUL_SAND : ItemTypes.BONE;
                case "block":
                    return block ? ItemTypes.LOG : ItemTypes.NETHERBRICK;
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private void findLogTool(Player player, ItemType type)
    {
        ItemStack itemStack = game.getRegistry().createBuilder(ItemStack.Builder.class).itemType(type).quantity(1).build();
        itemStack.offer(Keys.DISPLAY_NAME, toolName);
        itemStack.offer(Keys.ITEM_LORE, asList(i18n.getTranslation(player, NONE, "created by {name}", player.getName())));
        player.setItemInHand(itemStack);
        // TODO search in inventory
        // TODO put item in hand back into inventory
        i18n.sendTranslated(player, POSITIVE, "Received a new Log-Tool!");
        // TODO LookupAttachment?
    }
}
