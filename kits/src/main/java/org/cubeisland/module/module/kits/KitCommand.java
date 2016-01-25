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
package org.cubeisland.module.module.kits;

import java.util.ArrayList;
import java.util.List;
import de.cubeisland.engine.butler.CommandInvocation;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.command.CommandContext;
import org.cubeengine.service.command.CommandSender;
import org.cubeengine.service.user.User;
import org.cubeengine.module.core.util.FileUtil;
import org.spongepowered.api.item.inventory.ItemStack;

import org.cubeengine.module.core.util.ChatFormat.WHITE;
import org.cubeengine.module.core.util.ChatFormat.YELLOW;
import static org.bukkit.Material.AIR;

@Command(name = "kit", desc = "Manages item-kits")
public class KitCommand extends ContainerCommand
{
    private final KitManager manager;
    private final Kits module;

    public KitCommand(Kits module)
    {
        super(module);
        this.module = module;
        this.manager = module.getKitManager();
    }

    @Override
    protected boolean selfExecute(CommandInvocation invocation)
    {
        if (invocation.tokens().size() - invocation.consumed() >= 1)
        {
            return this.getCommand("give").execute(invocation);
        }
        return super.selfExecute(invocation);
    }

    @Command(desc = "Creates a new kit with the items in your inventory.")
    @Restricted(value = User.class, msg = "Just log in or use the config!")
    public void create(User context, String kitname, @Flag boolean toolbar)
    {
        List<KitItem> itemList = new ArrayList<>();
        if (toolbar)
        {
            ItemStack[] items = context.getInventory().getContents();
            for (int i = 0; i <= 8; ++i)
            {
                if (items[i] == null || items[i].getType() == AIR)
                {
                    break;
                }
                itemList.add(
                        new KitItem(items[i].getType(),
                            items[i].getDurability(),
                            items[i].getAmount(),
                            items[i].getItemMeta().getDisplayName(),
                            items[i].getEnchantments()));
            }
        }
        else
        {
            for (ItemStack item : context.getInventory().getContents())
            {
                if (item == null || item.getTypeId() == 0)
                {
                    break;
                }
                itemList.add(
                        new KitItem(item.getType(),
                            item.getDurability(),
                            item.getQuantity(),
                            item.getItemMeta().getDisplayName(),
                            item.getEnchantments()));
            }
        }
        Kit kit = new Kit(module, kitname, false, 0, -1, true, "", new ArrayList<>(), itemList);
        if (!FileUtil.isValidFileName(kit.getKitName()))
        {
            context.sendTranslated(NEGATIVE, "{name#kit} is is not a valid name! Do not use characters like *, | or ?", kit.getKitName());
            return;
        }
        manager.saveKit(kit);
        if (kit.getPermission() != null)
        {
            module.getCore().getPermissionManager().registerPermission(module, kit.getPermission());
        }
        context.sendTranslated(POSITIVE, "Created the {name#kit} kit!", kit.getKitName());
    }


    @Alias(value = "kitlist")
    @Command(desc = "Lists all currently available kits.")
    public void list(CommandContext context)
    {
        context.sendTranslated(POSITIVE, "The following kits are available:");
        String format = "  " + WHITE + "-" + YELLOW;
        for (String kitName : manager.getKitsNames())
        {
            context.sendMessage(format + kitName);
        }
    }

    @Command(desc = "Gives a set of items.")
    public void give(CommandSender context, String kitname, @Default User player, @Flag boolean all, @Flag boolean force)
    {
        Kit kit = manager.getKit(kitname);
        force = force && module.perms().COMMAND_KIT_GIVE_FORCE.isAuthorized(context);
        if (kit == null)
        {
            context.sendTranslated(NEGATIVE, "Kit {input} not found!", kitname);
            return;
        }
        // TODO extract to giveall cmd
        if (all)
        {
            boolean gaveKit = false;
            int kitNotreceived = 0;
            for (User receiver : module.getCore().getUserManager().getOnlineUsers())
            {
                try
                {
                    if (kit.give(context, receiver, force))
                    {
                        if (receiver.equals(context))
                        {
                            context.sendTranslated(POSITIVE, "Received the {name#kit} kit!", kit.getKitName());
                        }
                        else
                        {
                            context.sendTranslated(POSITIVE, "You gave {user} the {name#kit} kit!", receiver, kit.getKitName());
                            receiver.sendTranslated(POSITIVE, "Received the {name#kit} kit. Enjoy.", kit.getKitName());
                        }
                        gaveKit = true;
                    }
                }
                catch (Exception ex)
                {
                    kitNotreceived++;
                }
            }
            if (!gaveKit)
            {
                context.sendTranslated(NEGATIVE, "No one received the kit!");
            }
            else if (kitNotreceived > 0)
            {
                context.sendTranslated(NEGATIVE, "{amount} players did not receive a kit!", kitNotreceived); // TODO Have a string for if there is only one player, so non-plural
            }
            return;
        }
        boolean other = false;
        User user;
        if (player != null)
        {
            user = player;
            other = true;
        }
        else if (context instanceof User)
        {
            user = (User)context;
        }
        else
        {
            context.sendTranslated(NEGATIVE, "You need to specify a player!");
            return;
        }
        if (!user.isOnline())
        {
            context.sendTranslated(NEGATIVE, "{user} is not online!", user.getDisplayName());
            return;
        }
        if (kit.give(context, user, force))
        {
            if (other)
            {
                context.sendTranslated(POSITIVE, "You gave {user} the {name#kit} kit!", user, kit.getKitName());
                if (kit.getCustomMessage().isEmpty())
                {
                    user.sendTranslated(POSITIVE, "Received the {name#kit} kit. Enjoy.", kit.getKitName());
                    return;
                }
                user.sendMessage(kit.getCustomMessage());
                return;
            }
            if (kit.getCustomMessage().isEmpty())
            {
                context.sendTranslated(POSITIVE, "Received the {name#kit} kit. Enjoy.", kit.getKitName());
                return;
            }
            context.sendMessage(kit.getCustomMessage());
            return;
        }
        if (other)
        {
            context.sendTranslated(NEUTRAL, "{user} has not enough space in your inventory for this kit!", user);
            return;
        }
        context.sendTranslated(NEUTRAL, "You don't have enough space in your inventory for this kit!");
    }
}
