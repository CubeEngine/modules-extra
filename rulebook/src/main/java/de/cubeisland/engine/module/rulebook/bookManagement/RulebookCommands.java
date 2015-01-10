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
package de.cubeisland.engine.module.rulebook.bookManagement;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import de.cubeisland.engine.command.alias.Alias;
import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Flag;
import de.cubeisland.engine.command.methodic.Flags;
import de.cubeisland.engine.command.methodic.Param;
import de.cubeisland.engine.command.methodic.Params;
import de.cubeisland.engine.core.command.CommandContainer;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.command.annotation.CommandPermission;
import de.cubeisland.engine.core.permission.Permission;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.ChatFormat;
import de.cubeisland.engine.i18n.language.Language;
import de.cubeisland.engine.module.rulebook.Rulebook;

import static de.cubeisland.engine.command.parameter.property.Requirement.OPTIONAL;
import static de.cubeisland.engine.core.permission.PermDefault.TRUE;
import static de.cubeisland.engine.core.util.formatter.MessageType.*;
import static org.bukkit.Material.BOOK_AND_QUILL;
import static org.bukkit.Material.WRITTEN_BOOK;

@Command(name = "rulebook", desc = "Shows all commands of the rulebook module")
public class RulebookCommands extends CommandContainer
{
    private final RulebookManager rulebookManager;
    private final Rulebook module;

    private final Permission getPermission;
    
    public RulebookCommands(Rulebook module)
    {
        super(module);
        this.rulebookManager = module.getRuleBookManager();
        this.module = module;
        this.getPermission = module.getBasePermission().childWildcard("command").childWildcard("get").child("other");
        this.module.getCore().getPermissionManager().registerPermission(module, getPermission);
    }

    @Alias(value = {"getrules", "rules"})
    @Command(desc = "gets the player the rulebook in the inventory")
    @Params(positional = @Param(req = OPTIONAL, label = "language"),
            nonpositional = @Param(names = {"player", "p"}, label = "name", type = User.class))
    @CommandPermission(permDefault = TRUE)
    public void getRuleBook(CommandContext context)
    {
        if(!(context.getSource() instanceof User) && !context.hasNamed("player"))
        {
            context.sendTranslated(NEGATIVE, "The post office will give you your book!");
            return;
        }
        
        Locale locale;
        User user;
        if(context.hasNamed("player"))
        {
            if(!getPermission.isAuthorized(context.getSource()))
            {
                context.sendTranslated(NEGATIVE, "You do not have the permissions to add the rulebook to the inventory of an other player");
                return;
            }
            user = context.get("player");
            if(user == null)
            {
                context.sendTranslated(NEGATIVE, "The given user was not found!");
                return;
            }
        }
        else
        {
            user = (User) context.getSource();
        }

        if(this.rulebookManager.getLocales().isEmpty())
        {
            context.sendTranslated(NEUTRAL, "It does not exist a rulebook yet");
            return;
        }

        if(context.hasPositional(0))
        {
            Language language = this.rulebookManager.getLanguage(context.getString(0));

            if(language == null)
            {
                context.sendTranslated(NEGATIVE, "Can't match the language");
                return;
            }
            
            locale = language.getLocale();
            
            if(!this.rulebookManager.contains(locale))
            {
                context.sendTranslated(NEUTRAL, "The language {name} is not supported yet.", language.getLocale().getDisplayLanguage(context.getSource().getLocale()));
                return;
            }
        }
        else
        {
            locale = user.getLocale();
            if(!this.rulebookManager.contains(locale))
            {
                locale = this.module.getCore().getI18n().getDefaultLanguage().getLocale();
                if(!this.rulebookManager.contains(locale))
                {
                    locale = this.rulebookManager.getLocales().iterator().next();
                }
            }
        }

        Set<Integer> books = this.inventoryRulebookSearching(user.getInventory(), locale);

        Iterator<Integer> iter = books.iterator();
        while(iter.hasNext())
        {
            user.getInventory().clear(iter.next());
        }

        user.getInventory().addItem(this.rulebookManager.getBook(locale));
        user.sendTranslated(POSITIVE, "Lots of fun with your rulebook.");
        if(!books.isEmpty())
        {
            user.sendTranslated(POSITIVE, "Your old rulebook was removed");
        }
    }

    @Alias(value = "listrules")
    @Command(desc = "list all available languages of the rulebooks.")
    @CommandPermission(permDefault = TRUE)
    public void list(CommandContext context, @Flag boolean supported)
    {
        if (supported)
        {
            context.sendTranslated(NEUTRAL, "supported languages:");
            for(Language language : this.module.getCore().getI18n().getLanguages())
            {
                context.sendMessage(ChatFormat.YELLOW +  "* " + language.getLocale().getDisplayLanguage(context.getSource().getLocale()));
            }
            return;
        }
        if (this.rulebookManager.getLocales().isEmpty())
        {
            context.sendTranslated(NEUTRAL, "No rulebook available at the moment");
            return;
        }
        context.sendTranslated(NEUTRAL, "available languages:");
        for (Locale locale : this.rulebookManager.getLocales())
        {
            context.sendMessage(ChatFormat.YELLOW + "* " + locale.getDisplayLanguage(
                context.getSource().getLocale()));
        }
    }

