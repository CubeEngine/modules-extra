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
import de.cubeisland.engine.command.CommandInvocation;
import de.cubeisland.engine.command.alias.Alias;
import de.cubeisland.engine.command.filter.Restricted;
import de.cubeisland.engine.command.parametric.Command;
import de.cubeisland.engine.command.parametric.Flag;
import de.cubeisland.engine.command.parametric.Greed;
import de.cubeisland.engine.command.parametric.Optional;
import de.cubeisland.engine.core.command.ContainerCommand;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.ChatFormat;
import de.cubeisland.engine.core.util.matcher.Match;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static de.cubeisland.engine.command.parameter.Parameter.INFINITE;
import static de.cubeisland.engine.core.util.formatter.MessageType.*;
import static org.bukkit.Material.AIR;

/**
 * The PowerTool commands allow binding commands and/or chat-macros to a specific item.
 * <p>The data is appended onto the items lore
 * <p>/powertool
 */
@Command(name = "powertool", desc = "Binding shortcuts to an item.", alias = "pt")
public class PowerToolCommand extends ContainerCommand implements Listener
{
    private final Powertools module;

    public PowerToolCommand(Powertools module)
    {
        super(module);
        this.module = module;
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
            for (ItemStack item : context.getInventory().getContents())
            {
                this.setPowerTool(item, null);
            }
            context.sendTranslated(POSITIVE, "Removed all commands bound to items in your inventory!");
            return;
        }
        if (context.getItemInHand().getType() == AIR)
        {
            context.sendTranslated(NEUTRAL, "You are not holding any item in your hand.");
            return;
        }
        this.setPowerTool(context.getItemInHand(), null);
        context.sendTranslated(POSITIVE, "Removed all commands bound to the item in your hand!");
    }

    @Alias(value = "ptr")
    @Command(alias = {"del", "delete", "rm"}, desc = "Removes a command from your powertool")
    @Restricted(value = User.class, msg = "No more power for you!")
    public void remove(User context, @Optional @Greed(INFINITE) String command, @Flag boolean chat)
    {
        if (context.getItemInHand().getTypeId() == 0)
        {
            context.sendTranslated(NEUTRAL, "You are not holding any item in your hand.");
            return;
        }
        this.remove(context, context.getItemInHand(), command, !chat);
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
        if (context.getItemInHand().getType() == AIR)
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
            powerTools = this.getPowerTools(context.getItemInHand());
        }
        powerTools.add(commandString);
        this.setPowerTool(context.getItemInHand(), powerTools);
    }

    @Alias(value = "ptl")
    @Command(desc = "Lists your powertool-bindings.")
    @Restricted(value = User.class, msg = "You already have enough power!")
    public void list(User context, @Flag boolean all)
    {
        if (all)
        {
            for (ItemStack item : context.getInventory().getContents())
            {
                String itemName = item.getItemMeta().getDisplayName();
                if (itemName == null)
                {
                    context.sendMessage(ChatFormat.GOLD + Match.material().getNameFor(item) + ChatFormat.GOLD + ":");
                }
                else
                {
                    context.sendMessage(ChatFormat.GOLD + itemName + ChatFormat.GOLD + ":");
                }
                this.showPowerToolList(context, this.getPowerTools(item), false, false);
            }
            return;
        }
        if (context.getItemInHand().getType().equals(AIR))
        {
            context.sendTranslated(NEUTRAL, "You do not have an item in your hand.");
        }
        else
        {
            this.showPowerToolList(context, this.getPowerTools(context.getItemInHand()), false, true);
        }
        return;
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
            context.sendMessage(ChatFormat.YELLOW + powertools.get(i) + ChatFormat.GOLD + "NEW"); // TODO translate
        }
        else
        {
            context.sendTranslated(NEUTRAL, "{amount} command(s) bound to this item:{}", i + 1, sb.toString());
            context.sendMessage(powertools.get(i));
        }
    }

    private void setPowerTool(ItemStack item, List<String> newPowerTools)
    {
        ItemMeta meta = item.getItemMeta();
        List<String> newLore = new ArrayList<>();
        if (meta.hasLore())
        {
            for (String line : meta.getLore())
            {
                if (line.equals(ChatFormat.DARK_GREEN + "PowerTool"))
                {
                    break;
                }
                newLore.add(line);
            }
        }
        if (newPowerTools != null && !newPowerTools.isEmpty())
        {
            newLore.add(ChatFormat.DARK_GREEN + "PowerTool");
            newLore.addAll(newPowerTools);
        }
        meta.setLore(newLore);
        item.setItemMeta(meta);
    }

    /**
     * Gets the PowerTools saved on this item.
     *
     * @param item
     * @return a list of the saved commands and/or chat-macros
     */
    private List<String> getPowerTools(ItemStack item)
    {
        ItemMeta meta = item.getItemMeta();
        List<String> powerTool = new ArrayList<>();
        if (meta.hasLore())
        {
            boolean ptStart = false;
            for (String line : meta.getLore())
            {
                if (!ptStart && line.equals("§2PowerTool"))
                {
                    ptStart = true;
                }
                else if (ptStart)
                {
                    powerTool.add(line);
                }
            }
        }
        return powerTool;
    }

    @EventHandler
    public void onLeftClick(PlayerInteractEvent event)
    {
        if (event.getAction().equals(Action.LEFT_CLICK_AIR) || event.getAction().equals(Action.LEFT_CLICK_BLOCK))
        {
            Player player = event.getPlayer();
            if (!player.getItemInHand().getType().equals(AIR)
                    && module.perms().POWERTOOL_USE.isAuthorized(event.getPlayer()))
            {
                List<String> powerTool = this.getPowerTools(player.getItemInHand());
                for (String command : powerTool)
                {
                    player.chat(command);
                }
                if (!powerTool.isEmpty())
                {
                    event.setUseItemInHand(Event.Result.DENY);
                    event.setUseInteractedBlock(Event.Result.DENY);
                    event.setCancelled(true);
                }
            }
        }
    }
}
