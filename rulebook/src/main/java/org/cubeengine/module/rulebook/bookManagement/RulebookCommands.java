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
package org.cubeengine.module.rulebook.bookManagement;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.command.CommandContext;
import org.cubeengine.libcube.service.command.annotation.CommandPermission;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.util.ChatFormat;
import de.cubeisland.engine.i18n.language.Language;
import org.cubeengine.module.rulebook.Rulebook;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.item.ItemTypes.WRITABLE_BOOK;
import static org.spongepowered.api.item.ItemTypes.WRITTEN_BOOK;
import static org.spongepowered.api.text.format.TextColors.YELLOW;

@Command(name = "rulebook", desc = "Shows all commands of the rulebook module")
public class RulebookCommands extends ContainerCommand
{
    private final RulebookManager rulebookManager;
    private final Rulebook module;

    private final Permission getOtherPerm;
    private I18n i18n;

    public RulebookCommands(CommandManager base, Rulebook module, Permission getOtherPerm, I18n i18n)
    {
        super(base, Rulebook.class);
        this.rulebookManager = module.getRuleBookManager();
        this.module = module;
        this.getOtherPerm = getOtherPerm;
        this.i18n = i18n;
    }

    @Alias(value = {"getrules", "rules"})
    @Command(desc = "gets the player the rulebook in the inventory")
    public void getRuleBook(CommandSource context, @Optional String language, @Default @Named({"player", "p"}) Player player)
    {
        // console msg /wo player: i18n.sendTranslated(context, NEGATIVE, "The post office will give you your book!");
        Locale locale;
        if (!context.equals(player))
        {
            if(!context.hasPermission(getOtherPerm.getId()))
            {
                i18n.sendTranslated(context, NEGATIVE, "You do not have the permissions to add the rulebook to the inventory of an other player");
                return;
            }
        }

        if(this.rulebookManager.getLocales().isEmpty())
        {
            i18n.sendTranslated(context, NEUTRAL, "It does not exist a rulebook yet");
            return;
        }
        if (language != null)
        {
            Language l = this.rulebookManager.getLanguage(language);
            if(l == null)
            {
                i18n.sendTranslated(context, NEGATIVE, "Can't match the language");
                return;
            }
            locale = l.getLocale();
            if(!this.rulebookManager.contains(locale))
            {
                i18n.sendTranslated(context, NEUTRAL, "The language {name} is not supported yet.", locale.getDisplayLanguage(context.getLocale()));
                return;
            }
        }
        else
        {
            locale = player.getLocale();
            if(!this.rulebookManager.contains(locale))
            {
                locale = i18n.getDefaultLanguage().getLocale();
                if(!this.rulebookManager.contains(locale))
                {
                    locale = this.rulebookManager.getLocales().iterator().next();
                }
            }
        }
        Set<Inventory> books = this.inventoryRulebookSearching(player.getInventory(), locale);
        for (Inventory book : books)
        {
            book.clear();
        }

        player.getInventory().offer(this.rulebookManager.getBook(locale));
        i18n.sendTranslated(player, POSITIVE, "Lots of fun with your rulebook.");
        if(!books.isEmpty())
        {
            i18n.sendTranslated(player, POSITIVE, "Your old rulebook was removed");
        }
    }

    @Alias(value = "listrules")
    @Command(desc = "list all available languages of the rulebooks.")
    public void list(CommandSource context, @Flag boolean supported)
    {
        if (supported)
        {
            i18n.sendTranslated(context, NEUTRAL, "supported languages:");
            for(Language language : i18n.getLanguages())
            {
                context.sendMessage(Text.of(YELLOW, "* ", language.getLocale().getDisplayLanguage(context.getLocale())));
            }
            return;
        }
        if (this.rulebookManager.getLocales().isEmpty())
        {
            i18n.sendTranslated(context, NEUTRAL, "No rulebook available at the moment");
            return;
        }
        i18n.sendTranslated(context, NEUTRAL, "available languages:");
        for (Locale locale : this.rulebookManager.getLocales())
        {
            context.sendMessage(Text.of(YELLOW, "* ", locale.getDisplayLanguage(context.getLocale())));
        }
    }

    @Alias(value = "removerules")
    @Command(desc = "removes the declared language and languagefiles!")
    public void remove(CommandSource context, String language)
    {
        Language lang = this.rulebookManager.getLanguage(language);

        if(lang == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "More than one or no language is matched with {input}", language);
            return;
        }
        if(!this.rulebookManager.contains(lang.getLocale()))
        {
            i18n.sendTranslated(context, POSITIVE, "The languagefile of {input} doesn't exist at the moment", lang.getLocale().getDisplayLanguage(context.getLocale()));
            return;
        }
        
        try
        {
            this.rulebookManager.removeBook(lang.getLocale());
            i18n.sendTranslated(context, POSITIVE, "The languagefiles of {input} was deleted", lang.getLocale().getDisplayLanguage(context.getLocale()));
        }
        catch(IOException ex)
        {
            throw new IllegalStateException(ex);
        }

    }

    @Alias(value = "modifyrules")
    @Command(desc = "modified the rulebook of the declared language with the book in hand")
    public void modify(Player context, String language)
    {
        ItemStack item = context.getItemInHand().orElse(null);

        if(item != null && !(item.getItem() == WRITABLE_BOOK || item.getItem() == WRITTEN_BOOK))
        {
            i18n.sendTranslated(context, NEGATIVE, "I would try it with a book as item in hand");
            return;
        }

        Language lang = this.rulebookManager.getLanguage(language);
        if(lang == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "More than one or no language is matched with {input}", language);
            return;
        }
        Locale locale = lang.getLocale();

        if (!this.rulebookManager.contains(locale))
        {
            i18n.sendTranslated(context, NEGATIVE, "You can't modify a non-existent book.");
            return;
        }
        try
        {
            this.rulebookManager.removeBook(locale);
            this.rulebookManager.addBook(item, locale);
            i18n.sendTranslated(context, POSITIVE, "The rulebook {name} was succesful modified.", locale
                .getDisplayLanguage(context.getLocale()));
        }
        catch(IOException ex)
        {
            throw new IllegalStateException(ex);
        }
    }

    @Alias(value = "addrules")
    @Command(desc = "adds the book in hand as rulebook of the declared language")
    public void add(Player context, String language)
    {
        ItemStack item = context.getItemInHand().orElse(null);

        if(item != null && !(item.getItem() == WRITABLE_BOOK || item.getItem() == WRITTEN_BOOK))
        {
            i18n.sendTranslated(context, NEGATIVE, "I would try it with a book as item in hand");
            return;
        }

        Language lang = this.rulebookManager.getLanguage(language);
        if(lang == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "More than one or no language is matched with {input}", language);
            return;
        }
        Locale locale = lang.getLocale();

        if (this.rulebookManager.contains(locale))
        {
            i18n.sendTranslated(context, NEUTRAL, "There is already a book in that language.");
            return;
        }
        this.rulebookManager.addBook(item, locale);
        i18n.sendTranslated(context, POSITIVE, "Rulebook for the language {input} was added succesfully",
                               lang.getLocale().getDisplayLanguage(context.getLocale()));
    }

    private Set<Inventory> inventoryRulebookSearching(Inventory inventory, Locale locale)
    {
        Set<Inventory> books = new HashSet<>();

        for (Inventory inv : inventory.query(WRITTEN_BOOK).slots())
        {
            if (inv.peek().isPresent())
            {
                // TODO check for CustomData
                // if (inv.peek().get().get(RulebookData.class).isPresent())
                {
                    books.add(inv);
                }
            }
        }
        return books;
    }
}
