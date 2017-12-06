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
package org.cubeengine.module.module.kits;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.Arrays;

@Command(name = "edit", desc = "Edit kits")
public class KitEditCommand extends ContainerCommand
{

    private I18n i18n;
    private KitManager manager;

    public KitEditCommand(CommandManager cm, I18n i18n, KitManager manager)
    {
        super(cm, Kits.class);
        this.i18n = i18n;
        this.manager = manager;
    }

    @Command(desc = "Controls if this kit is given to new players")
    public void giveOnFirstJoin(CommandSource context, Kit kit, boolean value)
    {
        kit.setGiveKitOnFirstJoin(value);
        manager.saveKit(kit);
        if (value)
        {
            i18n.send(context, POSITIVE, "New players automatically receive the kit {name}.", kit.getKitName());
            return;
        }
        i18n.send(context, POSITIVE, "New players do not automatically receive the kit {name}.", kit.getKitName());
    }

    @Command(desc = "Sets the custom message")
    public void customMessage(CommandSource context, Kit kit, @Optional String value)
    {
        kit.setCustomMessage(value);
        manager.saveKit(kit);
        if (value != null)
        {

            i18n.send(context, POSITIVE, "The custom message for {name} is now {txt}.", kit.getKitName(), TextSerializers.FORMATTING_CODE.deserialize(value));
            return;
        }

        i18n.send(context, POSITIVE, "The custom message for {name} was removed.", kit.getKitName());
    }

    @Command(desc = "Sets commands to be run when a kit is received")
    public void setCommand(CommandSource context, Kit kit, @org.cubeengine.butler.parametric.Optional String... commands)
    {
        kit.clearCommands();
        if (commands != null)
        {
            kit.setCommands(Arrays.asList(commands));
            i18n.send(context, POSITIVE, "Kit commands for {name} set.", kit.getKitName());
        }
        else
        {
            i18n.send(context, POSITIVE, "Kit commands for {name} removed.", kit.getKitName());
        }
        manager.saveKit(kit);
    }

    @Command(desc = "Controls permission check for a kit")
    public void kitPermission(CommandSource context, Kit kit, boolean value)
    {
        kit.setPermission(value);
        manager.saveKit(kit);
        if (value)
        {
            i18n.send(context, POSITIVE, "A permission is needed for the kit {name}.", kit.getKitName());
            return;
        }
        i18n.send(context, POSITIVE, "No permission is needed for the kit {name}.", kit.getKitName());
    }

    @Command(desc = "Controls the limit for receiving a kit")
    public void limitUsage(CommandSource context, Kit kit, int value)
    {
        if (value < 0)
        {
            value = 0;
        }

        kit.setLimitUsage(value);
        manager.saveKit(kit);
        if (value > 0)
        {
            i18n.send(context, POSITIVE, "The kit {name} can be received {amount} times.", kit.getKitName(), value);
            return;
        }
        i18n.send(context, POSITIVE, "The kit {name} can be received an unlimited amount of times.", kit.getKitName());
    }

    @Command(desc = "Controls the minimum delay between receiving a kit in seconds")
    public void usageDelay(CommandSource context, Kit kit, int value)
    {
        if (value < 0)
        {
            value = 0;
        }
        value *= 1000;

        kit.setUsageDelay(value);
        manager.saveKit(kit);
        if (value > 0)
        {
            i18n.send(context, POSITIVE, "The kit {name} can be received every {amount} seconds.", kit.getKitName(), value);
            return;
        }
        i18n.send(context, POSITIVE, "The kit {name} can be received without delays.", kit.getKitName());
    }

}
