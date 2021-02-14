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
package org.cubeengine.module.vigil.commands;

import java.util.Collections;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Label;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.command.annotation.Using;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.matcher.StringMatcher;
import org.cubeengine.libcube.util.SpawnUtil;
import org.cubeengine.module.vigil.data.LookupData;
import org.cubeengine.module.vigil.data.VigilData;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;

import static java.util.Arrays.asList;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Singleton
@Using(LookupDataParser.class)
@Command(name = "vigil", alias = "log", desc = "Vigil-Module Commands")
public class VigilCommands extends DispatcherCommand
{
    public static final Component toolName = Component.text("Vigil Log-Tool", NamedTextColor.DARK_AQUA);

    private StringMatcher sm;
    private I18n i18n;

    @Inject
    public VigilCommands(VigilAdminCommands adminCommands, VigilLookupCommands lookupCommands, StringMatcher sm, I18n i18n)
    {
        super(adminCommands, lookupCommands);
        this.sm = sm;
        this.i18n = i18n;
    }

    @Alias(value = "lb")
    @Command(desc = "Gives you a block to check logs with.")
    /*
        "no log-type: Shows everything\n" +
        "chest: Shows chest-interactions only\n" +
        "player: Shows player-interactions only\n" +
        "kills: Shows kill-interactions only\n" +
        "block: Shows block-changes only
     */
    @Restricted(msg = "Why don't you check in your log-file? You won't need a block there!")
    public void block(ServerPlayer context, @Default @Label("log-type") LookupData logType)
    {
        findLogTool(context, logType);
    }

    @Alias(value = "lt")
    @Command(desc = "Gives you an item to check logs with.")
    /*
        "no log-type: Shows everything\n" +
        "chest: Shows chest-interactions only\n" +
        "player: Shows player-interactions only\n" +
        "kills: Shows kill-interactions only\n" +
        "block: Shows block-changes only
     */
    @Restricted(msg = "Why don't you check in your log-file? You won't need an item there!")
    public void tool(ServerPlayer context, @Default @Label("log-type") LookupData logType)
    {
        findLogTool(context, logType);
    }

    private ItemType matchType(String type, boolean block)// or item
    {
        if (type == null)
        {
            return block ? ItemTypes.BEDROCK.get() : ItemTypes.BOOK.get();
        }
        String match = sm.matchString(type, "chest", "player", "kills", "block");
        if (match != null)
        {
            switch (match)
            {
                case "chest":
                case "container":
                    return block ? ItemTypes.CHEST.get() : ItemTypes.BRICK.get();
                case "player":
                    return block ? ItemTypes.CARVED_PUMPKIN.get() : ItemTypes.CLAY_BALL.get();
                case "kills":
                    return block ? ItemTypes.SOUL_SAND.get() : ItemTypes.BONE.get();
                case "block":
                    return block ? ItemTypes.OAK_LOG.get() : ItemTypes.NETHER_BRICK.get();
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private void findLogTool(ServerPlayer player, LookupData data)
    {

        ItemStack itemStack = ItemStack.of(ItemTypes.BOOK);
        itemStack.offer(Keys.CUSTOM_NAME, toolName);
        itemStack.offer(Keys.LORE, asList(i18n.translate(player, "created by {name}", player.getName())));
        itemStack.offer(Keys.APPLIED_ENCHANTMENTS, Collections.emptyList());
        VigilData.syncToStack(itemStack, data);
        ItemStack inHand = player.getItemInHand(HandTypes.MAIN_HAND);
        player.setItemInHand(HandTypes.MAIN_HAND, itemStack);
        if (!inHand.isEmpty())
        {
            if (player.getInventory().offer(inHand).getType() != InventoryTransactionResult.Type.SUCCESS)
            {
                SpawnUtil.spawnItem(inHand, player.getServerLocation());
            }
        }
        i18n.send(player, POSITIVE, "Received a new Log-Tool!");
    }
}
