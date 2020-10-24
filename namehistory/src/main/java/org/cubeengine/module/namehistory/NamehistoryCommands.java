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
package org.cubeengine.module.namehistory;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.User;
import java.text.SimpleDateFormat;

@Singleton
public class NamehistoryCommands
{
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    private I18n i18n;

    @Inject
    public NamehistoryCommands(I18n i18n)
    {
        this.i18n = i18n;
    }

    @Command(desc = "Shows the namehistory of a player")
    public void namehistory(CommandCause context, @Default User player)
    {
        HistoryFetcher.get(player.getUniqueId()).thenAccept(historyData -> {
            if (!historyData.isPresent() || historyData.get().names.size() <= 1)
            {
                i18n.send(context.getAudience(), NEGATIVE, "No namehistory available for {user}", player);
                return;
            }
            i18n.send(context.getAudience(), POSITIVE, "The following names were known for {user}", player);
            for (NameChange names : historyData.get().names)
            {
                final String accountCreation = names.changedToAt.isPresent() ?
                                               sdf.format(names.changedToAt.get().getTime()) :
                                               i18n.getTranslation(context.getAudience(), "account creation");
                i18n.send(context.getAudience(), NEUTRAL, " - {user} since {input}", names.name, accountCreation);
            }
        });
    }
}
