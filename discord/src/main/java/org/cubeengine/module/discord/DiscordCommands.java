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
package org.cubeengine.module.discord;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Command(name = "discord", desc = "Interact with the Discord bridge")
@Singleton
public class DiscordCommands extends DispatcherCommand
{
    private final Discord module;
    private I18n i18n;

    @Inject
    public DiscordCommands(Discord module, I18n i18n)
    {
        this.module = module;
        this.i18n = i18n;
    }

    @Command(desc = "Shows information to join the bot to a Discord server")
    public void join(CommandCause context)
    {
        final String appId = module.getConfig().applicationId;
        if (appId == null) {
            i18n.send(context, NEGATIVE, "No application ID has been configured. Without that, I can't help you.");
            return;
        }

        String url = "https://discord.com/api/oauth2/authorize?client_id=" + appId + "&permissions=0&scope=bot";

        i18n.send(context, POSITIVE, "Open this link to add the bot to your Discord Server: {url}", url);
    }


    @Command(desc = "Mutes the Discord chat for you")
    public void mute(CommandCause context, @Default ServerPlayer player)
    {
        final boolean isMuted = player.get(DiscordData.MUTED).orElse(false);
        if (isMuted) {
            player.offer(DiscordData.MUTED, true);
            i18n.send(context, POSITIVE, "You will no longer see any Discord messages until you run this command again.");
        } else {
            player.offer(DiscordData.MUTED, false);
            i18n.send(context, POSITIVE, "You will now see messages from Discord again!");
        }
    }
}
