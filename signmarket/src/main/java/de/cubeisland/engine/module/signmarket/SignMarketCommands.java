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
package de.cubeisland.engine.module.signmarket;

import de.cubeisland.engine.core.command.CommandContainer;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.command.alias.Alias;
import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.core.user.User;

import static de.cubeisland.engine.core.util.formatter.MessageType.*;

@Command(name = "marketsign", desc = "MarketSign-Commands", alias = {"signmarket", "market"})
public class SignMarketCommands extends CommandContainer
{
    private final Signmarket module;

    public SignMarketCommands(Signmarket module)
    {
        super(module);
        this.module = module;
    }

    @Alias(names = "medit")
    @Command(alias = "edit", desc = "Enters the editmode allowing to change market signs easily")
    public void editMode(CommandContext context)
    {
        if (context.getSource() instanceof User)
        {
            if (this.module.getEditModeListener().hasUser((User)context.getSource()))
            {
                this.module.getEditModeListener().removeUser((User)context.getSource());
            }
            else
            {
                if (this.module.getConfig().disableInWorlds.contains(((User)context.getSource()).getWorld().getName()))
                {
                    context.sendTranslated(NEUTRAL, "MarketSigns are disabled in the configuration for this world!");
                    return;
                }
                this.module.getEditModeListener().addUser((User)context.getSource());
                context.sendTranslated(POSITIVE, "You are now in edit mode!");
                context.sendTranslated(POSITIVE, "Chat will now work as commands.");
                context.sendTranslated(NEUTRAL, "Type exit or use this command again to leave the edit mode.");
            }
        }
        else
        {
            context.sendTranslated(NEGATIVE, "Only players can edit market signs!");
        }
    }
}
