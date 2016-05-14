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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import de.cubeisland.engine.i18n.language.Language;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.module.rulebook.Rulebook;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

public final class RulebookManager
{
    private final Rulebook module;
    private final Map<Locale, String[]> rulebooks;
    private I18n i18n;

    public RulebookManager(Rulebook module, I18n i18n)
    {
        this.module = module;
        this.i18n = i18n;

        this.rulebooks = new HashMap<>();

        for(Path book : RuleBookFile.getLanguageFiles(i18n, this.module.getFolder()))
        {
            Language language = this.getLanguage(StringUtils.stripFileExtension(book.getFileName().toString()));
            try
            {
                rulebooks.put(language.getLocale(), RuleBookFile.convertToPages(book));
            }
            catch(IOException ex)
            {
                throw new IllegalStateException(ex);
            }
        }
    }

    public Language getLanguage(String name)
    {
        Set<Language> languages = i18n.searchLanguages(name, 2);
        if(languages.size() == 1)
        {
            return languages.iterator().next();
        }
        return null;
    }

    public Collection<Locale> getLocales()
    {
        return this.rulebooks.keySet();
    }

    public boolean contains(Locale locale)
    {
        return this.rulebooks.containsKey(locale);
    }

    public String[] getPages(Locale locale)
    {
        return this.rulebooks.get(locale);
    }

    public ItemStack getBook(Locale locale)
    {
        if (!this.contains(locale))
        {
            locale = Locale.getDefault();
        }
        ItemStack ruleBook = ItemStack.of(ItemTypes.WRITTEN_BOOK, 1);
        ruleBook.offer(Keys.BOOK_AUTHOR, Text.of("Server"));
        ruleBook.offer(Keys.DISPLAY_NAME, i18n.getTranslation(locale, POSITIVE, "rulebook"));
        ruleBook.offer(Keys.BOOK_PAGES, stream(getPages(locale)).map(Text::of).collect(toList()));
        // TODO add custom data
        ruleBook.offer(Keys.ITEM_LORE, singletonList(Text.of(locale.getLanguage())));
        return ruleBook;
    }

    public void removeBook(Locale locale) throws IOException
    {
        for(Path file : RuleBookFile.getLanguageFiles(i18n, this.module.getFolder()))
        {
            Locale fileLocale = this.getLanguage(StringUtils.stripFileExtension(file.getFileName().toString())).getLocale();
            if(fileLocale.equals(locale))
            {
                Files.delete(file);
            }
        }

        this.rulebooks.remove(locale);
    }

    public void addBook(ItemStack book, Locale locale)
    {
        if(!this.contains(locale))
        {
            try
            {
                Path file = this.module.getFolder().resolve(locale.getDisplayLanguage() + ".txt");
                List<String> list = book.get(Keys.BOOK_PAGES).get().stream().map(Text::toPlain).collect(toList());
                RuleBookFile.createFile(file, list.toArray(new String[list.size()]));
                this.rulebooks.put(locale, RuleBookFile.convertToPages(file));
            }
            catch(IOException ex)
            {
                throw new IllegalStateException(ex);
            }
        }
    }
}
