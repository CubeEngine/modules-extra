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

import java.text.SimpleDateFormat;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.Broadcaster;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.DAYS;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Singleton
@Module(id = "namehistory", name = "Namehistory", version = "1.0.0",
        description = "Tracks users changing names on your server",
        dependencies = @Dependency("cubeengine-core"),
        url = "http://cubeengine.org",
        authors = {"Anselm 'Faithcaio' Brehme", "Phillip Schichtel"})
public class Namehistory extends CubeEngineModule
{
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    @Inject private EventManager em;
    @Inject private CommandManager cm;
    @Inject private Broadcaster bc;
    @Inject private I18n i18n;

    @Listener
    public void onEnable(GamePreInitializationEvent event)
    {
        cm.addCommands(this, this);
    }

    @Listener
    public void onJoin(ClientConnectionEvent.Join event, @First Player player)
    {
        long days = 5;
        HistoryFetcher.get(player.getUniqueId()).thenAccept(historyData -> {
            if (historyData.isPresent())
            {
                List<NameChange> list = historyData.get().names;
                NameChange last = list.get(list.size() - 1);
                if (last.changedToAt.isPresent() && last.changedToAt.get().getTime() > currentTimeMillis() - DAYS.toMillis(days))
                {
                    bc.broadcastMessage(POSITIVE, "{name} was renamed to {user}", list.get(list.size() - 2).name, player);
                }
            }
        });
    }

    @Command(desc = "Shows the namehistory of a player")
    public void namehistory(CommandSource context, @Default User player)
    {
        HistoryFetcher.get(player.getUniqueId()).thenAccept(historyData -> {
            if (!historyData.isPresent() || historyData.get().names.size() <= 1)
            {
                i18n.send(context, NEGATIVE, "No namehistory available for {user}", player);
                return;
            }
            i18n.send(context, POSITIVE, "The following names were known for {user}", player);
            for (NameChange names : historyData.get().names)
            {
                i18n.send(context, NEUTRAL, " - {user} since {input}", names.name,
                    names.changedToAt.isPresent() ? sdf.format(names.changedToAt.get().getTime()) : i18n.translate(context, NONE, "account creation").toPlain());
            }
        });
    }
}
