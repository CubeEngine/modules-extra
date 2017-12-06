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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.util.FileUtil;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.command.annotation.ParameterPermission;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.inventoryguard.InventoryGuardFactory;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.text.serializer.TextSerializers.FORMATTING_CODE;

@Command(name = "kit", desc = "Manages kits")
public class KitCommand extends ContainerCommand
{
    private final KitManager manager;
    private final Kits module;
    private I18n i18n;
    private InventoryGuardFactory igf;

    public KitCommand(Kits module, I18n i18n, InventoryGuardFactory igf, CommandManager cm)
    {
        super(cm, Kits.class);
        this.module = module;
        this.i18n = i18n;
        this.igf = igf;
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

    @Command(alias = "remove", desc = "Deletes a kit")
    public void delete(CommandSource context, Kit kit)
    {
        manager.deleteKit(kit);
        i18n.send(context, POSITIVE, "Kit deleted.");
    }

    @Command(alias = "open", desc = "Opens the configured kit if the kit does not exists a new is created")
    @Restricted(value = Player.class, msg = "Just log in or use the config!")
    public void create(Player context, String kitname)
    {
        if (!FileUtil.isValidFileName(kitname))
        {
            i18n.send(context, NEGATIVE,
                                "{name#kit} is is not a valid name! Do not use characters like *, | or ?", kitname);
            return;
        }

        Inventory inventory = Inventory.builder().of(InventoryArchetypes.DOUBLE_CHEST)
                .property(InventoryTitle.PROPERTY_NAME, InventoryTitle.of(Text.of(i18n.getTranslation(context, "Kit Contents: "), kitname)))
                .build(module.getPlugin());

        List<ItemStack> itemList = new ArrayList<>();
        Kit kit = manager.getKit(kitname);
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

    private void showKit(Player player, Inventory inventory, List<ItemStack> itemList, Kit kit)
    {
        for (ItemStack stack : itemList)
        {
            inventory.offer(stack);
        }

        itemList.clear();
        igf.prepareInv(inventory, player.getUniqueId()).onClose(() -> {
            inventory.slots().forEach(slot -> {
                Optional<ItemStack> item = slot.peek();
                item.ifPresent(itemStack -> itemList.add(itemStack.copy()));
            });
            manager.saveKit(kit);
            i18n.send(player, POSITIVE, "Created the {name#kit} kit!", kit.getKitName());
        }).submitInventory(Kits.class, true);
    }


    @Alias(value = "kitlist")
    @Command(desc = "Lists all currently available kits.")
    public void list(CommandSource context)
    {
        i18n.send(context, POSITIVE, "The following kits are available:");
        for (String kitName : manager.getKitsNames())
        {
            context.sendMessage(Text.of(" ", TextColors.WHITE, "-", TextColors.YELLOW, kitName));
        }
    }

    @Command(desc = "Gives a kit to every online player")
    public void giveall(CommandSource context, Kit kit, @ParameterPermission @Flag boolean force)
    {
        boolean gaveKit = false;
        int kitNotReceived = 0;
        for (Player receiver : Sponge.getServer().getOnlinePlayers())
        {
            try
            {
                if (kit.give(context, receiver, force))
                {
                    if (receiver.equals(context))
                    {
                        i18n.send(context, POSITIVE, "Received the {name#kit} kit!", kit.getKitName());
                    }
                    else
                    {
                        i18n.send(context, POSITIVE, "You gave {user} the {name#kit} kit!", receiver, kit.getKitName());
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
            i18n.send(context, NEGATIVE, "No one received the kit!");
        }
        else if (kitNotReceived > 0)
        {
            i18n.send(context, NEGATIVE, "{amount} players did not receive a kit!",
                                kitNotReceived); // TODO Have a string for if there is only one player, so non-plural
        }
    }

    @Command(desc = "Gives a set of items.")
    public void give(CommandSource context, Kit kit, @Default Player player, @ParameterPermission @Flag boolean force)
    {
        boolean other = !context.getIdentifier().equals(player.getIdentifier());
        if (other && !context.hasPermission(module.perms().GIVE_OTHER.getId()))
        {
            i18n.send(context, NEGATIVE, "You are not allowed to give kits to other players!");
            return;
        }
        if (kit.give(context, player, force))
        {
            if (other)
            {
                i18n.send(context, POSITIVE, "You gave {user} the {name#kit} kit!", player, kit.getKitName());
                if (kit.getCustomMessage().isEmpty())
                {
                    i18n.send(player, POSITIVE, "Received the {name#kit} kit. Enjoy.", kit.getKitName());
                    return;
                }
                player.sendMessage(FORMATTING_CODE.deserialize(kit.getCustomMessage()));
                return;
            }
            if (kit.getCustomMessage().isEmpty())
            {
                i18n.send(context, POSITIVE, "Received the {name#kit} kit. Enjoy.", kit.getKitName());
                return;
            }
            context.sendMessage(FORMATTING_CODE.deserialize(kit.getCustomMessage()));
            return;
        }
        if (other)
        {
            i18n.send(context, NEUTRAL, "{user} has not enough space in your inventory for this kit!",
                                player);
            return;
        }
        i18n.send(context, NEUTRAL, "You don't have enough space in your inventory for this kit!");
    }
}
