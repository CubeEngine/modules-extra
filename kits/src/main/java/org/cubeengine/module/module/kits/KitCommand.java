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
package org.cubeengine.module.module.kits;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Delegate;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.ParameterPermission;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.command.annotation.Using;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.inventoryguard.InventoryGuardFactory;
import org.cubeengine.libcube.util.ChatFormat;
import org.cubeengine.libcube.util.FileUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.ContainerTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.item.inventory.type.ViewableInventory;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Singleton
@Delegate("give")
@Command(name = "kit", desc = "Manages kits")
@Using(KitParser.class)
public class KitCommand extends DispatcherCommand
{
    private final KitManager manager;
    private final Kits module;
    private I18n i18n;
    private InventoryGuardFactory igf;

    @Inject
    public KitCommand(Kits module, I18n i18n, InventoryGuardFactory igf, KitEditCommand editCommand)
    {
        super(Kits.class, editCommand);
        this.module = module;
        this.i18n = i18n;
        this.igf = igf;
        this.manager = module.getKitManager();
    }

    @Command(alias = "remove", desc = "Deletes a kit")
    public void delete(CommandCause context, Kit kit)
    {
        manager.deleteKit(kit);
        i18n.send(context.getAudience(), POSITIVE, "Kit deleted.");
    }

    @Command(alias = "open", desc = "Opens the configured kit if the kit does not exists a new is created")
    @Restricted(msg = "Just log in or use the config!")
    public void create(ServerPlayer context, String kitname)
    {
        if (!FileUtil.isValidFileName(kitname))
        {
            i18n.send(context, NEGATIVE,
                                "{name#kit} is is not a valid name! Do not use characters like *, | or ?", kitname);
            return;
        }

        ViewableInventory inventory = ViewableInventory.builder().type(ContainerTypes.GENERIC_9X6).completeStructure().build();
        List<ItemStack> itemList = new ArrayList<>();
        Kit kit = manager.getExactKit(kitname);
        if (kit == null)
        {
            kit = new Kit(module, kitname, false, 0, -1, true, "", new ArrayList<>(), itemList);
        }
        else
        {
            itemList = kit.getItems();
        }

        showKit(context, inventory, itemList, kit);
    }

    private void showKit(ServerPlayer player, ViewableInventory inventory, List<ItemStack> itemList, Kit kit)
    {
        for (ItemStack stack : itemList)
        {
            inventory.offer(stack);
        }

        itemList.clear();

        final InventoryMenu menu = inventory.asMenu();
        menu.registerClose((cause, container) -> {
            inventory.slots().forEach(slot -> {
                final ItemStack item = slot.peek();
                if (!item.isEmpty())
                {
                    itemList.add(item.copy());
                }
            });
            manager.saveKit(kit);
            i18n.send(player, POSITIVE, "Created the {name#kit} kit!", kit.getKitName());
        });

        menu.setTitle(i18n.translate(player.getLocale(), Style.empty(), "Kit Contents: {name}", kit.getKitName()));
        menu.open(player);
    }


    @Alias(value = "kitlist")
    @Command(desc = "Lists all currently available kits.")
    public void list(CommandCause context)
    {
        final Set<String> kitsNames = manager.getKitsNames();
        if (kitsNames.isEmpty())
        {
            i18n.send(context.getAudience(), NEUTRAL, "No kits created yet.");
            return;
        }
        i18n.send(context.getAudience(), POSITIVE, "The following kits are available:");
        for (String kitName : kitsNames)
        {
            context.sendMessage(Identity.nil(), Component.text().append(Component.text(" - ", NamedTextColor.WHITE)).append(Component.text(kitName, NamedTextColor.YELLOW)).build());
        }
    }

    @Command(desc = "Gives a kit to every online player")
    public void giveall(CommandCause context, Kit kit, @ParameterPermission @Flag boolean force)
    {
        boolean gaveKit = false;
        int kitNotReceived = 0;
        for (ServerPlayer receiver : Sponge.getServer().getOnlinePlayers())
        {
            try
            {
                if (kit.checkPerm(context, force))
                {
                    return;
                }
                if (kit.checkLimit(context, receiver, force))
                {
                    return;
                }

                if (kit.give(context, receiver, force))
                {
                    if (receiver.equals(context.getAudience()))
                    {
                        i18n.send(context.getAudience(), POSITIVE, "Received the {name#kit} kit!", kit.getKitName());
                    }
                    else
                    {
                        i18n.send(context.getAudience(), POSITIVE, "You gave {user} the {name#kit} kit!", receiver, kit.getKitName());
                        i18n.send(receiver, POSITIVE, "Received the {name#kit} kit. Enjoy.", kit.getKitName());
                    }
                    gaveKit = true;
                }
            }
            catch (Exception ex)
            {
                kitNotReceived++;
            }
        }
        if (!gaveKit)
        {
            i18n.send(context.getAudience(), NEGATIVE, "No one received the kit!");
        }
        else if (kitNotReceived > 0)
        {
            i18n.sendN(context.getAudience(), NEGATIVE, kitNotReceived,
                    "{amount} players did not receive a kit!",
                    "One player did not receive a kit!",
                    kitNotReceived);
        }
    }

    @Command(desc = "Gives a set of items.")
    public void give(CommandCause context, Kit kit, @Default ServerPlayer player, @ParameterPermission @Flag boolean force)
    {
        boolean other = !context.getIdentifier().equals(player.getIdentifier());
        if (other && !context.hasPermission(module.perms().GIVE_OTHER.getId()))
        {
            i18n.send(context.getAudience(), NEGATIVE, "You are not allowed to give kits to other players!");
            return;
        }

        if (kit.checkPerm(context, force))
        {
            return;
        }
        if (kit.checkLimit(context, player, force))
        {
            return;
        }

        if (kit.give(context, player, force))
        {
            if (other)
            {
                i18n.send(context.getAudience(), POSITIVE, "You gave {user} the {name#kit} kit!", player, kit.getKitName());
                if (kit.getCustomMessage().isEmpty())
                {
                    i18n.send(player, POSITIVE, "Received the {name#kit} kit. Enjoy.", kit.getKitName());
                    return;
                }

                player.sendMessage(Identity.nil(), ChatFormat.fromLegacy(kit.getCustomMessage(), '&'));
                return;
            }
            if (kit.getCustomMessage() == null || kit.getCustomMessage().isEmpty())
            {
                i18n.send(context.getAudience(), POSITIVE, "Received the {name#kit} kit. Enjoy.", kit.getKitName());
                return;
            }
            player.sendMessage(Identity.nil(), ChatFormat.fromLegacy(kit.getCustomMessage(), '&'));
            return;
        }
        if (other)
        {
            i18n.send(context.getAudience(), NEUTRAL, "{user} has not enough space in your inventory for this kit!",
                                player);
            return;
        }
        i18n.send(context.getAudience(), NEUTRAL, "You don't have enough space in your inventory for this kit!");
    }
}
