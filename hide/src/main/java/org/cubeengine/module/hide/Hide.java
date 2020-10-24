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
package org.cubeengine.module.hide;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.Broadcaster;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.event.ModuleListener;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;

// TODO hide on Dynmap SpongeAPI
// TODO event for hide and show
// TODO contextual - can see hidden players
@Singleton
@Module
public class Hide
{
    private Set<UUID> hiddenUsers;

    @Inject private Broadcaster bc;

    @ModuleCommand private HideCommands hideCommands;
    @ModuleListener private HideListener listener;

    @Listener
    public void onEnable(StartedEngineEvent<Server> event)
    {
        hiddenUsers = new HashSet<>();
        // canSeeHiddens = new HashSet<>();
    }

    @Listener
    public void onDisable(StoppingEngineEvent<Server> event)
    {
        for (UUID hiddenId : hiddenUsers)
        {
            Sponge.getServer().getPlayer(hiddenId).ifPresent(p -> this.showPlayer(p, true));
        }
        this.hiddenUsers.clear();
    }

    public void hidePlayer(final Player player, boolean join)
    {
        this.hiddenUsers.add(player.getUniqueId());
        player.offer(Keys.IS_INVISIBLE, true);
        if (!join)
        {
            bc.broadcastTranslated(NEUTRAL, "{user:color=YELLOW} left the game", player.getName());
        }
        // can see hidden + msg
    }

    public void showPlayer(final ServerPlayer player, boolean quit)
    {
        player.remove(Keys.IS_INVISIBLE);
        if (!quit)
        {
            bc.broadcastTranslated(NEUTRAL, "{user:color=YELLOW} joined the game", player.getName());
        }
        // can see hidden + msg
    }

    public Set<UUID> getHiddenUsers()
    {
        return hiddenUsers;
    }

    public boolean isHidden(Player player)
    {
        return this.hiddenUsers.contains(player.getUniqueId());
    }
}
