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
package org.cubeengine.module.powertools;

import java.util.ArrayList;
import java.util.List;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Greedy;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.ChatFormat;
import org.cubeengine.module.powertools.data.PowertoolData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.InteractItemEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;

import static java.util.stream.Collectors.toList;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

/**
 * The PowerTool commands allow binding commands and/or chat-macros to a specific item.
 * <p>The data is appended onto the items lore
 */
@Singleton
@Command(name = "powertool", desc = "Binding shortcuts to an item.", alias = "pt")
public class PowertoolCommand extends DispatcherCommand
{
    private final Powertools module;
    private I18n i18n;

    @Inject
    public PowertoolCommand(Powertools module, I18n i18n)
    {
        super(Powertools.class);
        this.module = module;
        this.i18n = i18n;
    }

//    @Override
//    protected boolean selfExecute(CommandInvocation invocation)
//    {
//        if (!invocation.isConsumed())
//        {
//            if ("?".equals(invocation.currentToken()))
//            {
//                return super.selfExecute(invocation);
//            }
//        }
//        if (invocation.tokens().size() - invocation.consumed() > 0)
//        {
//            return this.getCommand("add").execute(invocation); // TODO add replace flag
//        }
//        return this.getCommand("clear").execute(invocation);
//    }

    @Alias(value = "ptc")
    @Command(desc = "Removes all commands from your powertool")
    @Restricted(value = ServerPlayer.class, msg = "No more power for you!")
    public void clear(ServerPlayer context, @Flag boolean all)
    {
        if (all)
        {
            for (Inventory slot : context.inventory().slots())
            {
                if (!slot.peek().isEmpty())
                {
                    slot.set(0, this.setPowerTool(slot.peek(), null));
                }
            }
            i18n.send(context, POSITIVE, "Removed all commands bound to items in your inventory!");
            return;
        }
        final ItemStack itemInHand = context.itemInHand(HandTypes.MAIN_HAND);
        if (itemInHand.isEmpty())
        {
            i18n.send(context, NEUTRAL, "You are not holding any item in your hand.");
            return;
        }
        context.setItemInHand(HandTypes.MAIN_HAND, this.setPowerTool(itemInHand, null));
        i18n.send(context, POSITIVE, "Removed all commands bound to the item in your hand!");
    }

    @Alias(value = "ptr")
    @Command(alias = {"del", "delete", "rm"}, desc = "Removes a command from your powertool")
    @Restricted(value = Player.class, msg = "No more power for you!")
    public void remove(ServerPlayer context, @Option @Greedy String command)
    {
        final ItemStack itemInHand = context.itemInHand(HandTypes.MAIN_HAND);
        if (itemInHand.isEmpty())
        {
            i18n.send(context, NEUTRAL, "You are not holding any item in your hand.");
            return;
        }
        context.setItemInHand(HandTypes.MAIN_HAND, this.remove(context, itemInHand, command));
    }

    private ItemStack remove(ServerPlayer context, ItemStack item, String cmd)
    {
        List<String> powers = item.get(PowertoolData.POWERS).orElse(null);
        if (cmd == null || cmd.isEmpty())
        {
            powers.remove(powers.size() - 1);
            this.setPowerTool(item, powers);
            i18n.send(context, POSITIVE, "Removed the last command bound to this item!");
        }
        else
        {
            boolean removed = false;
            while (powers.remove(cmd)) // removes also multiple same cmds
            {
                removed = true;
            }
            if (removed)
            {
                i18n.send(context, POSITIVE, "Removed the command: {input#command} bound to this item!", cmd);
            }
            else
            {
                i18n.send(context, NEGATIVE, "The command {input#command} was not found on this item!", cmd);
            }
        }
        this.setPowerTool(item, powers);
        if (powers.isEmpty())
        {
            i18n.send(context, NEUTRAL, "No more commands saved on this item!");
            return item;
        }
        this.showPowerToolList(context, powers, false, false);
        return item;
    }

    @Alias(value = "pta")
    @Command(desc = "Adds a command to your powertool")
    @Restricted(value = Player.class, msg = "You already have enough power!")
    public void add(ServerPlayer context, @Greedy String commandString, @Flag boolean replace)
    {
        final ItemStack itemInHand = context.itemInHand(HandTypes.MAIN_HAND);
        if (itemInHand.isEmpty())
        {
            i18n.send(context, NEUTRAL, "You do not have an item in your hand to bind the command to!");
            return;
        }
        List<String> powerTools;
        if (replace)
        {
            powerTools = new ArrayList<>(1);
        }
        else
        {
            powerTools = new ArrayList<>(this.getPowerTools(itemInHand));
        }
        powerTools.add(commandString);
        context.setItemInHand(HandTypes.MAIN_HAND, this.setPowerTool(itemInHand, powerTools));
    }

