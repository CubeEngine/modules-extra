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

import java.util.Arrays;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.ChatFormat;
import org.spongepowered.api.command.CommandCause;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Singleton
@Command(name = "edit", desc = "Edit kits")
public class KitEditCommand extends DispatcherCommand
{

    private I18n i18n;
    private KitManager manager;

    @Inject
    public KitEditCommand(I18n i18n, KitManager manager)
    {
        super(Kits.class);
        this.i18n = i18n;
        this.manager = manager;
    }

    @Command(desc = "Controls if this kit is given to new players")
    public void giveOnFirstJoin(CommandCause context, Kit kit, boolean value)
    {
        kit.setGiveKitOnFirstJoin(value);
        manager.saveKit(kit);
        if (value)
        {
            i18n.send(context.getAudience(), POSITIVE, "New players automatically receive the kit {name}.", kit.getKitName());
            return;
        }
        i18n.send(context.getAudience(), POSITIVE, "New players do not automatically receive the kit {name}.", kit.getKitName());
    }

    @Command(desc = "Sets the custom message")
    public void customMessage(CommandCause context, Kit kit, @Option String value)
    {
        kit.setCustomMessage(value);
        manager.saveKit(kit);
        if (value != null)
        {

            i18n.send(context.getAudience(), POSITIVE, "The custom message for {name} is now {txt}.", kit.getKitName(), ChatFormat.fromLegacy(value, '&'));
            return;
        }

        i18n.send(context.getAudience(), POSITIVE, "The custom message for {name} was removed.", kit.getKitName());
    }

    @Command(desc = "Sets commands to be run when a kit is received")
    public void setCommand(CommandCause context, Kit kit, @Option String... commands)
    {
        kit.clearCommands();
        if (commands != null)
        {
            kit.setCommands(Arrays.asList(commands));
            i18n.send(context.getAudience(), POSITIVE, "Kit commands for {name} set.", kit.getKitName());
        }
        else
        {
            i18n.send(context.getAudience(), POSITIVE, "Kit commands for {name} removed.", kit.getKitName());
        }
        manager.saveKit(kit);
    }

    @Command(desc = "Controls permission check for a kit")
    public void kitPermission(CommandCause context, Kit kit, boolean value)
    {
        kit.setPermission(value);
        manager.saveKit(kit);
        if (value)
        {
            i18n.send(context.getAudience(), POSITIVE, "A permission is needed for the kit {name}.", kit.getKitName());
            return;
        }
        i18n.send(context.getAudience(), POSITIVE, "No permission is needed for the kit {name}.", kit.getKitName());
    }

    @Command(desc = "Controls the limit for receiving a kit")
    public void limitUsage(CommandCause context, Kit kit, int value)
    {
        if (value < 0)
        {
            value = 0;
        }

        kit.setLimitUsage(value);
        manager.saveKit(kit);
        if (value > 0)
        {
            i18n.send(context.getAudience(), POSITIVE, "The kit {name} can be received {amount} times.", kit.getKitName(), value);
            return;
        }
        i18n.send(context.getAudience(), POSITIVE, "The kit {name} can be received an unlimited amount of times.", kit.getKitName());
    }

    @Command(desc = "Controls the minimum delay between receiving a kit in seconds")
    public void usageDelay(CommandCause context, Kit kit, int value)
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
            i18n.send(context.getAudience(), POSITIVE, "The kit {name} can be received every {amount} seconds.", kit.getKitName(), value);
            return;
        }
        i18n.send(context.getAudience(), POSITIVE, "The kit {name} can be received without delays.", kit.getKitName());
    }

}
