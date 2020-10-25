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
package org.cubeengine.module.signmarket;

import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.entity.living.player.Player;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Command(name = "marketsign", desc = "MarketSign-Commands", alias = {"signmarket", "market"})
public class SignMarketCommands extends ContainerCommand
{
    private final Signmarket module;
    private I18n i18n;

    public SignMarketCommands(CommandManager base, Signmarket module, I18n i18n)
    {
        super(base, Signmarket.class);
        this.module = module;
        this.i18n = i18n;
    }

    @Alias(value = "medit")
    @Command(alias = "edit", desc = "Enters the editmode allowing to change market signs easily")
    @Restricted(value = Player.class, msg = "Only players can edit market signs!")
    public void editMode(Player context)
    {
        if (this.module.getEditModeCommand().hasUser(context))
        {
            this.module.getEditModeCommand().removeUser(context);
            return;
        }
        this.module.getEditModeCommand().addUser(context);
        i18n.send(context, POSITIVE, "You are now in edit mode!");
        i18n.send(context, POSITIVE, "Chat will now work as commands.");
        i18n.send(context, NEUTRAL, "Type exit or use this command again to leave the edit mode.");
    }
}
