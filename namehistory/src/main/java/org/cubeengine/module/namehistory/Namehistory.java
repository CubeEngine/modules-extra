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

import java.util.List;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.Broadcaster;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.processor.Module;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.DAYS;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Singleton
@Module
public class Namehistory
{
    @Inject private Broadcaster bc;

    @ModuleCommand private NamehistoryCommands namehistoryCommands;

    @Listener
    public void onJoin(ServerSideConnectionEvent.Join event, @First Player player)
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
}
