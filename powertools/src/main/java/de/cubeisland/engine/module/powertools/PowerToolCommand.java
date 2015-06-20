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
package de.cubeisland.engine.module.powertools;

import java.util.ArrayList;
import java.util.List;
import de.cubeisland.engine.butler.CommandInvocation;
import de.cubeisland.engine.butler.alias.Alias;
import de.cubeisland.engine.butler.filter.Restricted;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Greed;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.module.core.util.ChatFormat;
import de.cubeisland.engine.module.core.util.matcher.MaterialMatcher;
import de.cubeisland.engine.service.command.ContainerCommand;
import de.cubeisland.engine.service.user.User;
import org.spongepowered.api.data.manipulator.DisplayNameData;
import org.spongepowered.api.data.manipulator.item.LoreData;
import org.spongepowered.api.entity.EntityInteractionTypes;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.entity.player.PlayerInteractEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;

import static de.cubeisland.engine.butler.parameter.Parameter.INFINITE;
import static de.cubeisland.engine.module.core.util.ChatFormat.GOLD;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.*;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.spongepowered.api.text.format.TextColors.DARK_GREEN;

/**
 * The PowerTool commands allow binding commands and/or chat-macros to a specific item.
 * <p>The data is appended onto the items lore
 * <p>/powertool
 */
@Command(name = "powertool", desc = "Binding shortcuts to an item.", alias = "pt")
public class PowerToolCommand extends ContainerCommand
{
    private final Powertools module;
    private MaterialMatcher materialMatcher;

    public PowerToolCommand(Powertools module, MaterialMatcher materialMatcher)
    {
        super(module);
        this.module = module;
        this.materialMatcher = materialMatcher;
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
    @Restricted(value = User.class, msg = "No more power for you!")
    public void clear(User context, @Flag boolean all)
    {
        if (all)
        {
            for (Inventory slot : context.asPlayer().getInventory().slots())
            {
                if (slot.peek().isPresent())
                {
                    this.setPowerTool(slot.peek().get(), null);
                }
            }
            context.sendTranslated(POSITIVE, "Removed all commands bound to items in your inventory!");
            return;
        }
        if (!context.asPlayer().getItemInHand().isPresent())
        {
            context.sendTranslated(NEUTRAL, "You are not holding any item in your hand.");
            return;
        }
        this.setPowerTool(context.asPlayer().getItemInHand().get(), null);
        context.sendTranslated(POSITIVE, "Removed all commands bound to the item in your hand!");
    }

    @Alias(value = "ptr")
    @Command(alias = {"del", "delete", "rm"}, desc = "Removes a command from your powertool")
    @Restricted(value = User.class, msg = "No more power for you!")
    public void remove(User context, @Optional @Greed(INFINITE) String command, @Flag boolean chat)
    {
        if (!context.asPlayer().getItemInHand().isPresent())
        {
            context.sendTranslated(NEUTRAL, "You are not holding any item in your hand.");
            return;
        }
        this.remove(context, context.asPlayer().getItemInHand().get(), command, !chat);
    }

    private void remove(User context, ItemStack item, String cmd, boolean isCommand)
    {
        List<String> powertools = this.getPowerTools(item);
        if (cmd == null || cmd.isEmpty())
        {
            powertools.remove(powertools.size() - 1);
            this.setPowerTool(item, powertools);
            context.sendTranslated(POSITIVE, "Removed the last command bound to this item!");
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
                context.sendTranslated(POSITIVE, "Removed the command: {input#command} bound to this item!", cmd);
            }
            else
            {
                context.sendTranslated(NEGATIVE, "The command {input#command} was not found on this item!", cmd);
            }
        }
        this.setPowerTool(item, powertools);
        if (powertools.isEmpty())
        {
            context.sendTranslated(NEUTRAL, "No more commands saved on this item!");
            return;
        }
        this.showPowerToolList(context, powertools, false, false);
    }

    @Alias(value = "pta")
    @Command(desc = "Adds a command to your powertool")
    @Restricted(value = User.class, msg = "You already have enough power!")
    public void add(User context, @Greed(INFINITE) String commandString, @Flag boolean chat, @Flag boolean replace)
    {
        if (!context.asPlayer().getItemInHand().isPresent())
        {
            context.sendTranslated(NEUTRAL, "You do not have an item in your hand to bind the command to!");
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
            powerTools = this.getPowerTools(context.asPlayer().getItemInHand().get());
        }
        powerTools.add(commandString);
        this.setPowerTool(context.asPlayer().getItemInHand().get(), powerTools);
    }

    @Alias(value = "ptl")
    @Command(desc = "Lists your powertool-bindings.")
    @Restricted(value = User.class, msg = "You already have enough power!")
    public void list(User context, @Flag boolean all)
    {
        if (all)
        {
            for (Inventory slot : context.asPlayer().getInventory().slots())
            {
                if (slot.peek().isPresent())
                {
                    ItemStack item = slot.peek().get();
                    DisplayNameData display = item.getData(DisplayNameData.class).orNull();
                    if (display == null)
                    {
                        context.sendMessage(GOLD + materialMatcher.getNameFor(item) + GOLD + ":");
                    }
                    else
                    {
                        context.sendMessage(Texts.of(TextColors.GOLD, display.getDisplayName(), GOLD, ":"));
                    }
                    this.showPowerToolList(context, this.getPowerTools(item), false, false);
                }
            }
            return;
        }
        if (!context.asPlayer().getItemInHand().isPresent())
        {
            context.sendTranslated(NEUTRAL, "You do not have an item in your hand.");
        }
        else
        {
            this.showPowerToolList(context, this.getPowerTools(context.asPlayer().getItemInHand().get()), false, true);
        }
    }

    private void showPowerToolList(User context, List<String> powertools, boolean lastAsNew, boolean showIfEmpty)
    {
        if ((powertools == null || powertools.isEmpty()))
        {
            if (showIfEmpty)
            {
                context.sendTranslated(NEGATIVE, "No commands saved on this item!");
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
            context.sendTranslated(NEUTRAL, "{amount} command(s) bound to this item:{}", i + 1, sb.toString());
            context.sendMessage(ChatFormat.YELLOW + powertools.get(i) + GOLD + "NEW"); // TODO translate
        }
        else
        {
            context.sendTranslated(NEUTRAL, "{amount} command(s) bound to this item:{}", i + 1, sb.toString());
            context.sendMessage(powertools.get(i));
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
            newLore.addAll(newPowerTools.stream().map(Texts::of).collect(toList()));
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
        Text first = Texts.of(DARK_GREEN, "PowerTool");
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
                    powerTool.add(Texts.toPlain(text));
                }
            }
            return powerTool;
        }
        return new ArrayList<>();
    }

    @Subscribe
    public void onLeftClick(PlayerInteractEvent event)
    {
        if (event.getInteractionType() != EntityInteractionTypes.ATTACK)
        {
            return;
        }
        Player player = event.getUser();
        if (!player.getItemInHand().isPresent() && module.perms().POWERTOOL_USE.isAuthorized(player))
        {
            List<String> powerTool = this.getPowerTools(player.getItemInHand().get());
            for (String command : powerTool)
            {
                player.getMessageSink().sendMessage(Texts.of(command));
            }
            if (!powerTool.isEmpty())
            {
                event.setCancelled(true);
            }
        }
    }
}
