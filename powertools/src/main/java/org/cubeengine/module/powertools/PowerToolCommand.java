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
package org.cubeengine.module.powertools;

import java.util.ArrayList;
import java.util.List;
import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Greed;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.core.util.ChatFormat;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.matcher.MaterialMatcher;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import static java.util.stream.Collectors.toList;
import static org.cubeengine.butler.parameter.Parameter.INFINITE;
import static org.cubeengine.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.text.format.TextColors.DARK_GREEN;
import static org.spongepowered.api.text.format.TextColors.GOLD;
import static org.spongepowered.api.text.format.TextColors.YELLOW;

/**
 * The PowerTool commands allow binding commands and/or chat-macros to a specific item.
 * <p>The data is appended onto the items lore
 */
@Command(name = "powertool", desc = "Binding shortcuts to an item.", alias = "pt")
public class PowerToolCommand extends ContainerCommand
{
    private final Powertools module;
    private MaterialMatcher materialMatcher;
    private I18n i18n;

    public PowerToolCommand(Powertools module, MaterialMatcher materialMatcher, I18n i18n)
    {
        super(module);
        this.module = module;
        this.materialMatcher = materialMatcher;
        this.i18n = i18n;
    }

    @Override
    protected boolean selfExecute(CommandInvocation invocation)
    {
        if ("?".equals(invocation.currentToken()))
        {
            return super.selfExecute(invocation);
        }
        if (invocation.tokens().size() - invocation.consumed() > 0)
        {
            return this.getCommand("add").execute(invocation); // TODO add replace flag
        }
        return this.getCommand("clear").execute(invocation);
    }

    @Alias(value = "ptc")
    @Command(desc = "Removes all commands from your powertool")
    @Restricted(value = Player.class, msg = "No more power for you!")
    public void clear(Player context, @Flag boolean all)
    {
        if (all)
        {
            for (Inventory slot : context.getInventory().slots())
            {
                if (slot.peek().isPresent())
                {
                    this.setPowerTool(slot.peek().get(), null);
                }
            }
            i18n.sendTranslated(context, POSITIVE, "Removed all commands bound to items in your inventory!");
            return;
        }
        if (!context.getItemInHand().isPresent())
        {
            i18n.sendTranslated(context, NEUTRAL, "You are not holding any item in your hand.");
            return;
        }
        this.setPowerTool(context.getItemInHand().get(), null);
        i18n.sendTranslated(context, POSITIVE, "Removed all commands bound to the item in your hand!");
    }

    @Alias(value = "ptr")
    @Command(alias = {"del", "delete", "rm"}, desc = "Removes a command from your powertool")
    @Restricted(value = Player.class, msg = "No more power for you!")
    public void remove(Player context, @Optional @Greed(INFINITE) String command, @Flag boolean chat)
    {
        if (!context.getItemInHand().isPresent())
        {
            i18n.sendTranslated(context, NEUTRAL, "You are not holding any item in your hand.");
            return;
        }
        this.remove(context, context.getItemInHand().get(), command, !chat);
    }

    private void remove(Player context, ItemStack item, String cmd, boolean isCommand)
    {
        List<String> powertools = this.getPowerTools(item);
        if (cmd == null || cmd.isEmpty())
        {
            powertools.remove(powertools.size() - 1);
            this.setPowerTool(item, powertools);
            i18n.sendTranslated(context, POSITIVE, "Removed the last command bound to this item!");
        }
        else
        {
            if (isCommand)
            {
                cmd = "/" + cmd;
            }
            boolean removed = false;
            while (powertools.remove(cmd)) // removes also multiple same cmds
            {
                removed = true;
            }
            if (removed)
            {
                i18n.sendTranslated(context, POSITIVE, "Removed the command: {input#command} bound to this item!", cmd);
            }
            else
            {
                i18n.sendTranslated(context, NEGATIVE, "The command {input#command} was not found on this item!", cmd);
            }
        }
        this.setPowerTool(item, powertools);
        if (powertools.isEmpty())
        {
            i18n.sendTranslated(context, NEUTRAL, "No more commands saved on this item!");
            return;
        }
        this.showPowerToolList(context, powertools, false, false);
    }

