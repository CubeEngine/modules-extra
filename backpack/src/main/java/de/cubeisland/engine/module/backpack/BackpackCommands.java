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

import de.cubeisland.engine.butler.alias.Alias;
import de.cubeisland.engine.butler.filter.Restricted;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Default;
import de.cubeisland.engine.butler.parametric.Label;
import de.cubeisland.engine.butler.parametric.Named;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.service.command.ContainerCommand;
import de.cubeisland.engine.service.command.CommandContext;
import de.cubeisland.engine.service.user.User;
import de.cubeisland.engine.module.core.util.ChatFormat;
import de.cubeisland.engine.module.core.util.StringUtils;
import org.spongepowered.api.world.World;

import static de.cubeisland.engine.module.core.util.formatter.MessageType.NEGATIVE;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.POSITIVE;
import static java.util.Arrays.asList;

@Command(name = "backpack", desc = "The Backpack commands", alias = "bp")
public class BackpackCommands extends ContainerCommand
{
    private final Backpack module;
    private final BackpackManager manager;

    public BackpackCommands(Backpack module, BackpackManager manager)
    {
        super(module);
        this.module = module;
        this.manager = manager;
    }

    @Alias(value = "openbp")
    @Command(desc = "opens a backpack")
    @Restricted(value = User.class, msg = "You cannot open a inventory in console!")
    public void open(CommandContext context, String name, @Default User player, @Named({"world", "for", "in", "w"}) World world)
    {
        if (world == null)
        {
            world = player.getWorld();
        }
        if (context.getSource() != player && !module.perms().OPEN_OTHER_USER.isAuthorized(context.getSource()))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to open the backpacks of other users!");
            return;
        }
        if (player.getWorld() != world && !module.perms().OPEN_OTHER_WORLDS.isAuthorized(context.getSource()))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to open backpacks from an other world!");
            return;
        }
        manager.openBackpack((User)context.getSource(), player, world, name);
    }

    @Alias(value = "createbp")
    @Command(desc = "creates a new backpack")
    public void create(CommandContext context, String name,
                       @Default @Optional User player,
                       @Named({"w", "world", "for", "in"}) World world,
                       @Named({"p", "pages"}) Integer pages,
                       @Named({"s","size"}) Integer size,
                       @Flag boolean global,
                       @Flag boolean single,
                       @Flag boolean blockinput)
    {
        if (world == null && !global)
        {
            if (!(context.getSource() instanceof User))
            {
                context.sendTranslated(POSITIVE, "You have to specify a world for non global backpacks!");
                return;
            }
            world = ((User)context.getSource()).getWorld();
        }
        manager.createBackpack(context.getSource(), player, name, world, global, single, blockinput, pages, size);
    }

    @Alias(value = "modifybp")
    @Command(desc = "modifies a backpack")
    public void modify(CommandContext context, String name, @Default User player,
                       @Named({"w", "world", "for", "in"}) World world,
                       @Named({"p", "pages"}) Integer pages,
                       @Named({"s","size"}) Integer size,
                       @Flag boolean blockinput)
    {
        if (world == null && (context.getSource() instanceof User))
        {
            world = ((User)context.getSource()).getWorld();
        }
        manager.modifyBackpack(context.getSource(), player, name, world, blockinput, pages, size);
    }

    @Alias(value = "givebp")
    @Command(desc = "Puts items into a backpack")
    // /givebp premium Faithcaio item diamondpick:1500 name "broken pick" lore "A broken\npick" "ench unbreaking:1,effi:3"
    public void give(CommandContext context, String name, @Default User player,
                     @Named({"item","i"}) @Label("item[:data]") String itemString, // TODO Required flag // TODO group parameter for ItemMeta
                     @Named({"name","n"}) @Label("name") String displayName,
                     @Named({"lore","l"}) @Label("lorelines...") String lore,
                     @Named({"amount","a"}) Integer amount,
                     @Named({"ench", "enchantments","e"}) @Label("enchs...") String enchantments,
                     @Named({"w", "world", "for", "in"}) World world)
    {
        if (world == null && (context.getSource() instanceof User))
        {
            world = ((User)context.getSource()).getWorld();
        }
        ItemStack matchedItem = Match.material().itemStack(itemString);
        if (matchedItem == null)
        {
            context.sendTranslated(NEGATIVE, "Item {input#name} not found!", itemString);
            return;
        }
        ItemMeta itemMeta = matchedItem.getItemMeta();
        if (displayName != null)
        {
            itemMeta.setDisplayName(ChatFormat.parseFormats(displayName));
        }
        if (lore != null)
        {
            itemMeta.setLore(asList(StringUtils.explode("\\n", ChatFormat.parseFormats(lore))));
        }
        if (enchantments != null)
        {
            String[] enchs = StringUtils.explode(",", enchantments);
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
        if (amount == null)
        {
            amount = matchedItem.getMaxStackSize();
        }
        matchedItem.setAmount(amount);
        this.manager.giveItem(context.getSource(), player, world, name, matchedItem);
    }
}
