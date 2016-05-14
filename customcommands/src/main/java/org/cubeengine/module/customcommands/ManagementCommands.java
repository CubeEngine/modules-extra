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
package org.cubeengine.module.customcommands;

import java.util.List;
import java.util.stream.Collectors;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Complete;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Greed;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;

import static java.util.Locale.ENGLISH;
import static org.cubeengine.butler.parameter.Parameter.INFINITE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Command(name = "customcommands", desc = "Commands to modify custom commands.")
public class ManagementCommands extends ContainerCommand
{
    private I18n i18n;
    private final CustomCommandsConfig config;

    public ManagementCommands(Customcommands module, I18n i18n, CommandManager cm)
    {
        super(cm, Customcommands.class);
        this.i18n = i18n;
        this.config = module.getConfig();
    }

    @Command(desc = "Adds a custom chat command.")
    public void add(CommandSource context, String name, @Greed(INFINITE) String message, @Flag boolean force, @Flag boolean global)
    {
        if (config.commands.containsKey(name))
        {
            if (!force)
            {
                i18n.sendTranslated(context, NEGATIVE, "Custom command {input} already exists. Set the flag {text:-force} if you want to replace the message.", "!" + name);
                return;
            }
            config.commands.put(name, message);
            i18n.sendTranslated(context, POSITIVE, "Custom command {input} has successfully been replaced.", "!" + name);
        }
        else
        {
            config.commands.put(name.toLowerCase(ENGLISH), message);
            i18n.sendTranslated(context, POSITIVE, "Custom command {input} has successfully been added.", "!" + name);
        }
        config.save();
    }

    @Command(desc = "Deletes a custom chat command.")
    public void delete(CommandSource context, @Complete(CustomCommandCompleter.class)String name, @Flag boolean global)
    {
        if (config.commands.containsKey(name))
        {
            config.commands.remove(name.toLowerCase(ENGLISH));
            config.save();

            i18n.sendTranslated(context, POSITIVE, "Custom command {input} has successfully been deleted.", "!" + name);
        }
        else
        {
            i18n.sendTranslated(context, NEGATIVE, "Custom command {input} has not been found.", "!" + name);
        }
    }


    @Command(name = "help", desc = "Prints out all the custom chat commands.")
    public void showHelp(CommandSource context)
    {
        List<Text> list = config.commands.entrySet().stream()
             .map(e -> Text.of("!", e.getKey(), " -> ", e.getValue()))
             .collect(Collectors.toList());

        PaginationList pages = PaginationList.builder().contents(list).build();
        pages.sendTo(context);
    }
}
