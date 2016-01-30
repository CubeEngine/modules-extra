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
package org.cubeengine.module.namehistory;

import java.sql.Date;
import java.text.SimpleDateFormat;
import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.event.EventManager;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.i18n.formatter.MessageType;
import org.cubeengine.service.user.Broadcaster;
import org.jooq.DSLContext;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.profile.GameProfileManager;

import static org.cubeengine.service.i18n.formatter.MessageType.*;

@ModuleInfo(name = "Namehistory", description = "Tracks users changing names on your server")
public class Namehistory extends Module
{
    @Inject private EventManager em;
    @Inject private CommandManager cm;
    @Inject private Broadcaster bc;
    @Inject private I18n i18n;

    @Enable
    public void onEnable()
    {
        em.registerListener(this, this);
        cm.addCommands(this, this);
    }

    @Listener
    public void onJoin(ClientConnectionEvent.Join event, @First Player player)
    {
        // TODO get nameHistory from Sponge
        if (changed in last x days configurable)
        {
            bc.broadcastMessage(POSITIVE, "{name} was renamed to {user}", lastName, player);
        }
    }

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    @Command(desc = "Shows the namehistory of a player")
    public void namehistory(CommandSource context, @Default User player)
    {
        // TODO get nameHistory from Sponge

        if (isEmpty)
        {
            i18n.sendTranslated(context,  NEGATIVE, "No NameHistory available for {user}", player);
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "The following names were known for {user}", player);

        i18n.sendTranslated(context, NEUTRAL," - {user} since {input}",
                            entry.getValue(TABLE_NAMEHISTORY.NAME), 0 >= value.getTime() ?
                               i18n.getTranslation(context, NONE, "account creation") :
                               sdf.format(value));
    }
}