    @Alias(value = "ptl")
    @Command(desc = "Lists your powertool-bindings.")
    @Restricted(msg = "You already have enough power!")
    public void list(ServerPlayer context, @Flag boolean all)
    {
        if (all)
        {
            for (Inventory slot : context.inventory().slots())
            {
                if (!slot.peek().isEmpty())
                {
                    ItemStack item = slot.peek();
                    item.get(PowertoolData.POWERS).ifPresent(list -> {
                        context.sendMessage(Identity.nil(), item.get(Keys.CUSTOM_NAME).orElse(item.type().asComponent()).color(NamedTextColor.GOLD).append(Component.text(":")));
                        showPowerToolList(context, list, false, false);
                    });
                }
            }
            return;
        }
        if (context.itemInHand(HandTypes.MAIN_HAND).isEmpty())
        {
            i18n.send(context, NEUTRAL, "You do not have an item in your hand.");
        }
        else
        {
            this.showPowerToolList(context, this.getPowerTools(context.itemInHand(HandTypes.MAIN_HAND)), false, true);
        }
    }

    private void showPowerToolList(ServerPlayer context, List<String> powertools, boolean lastAsNew, boolean showIfEmpty)
    {
        if ((powertools == null || powertools.isEmpty()))
        {
            if (showIfEmpty)
            {
                i18n.send(context, NEGATIVE, "No commands saved on this item!");
            }
            return;
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (; i < powertools.size() - 1; i++)
        {
            sb.append("\n").append(ChatFormat.WHITE).append(powertools.get(i));
        }
        if (lastAsNew)
        {
            i18n.send(context, NEUTRAL, "{amount} command(s) bound to this item:{}", i + 1, sb.toString());
            final Component newText = i18n.translate(context, Style.style(NamedTextColor.GOLD), "NEW");
            context.sendMessage(Identity.nil(), Component.text().append(Component.text(powertools.get(i), NamedTextColor.YELLOW)).append(newText).build());
        }
        else
        {
            i18n.send(context, NEUTRAL, "{amount} command(s) bound to this item:{}", i + 1, sb.toString());
            context.sendMessage(Identity.nil(), Component.text(powertools.get(i)));
        }
    }

    private ItemStack setPowerTool(ItemStack item, List<String> newPowerTools)
    {
        if (newPowerTools == null)
        {
            item.remove(PowertoolData.POWERS);
        }
        else
        {
            item.offer(PowertoolData.POWERS, newPowerTools);
        }

        List<Component> lore = item.get(Keys.LORE).orElse(null);
        if (lore == null)
        {
            lore = new ArrayList<>();
        }
        List<Component> newLore = new ArrayList<>();

        for (Component text : lore)
        {
            if (PlainComponentSerializer.plain().serialize(text).equals("PowerTool"))
            {
                break;
            }
            newLore.add(text);
        }

        if (newPowerTools != null && !newPowerTools.isEmpty())
        {
            newLore.add(Component.text("PowerTool", NamedTextColor.DARK_GREEN));
            newLore.addAll(newPowerTools.stream().map(Component::text).collect(toList()));
        }

        if (newLore.isEmpty())
        {
            item.remove(Keys.LORE);
            return item;
        }

        item.offer(Keys.LORE, newLore);
        return item;
    }

    /**
     * Gets the PowerTools saved on this item.
     *
     * @param item
     * @return a list of the saved commands and/or chat-macros
     */
    private List<String> getPowerTools(ItemStack item)
    {
        return item.get(PowertoolData.POWERS).orElse(new ArrayList<>());
    }

    @Listener
    public void onLeftClick(InteractItemEvent.Primary event, @First ServerPlayer player)
    {
        this.onLeftClick0(event, player);
    }

    @Listener
    public void onLeftClick(InteractBlockEvent.Primary.Start event, @First ServerPlayer player)
    {
        this.onLeftClick0(event, player);
    }

    private void onLeftClick0(Event event, ServerPlayer player)
    {
        final ItemStack itemInHand = player.itemInHand(HandTypes.MAIN_HAND);
        if (!itemInHand.isEmpty() && player.hasPermission(module.perms().POWERTOOL_USE.getId()))
        {
            List<String> powers = this.getPowerTools(itemInHand);
            for (String power : powers)
            {
                if (power.startsWith("/"))
                {
                    try
                    {
                        Sponge.server().commandManager().process(player, power.substring(1));
                    }
                    catch (CommandException e)
                    {
                        throw new IllegalStateException(e);
                    }
                }
                else
                {
                    player.sendMessage(Identity.nil(), Component.text(power));
                }
            }
            if (!powers.isEmpty() && event instanceof Cancellable)
            {
                ((Cancellable)event).setCancelled(true);
            }
        }
    }
}