    @Alias(value = "removerules")
    @Command(desc = "removes the declared language and languagefiles!")
    public void remove(CommandContext context, String language)
    {
        Language lang = this.rulebookManager.getLanguage(language);

        if(lang == null)
        {
            context.sendTranslated(NEGATIVE, "More than one or no language is matched with {input}", language);
            return;
        }
        if(!this.rulebookManager.contains(lang.getLocale()))
        {
            context.sendTranslated(POSITIVE, "The languagefile of {input} doesn't exist at the moment", lang.getLocale().getDisplayLanguage(context.getSource().getLocale()));
            return;
        }
        
        try
        {
            this.rulebookManager.removeBook(lang.getLocale());
            context.sendTranslated(POSITIVE, "The languagefiles of {input} was deleted", lang.getLocale().getDisplayLanguage(context.getSource().getLocale()));
        }
        catch(IOException ex)
        {
            context.sendTranslated(NEGATIVE, "The language file of {input} couldn't be deleted",
                                   lang.getLocale().getDisplayLanguage(context.getSource().getLocale()));
            this.module.getLog().error(ex, "Error when deleting the files!");
        }

    }

    @Alias(value = "modifyrules")
    @Command(desc = "modified the rulebook of the declared language with the book in hand")
    public void modify(CommandContext context, String language)
    {
        if(!(context.getSource() instanceof User))
        {
            context.sendTranslated(NEUTRAL, "You're able to write, right?");
        }
        User user = (User) context.getSource();

        ItemStack item = user.getItemInHand();

        if(!(item.getType() == WRITTEN_BOOK) && !(item.getType() == BOOK_AND_QUILL))
        {
            context.sendTranslated(NEGATIVE, "I would try it with a book as item in hand");
            return;
        }

        Language lang = this.rulebookManager.getLanguage(language);
        if(lang == null)
        {
            context.sendTranslated(NEGATIVE, "More than one or no language is matched with {input}", language);
            return;
        }
        Locale locale = lang.getLocale();

        if (!this.rulebookManager.contains(locale))
        {
            context.sendTranslated(NEGATIVE, "You can't modify a non-existent book.");
            return;
        }
        try
        {
            this.rulebookManager.removeBook(locale);
            this.rulebookManager.addBook(item, locale);
            context.sendTranslated(POSITIVE, "The rulebook {name} was succesful modified.", locale
                .getDisplayLanguage(context.getSource().getLocale()));
        }
        catch(IOException ex)
        {
            context.sendTranslated(NEUTRAL, "An error ocurred while deleting the old rulebook");
            this.module.getLog().error(ex, "Error when deleting the files!");
        }
    }

    @Alias(value = "addrules")
    @Command(desc = "adds the book in hand as rulebook of the declared language")
    public void add(CommandContext context, String language)
    {
        if(!(context.getSource() instanceof User))
        {
            context.sendTranslated(NEUTRAL, "Are you illiterate?");
        }
        User user = (User) context.getSource();

        ItemStack item = user.getItemInHand();

        if(!(item.getType() == WRITTEN_BOOK) && !(item.getType() == BOOK_AND_QUILL))
        {
            context.sendTranslated(NEGATIVE, "I would try it with a book as item in hand");
            return;
        }

        Language lang = this.rulebookManager.getLanguage(language);
        if(lang == null)
        {
            context.sendTranslated(NEGATIVE, "More than one or no language is matched with {input}", language);
            return;
        }
        Locale locale = lang.getLocale();

        if (this.rulebookManager.contains(locale))
        {
            context.sendTranslated(NEUTRAL, "There is already a book in that language.");
            return;
        }
        this.rulebookManager.addBook(item, locale);
        context.sendTranslated(POSITIVE, "Rulebook for the language {input} was added succesfully",
                               lang.getLocale().getDisplayLanguage(context.getSource().getLocale()));
    }

    private Set<Integer> inventoryRulebookSearching(PlayerInventory inventory, Locale locale)
    {
        Set<Integer> books = new HashSet<>();

        for(int i = 0; i < inventory.getSize(); i++)
        {
            ItemStack item = inventory.getItem(i);

            if(item != null && item.getType() == WRITTEN_BOOK)
            {
                List<String> lore = item.getItemMeta().getLore();
                if(lore != null)
                {
                    if(lore.size() > 0 && locale.getLanguage().equalsIgnoreCase(lore.get(0)))
                    {
                        books.add(i);
                    }
                }
            }
        }
        return books;
    }
}
