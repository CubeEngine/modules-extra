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
import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Disable;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.libcube.service.Broadcaster;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.entity.InvisibilityData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;

// TODO hide on Dynmap SpongeAPI
// TODO event for hide and show
// TODO contextual - can see hidden players
@ModuleInfo(name = "Hide", description = "Hide yourself from the world")
public class Hide extends Module
{
    private Set<UUID> hiddenUsers;

    public HidePerm perms()
    {
        return perms;
    }

    private HidePerm perms;

    @Inject private CommandManager cm;
    @Inject private EventManager em;
    @Inject private I18n i18n;
    @Inject private Broadcaster bc;
    @Inject private PermissionManager pm;

    @Enable
    public void onEnable()
    {
        hiddenUsers = new HashSet<>();
        // canSeeHiddens = new HashSet<>();
        cm.addCommands(cm, this, new HideCommands(this, i18n));
        em.registerListener(Hide.class, new HideListener(this, i18n));
        this.perms = new HidePerm(pm);
    }

    @Disable
    public void onDisable()
    {
        for (UUID hiddenId : hiddenUsers)
        {
            Sponge.getServer().getPlayer(hiddenId).ifPresent(this::showPlayer);
        }
        this.hiddenUsers.clear();
    }

    public void hidePlayer(final Player player)
    {
        player.offer(Keys.INVISIBLE, true);

        bc.broadcastTranslated(NEUTRAL, "{user:color=YELLOW} left the game", player);
        // can see hidden + msg
    }

    public void showPlayer(final User player)
    {
        player.remove(InvisibilityData.class);

        bc.broadcastTranslated(NEUTRAL, "{user:color=YELLOW} joined the game", player);
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
