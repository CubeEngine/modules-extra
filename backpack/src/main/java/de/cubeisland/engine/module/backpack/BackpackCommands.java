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
package de.cubeisland.engine.module.backpack;

import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Flag;
import de.cubeisland.engine.command.methodic.Flags;
import de.cubeisland.engine.command.methodic.Param;
import de.cubeisland.engine.command.methodic.Params;
import de.cubeisland.engine.core.command.CommandContainer;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.command_old.parameterized.completer.WorldCompleter;
import de.cubeisland.engine.core.command_old.reflected.Alias;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.ChatFormat;
import de.cubeisland.engine.core.util.StringUtils;
import de.cubeisland.engine.core.util.matcher.Match;

import static de.cubeisland.engine.core.util.formatter.MessageType.NEGATIVE;
import static de.cubeisland.engine.core.util.formatter.MessageType.POSITIVE;
import static java.util.Arrays.asList;

@Command(name = "backpack", desc = "The Backpack commands", alias = "bp")
public class BackpackCommands extends CommandContainer
{
    private final Backpack module;
    private final BackpackManager manager;

    public BackpackCommands(Backpack module, BackpackManager manager)
    {
        super(module);
        this.module = module;
        this.manager = manager;
    }

    @Alias(names = "openbp")
    @Command(desc = "opens a backpack")
    @Params(positional = {@Param(label = "name"),
                          @Param(req = false, label = "user", type = User.class)},
            nonpositional = @Param(names = {"world", "for", "in", "w"}, completer = WorldCompleter.class, type = World.class))
    public void open(CommandContext context)
    {
        if (context.getSource() instanceof User)
        {
            User forUser = (User)context.getSource();
            if (context.hasPositional(1))
            {
                forUser = context.get(1);
            }
            World forWorld = forUser.getWorld();
            if (context.hasNamed("w"))
            {
                forWorld = context.get("w", null);
                if (forWorld == null)
                {
                    context.sendTranslated(NEGATIVE, "Unknown World {input#world}!", context.getString("w"));
                    return;
                }
            }
            if (context.getSource() != forUser && !module.perms().OPEN_OTHER_USER.isAuthorized(context.getSource()))
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to open the backpacks of other users!");
                return;
            }
            if (forUser.getWorld() != forWorld && ! module.perms().OPEN_OTHER_WORLDS.isAuthorized(context.getSource()))
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to open backpacks from an other world!");
                return;
            }
            manager.openBackpack((User)context.getSource(), forUser, forWorld, context.getString(0));
            return;
        }
        context.sendTranslated(NEGATIVE, "You cannot open a inventory in console!"); // TODO perhaps save inventory to yml
    }

    @Alias(names = "createbp")
    @Command(desc = "creates a new backpack")
    @Params(positional = {@Param(label = "name"),
                          @Param(req = false, label = "user", type = User.class)},
            nonpositional = {@Param(names = {"w", "world", "for", "in"}, completer = WorldCompleter.class, type = World.class),
                             @Param(names = {"p", "pages"}, type = Integer.class),
                             @Param(names = {"s","size"}, type = Integer.class)})
    @Flags({@Flag(name = "g", longName = "global"), // TODO OR flags
            @Flag(name = "s", longName = "single"),
            @Flag(name = "b", longName = "blockinput")})
    public void create(CommandContext context)
    {
        User forUser = null;
        World forWorld = null;
        if (context.hasNamed("w"))
        {
            forWorld = context.get("w", null);
            if (forWorld == null)
            {
                context.sendTranslated(NEGATIVE, "Unknown World {input#world}!", context.getString("w"));
                return;
            }
        }
        else if (context.getSource() instanceof User)
        {
            forUser = (User)context.getSource();
            forWorld = ((User)context.getSource()).getWorld();
        }
        else if (!context.hasFlag("g"))
        {
            context.sendTranslated(POSITIVE, "You have to specify a world for non global backpacks!");
            return;
        }
        if (context.hasPositional(1))
        {
            forUser = context.get(1);
        }
        else if (!(context.getSource() instanceof User))
        {
            context.sendTranslated(NEGATIVE, "You need to specify a user");
            return;
        }
        manager.createBackpack(context.getSource(), forUser, context.getString(0), forWorld,
                               context.hasFlag("g"), context.hasFlag("s"), context.hasFlag("b"),
                               context.get("p", 1), context.get("s", 6));
    }

    @Alias(names = "modifybp")
    @Command(desc = "modifies a backpack")
    @Params(positional = {@Param(label = "name"),
                          @Param(req = false, label = "user", type = User.class)},
            nonpositional = {@Param(names = {"pages","p"}, type = Integer.class),
                             @Param(names = {"size","s"}, type = Integer.class),
                             @Param(names = {"blockinput","b"}, type = Boolean.class, label = "true|false"),
                             @Param(names = {"world", "for", "in", "w"}, completer = WorldCompleter.class, type = World.class)})
    public void modify(CommandContext context)
    {
        User forUser = null;
        World forWorld = null;
        if (context.getSource() instanceof User)
        {
            forUser = (User)context.getSource();
            forWorld = ((User)context.getSource()).getWorld();
        }
        else if (context.hasNamed("w"))
        {
            forWorld = context.get("w", null);
            if (forWorld == null)
            {
                context.sendTranslated(NEGATIVE, "Unknown World {input#world}!", context.getString("w"));
                return;
            }
        }
        else if (!context.hasFlag("g"))
        {
            context.sendTranslated(POSITIVE, "You have to specify a world for non global backpacks!");
            return;
        }
        if (context.hasPositional(1))
        {
            forUser = context.get(1);
        }
        else if (!(context.getSource() instanceof User))
        {
            context.sendTranslated(NEGATIVE, "You need to specify a user");
            return;
        }
        manager.modifyBackpack(context.getSource(), forUser, context.getString(0), forWorld,
                               (Integer)context.get("p", null),
                               (Boolean)context.get("b", null),
                               (Integer)context.get("s", null));
    }

    @Alias(names = "givebp")
    @Command(desc = "Puts items into a backpack")
    @Params(positional = {@Param(label = "name"),
                          @Param(req = false, label = "user", type = User.class)},
            nonpositional ={@Param(names = {"item","i"}, req = true, label = "item[:data]"),
                            @Param(names = {"name","n"}),
                            @Param(names = {"lore","l"}, label = "lorelines..."),
                            @Param(names = {"amount","a"}, type = Integer.class),
                            @Param(names = {"ench", "enchantments","e"}, label = "enchs..."),
                            @Param(names = {"world", "for", "in", "w"}, completer = WorldCompleter.class, type = World.class)})
    // /givebp premium Faithcaio item diamondpick:1500 name "broken pick" lore "A broken\npick" "ench unbreaking:1,effi:3"
    public void give(CommandContext context)
    {
        User forUser = null;
        World forWorld = null;
        if (context.getSource() instanceof User)
        {
            forUser = (User)context.getSource();
            forWorld = ((User)context.getSource()).getWorld();
        }
        else if (context.hasNamed("w"))
        {
            forWorld = context.get("w", null);
            if (forWorld == null)
            {
                context.sendTranslated(NEGATIVE, "Unknown World {input#world}!", context.getString("w"));
                return;
            }
        }
        if (context.hasPositional(1))
        {
            forUser = context.get(1);
        }
        else if (!(context.getSource() instanceof User))
        {
            context.sendTranslated(NEGATIVE, "You need to specify a user");
            return;
        }
        ItemStack matchedItem = Match.material().itemStack(context.getString("i"));
        if (matchedItem == null)
        {
            context.sendTranslated(NEGATIVE, "Item {input#name} not found!", context.getString("i"));
            return;
        }
        ItemMeta itemMeta = matchedItem.getItemMeta();
        if (context.hasNamed("n"))
        {
            itemMeta.setDisplayName(ChatFormat.parseFormats(context.getString("n")));
        }
        if (context.hasNamed("l"))
        {
            itemMeta.setLore(asList(StringUtils.explode("\\n", ChatFormat.parseFormats(context.getString("l")))));
        }
        if (context.hasNamed("e"))
        {
            String[] enchs = StringUtils.explode(",", context.getString("e"));
            for (String ench : enchs)
            {
                Enchantment enchantment;
                int power;
                if (ench.contains(":"))
                {
                    enchantment = Match.enchant().enchantment(ench.substring(0, ench.indexOf(":")));
                    if (enchantment == null)
                    {
                        context.sendTranslated(NEGATIVE, "Unknown Enchantment {input#enchant}", ench);
                        return;
                    }
                    power = Integer.parseInt(ench.substring(ench.indexOf(":")+1));
                }
                else
                {
                    enchantment = Match.enchant().enchantment(ench);
                    if (enchantment == null)
                    {
                        context.sendTranslated(NEGATIVE, "Unknown Enchantment {input#enchant}", ench);
                        return;
                    }
                    power = enchantment.getMaxLevel();
                }
                itemMeta.addEnchant(enchantment, power, true);
            }
        }
        matchedItem.setItemMeta(itemMeta);
        Integer amount = matchedItem.getMaxStackSize();
        if (context.hasNamed("a"))
        {
            amount = context.get("a", null);
            if (amount == null)
            {
                context.sendTranslated(NEGATIVE, "Invalid amount {input#amount}", context.getString("a"));
                return;
            }
        }
        matchedItem.setAmount(amount);
        this.manager.giveItem(context.getSource(), forUser, forWorld, context.getString(0), matchedItem);
    }
}