    @Alias(value = "pta")
    @Command(desc = "Adds a command to your powertool")
    @Restricted(value = Player.class, msg = "You already have enough power!")
    public void add(Player context, @Greed(INFINITE) String commandString, @Flag boolean chat, @Flag boolean replace)
    {
        if (!context.getItemInHand().isPresent())
        {
            i18n.sendTranslated(context, NEUTRAL, "You do not have an item in your hand to bind the command to!");
            return;
        }
        if (!chat)
        {
            commandString = "/" + commandString;
        }
        List<String> powerTools;
        if (replace)
        {
            powerTools = new ArrayList<>(1);
        }
        else
        {
            powerTools = this.getPowerTools(context.getItemInHand().get());
        }
        powerTools.add(commandString);
        this.setPowerTool(context.getItemInHand().get(), powerTools);
    }

    @Alias(value = "ptl")
    @Command(desc = "Lists your powertool-bindings.")
    @Restricted(value = Player.class, msg = "You already have enough power!")
    public void list(Player context, @Flag boolean all)
    {
        if (all)
        {
            for (Inventory slot : context.getInventory().slots())
            {
                if (slot.peek().isPresent())
                {
                    ItemStack item = slot.peek().get();
                    DisplayNameData display = item.getData(DisplayNameData.class).orNull();
                    if (display == null)
                    {
                        context.sendMessage(Text.of(GOLD, materialMatcher.getNameFor(item), GOLD, ":"));
                    }
                    else
                    {
                        context.sendMessage(Text.of(GOLD, display.getDisplayName(), GOLD, ":"));
                    }
                    this.showPowerToolList(context, this.getPowerTools(item), false, false);
                }
            }
            return;
        }
        if (!context.getItemInHand().isPresent())
        {
            i18n.sendTranslated(context, NEUTRAL, "You do not have an item in your hand.");
        }
        else
        {
            this.showPowerToolList(context, this.getPowerTools(context.getItemInHand().get()), false, true);
        }
    }

    private void showPowerToolList(Player context, List<String> powertools, boolean lastAsNew, boolean showIfEmpty)
    {
        if ((powertools == null || powertools.isEmpty()))
        {
            if (showIfEmpty)
            {
                i18n.sendTranslated(context, NEGATIVE, "No commands saved on this item!");
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
            i18n.sendTranslated(context, NEUTRAL, "{amount} command(s) bound to this item:{}", i + 1, sb.toString());
            Text newText = i18n.getTranslation(context, NONE, "NEW");
            context.sendMessage(Text.of(YELLOW, powertools.get(i), GOLD, newText));
        }
        else
        {
            i18n.sendTranslated(context, NEUTRAL, "{amount} command(s) bound to this item:{}", i + 1, sb.toString());
            context.sendMessage(Text.of(powertools.get(i)));
        }
    }

    private void setPowerTool(ItemStack item, List<String> newPowerTools)
    {
        LoreData lore = item.getOrCreate(LoreData.class).get();
        List<Text> newLore = new ArrayList<>();
        Text first = Texts.of(DARK_GREEN, "PowerTool");
        for (Text text : lore.getAll())
        {
            if (text.equals(first))
            {
                break;
            }
            newLore.add(first);
        }

        if (newPowerTools != null && !newPowerTools.isEmpty())
        {
            newLore.add(first);
            newLore.addAll(newPowerTools.stream().map(Text::of).collect(toList()));
        }

        if (!newLore.isEmpty())
        {
            item.remove(LoreData.class);
            return;
        }

        lore.set(newLore);
        item.offer(lore);
    }

    /**
     * Gets the PowerTools saved on this item.
     *
     * @param item
     * @return a list of the saved commands and/or chat-macros
     */
    private List<String> getPowerTools(ItemStack item)
    {
        Text first = Text.of(DARK_GREEN, "PowerTool");
        LoreData lore = item.getData(LoreData.class).orNull();
        if (lore != null)
        {
            List<String> powerTool = new ArrayList<>();
            boolean ptStart = false;
            for (Text text : lore.getAll())
            {
                if (text.equals(first))
                {
                    ptStart = true;
                }
                else if (ptStart)
                {
                    powerTool.add(text.toPlain());
                }
            }
            return powerTool;
        }
        return new ArrayList<>();
    }

    @Listener
    public void onLeftClick(InteractBlockEvent.Primary event, @First Player player)
    {
        if (!player.getItemInHand().isPresent() && player.hasPermission(module.perms().POWERTOOL_USE.getId()))
        {
            List<String> powerTool = this.getPowerTools(player.getItemInHand().get());
            for (String command : powerTool)
            {
                player.getMessageChannel().send(Text.of(command)); // TODO is this working for cmds?
            }
            if (!powerTool.isEmpty())
            {
                event.setCancelled(true);
            }
        }
    }
}
