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
package org.cubeengine.module.authorization;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parameter.TooFewArgumentsException;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Desc;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.command.annotation.CommandPermission;
import org.cubeengine.libcube.service.command.annotation.Unloggable;
import org.cubeengine.libcube.service.command.exception.PermissionDeniedException;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.command.parser.PlayerList;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.ban.Ban;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

import javax.inject.Inject;

public class AuthCommands
{
    private final Authorization module;
    private final BanService bs;
    private I18n i18n;

    private final ConcurrentHashMap<UUID, Long> fails = new ConcurrentHashMap<>();

    @Inject
    public AuthCommands(Authorization module, I18n i18n)
    {
        this.module = module;
        this.i18n = i18n;
        this.bs = Sponge.getServiceManager().provideUnchecked(BanService.class);
    }

    @Unloggable
    @Command(alias = "setpw", desc = "Sets your password.")
    public void setPassword(CommandSource context, String password, @Default Player player)
    {
        if ((context.equals(player)))
        {
            module.setPassword(player.getUniqueId(), password);
            i18n.send(context, POSITIVE, "Your password has been set!");
            return;
        }
        if (!context.hasPermission(module.perms().COMMAND_SETPASSWORD_OTHER.getId()))
        {
            throw new PermissionDeniedException(module.perms().COMMAND_SETPASSWORD_OTHER);
        }
        module.setPassword(player.getUniqueId(), password);
        i18n.send(context, POSITIVE, "{user}'s password has been set!", player);
    }

    @Command(alias = "clearpw", desc = "Clears your password.")
    public void clearPassword(CommandSource context,
                              @Optional @Desc("* or a list of Players delimited by ,") PlayerList players)
    {
        if (players == null)
        {
            if (!(context instanceof Player))
            {
                throw new TooFewArgumentsException();
            }
            module.resetPassword(((Player)context).getUniqueId());
            i18n.send(context, POSITIVE, "Your password has been reset!");
            return;
        }
        if (players.isAll())
        {
            if (!context.hasPermission(module.perms().COMMAND_CLEARPASSWORD_ALL.getId()))
            {
                throw new PermissionDeniedException(module.perms().COMMAND_CLEARPASSWORD_ALL);
            }
            module.resetAllPasswords();
            i18n.send(context, POSITIVE, "All passwords reset!");
            return;
        }
        if (!context.hasPermission(module.perms().COMMAND_CLEARPASSWORD_OTHER.getId()))
        {
            throw new PermissionDeniedException(module.perms().COMMAND_CLEARPASSWORD_OTHER);
        }
        for (Player user : players.list())
        {
            module.resetPassword(user.getUniqueId());
            i18n.send(context, POSITIVE, "{user}'s password has been reset!", user.getName());
        }
    }

    @Unloggable
    @Command(desc = "Logs you in with your password!")
    @CommandPermission(checkPermission = false) // TODO assign by default
    @Restricted(value = Player.class, msg = "Only players can log in!")
    public void login(Player context, String password)
    {
        if (module.isLoggedIn(context.getUniqueId()))
        {
            i18n.send(context, POSITIVE, "You are already logged in!");
            return;
        }
        boolean isLoggedIn = module.login(context, password);
        if (isLoggedIn)
        {
            i18n.send(context, POSITIVE, "You logged in successfully!");
            return;
        }
        i18n.send(context, NEGATIVE, "Wrong password!");
        if (module.getConfig().fail2ban)
        {
            if (fails.get(context.getUniqueId()) != null)
            {
                if (fails.get(context.getUniqueId()) + SECONDS.toMillis(10) > currentTimeMillis())
                {
                    Text msg = Text.of(i18n.translate(context, NEGATIVE, "Too many wrong passwords!") + "\n"
                            + i18n.translate(context, NEUTRAL, "For your security you were banned 10 seconds."));
                    Instant expires = Instant.now().plus(module.getConfig().banDuration, ChronoUnit.SECONDS);
                    this.bs.addBan(Ban.builder().profile(context.getProfile()).reason(msg)
                                      .expirationDate(expires).source(context).build());
                    if (!Sponge.getServer().getOnlineMode())
                    {
                        this.bs.addBan(Ban.builder().address(context.getConnection().getAddress().getAddress()).reason(msg)
                                          .expirationDate(expires).source(context).build());
                    }
                    context.kick(msg);
                }
            }
            fails.put(context.getUniqueId(), currentTimeMillis());
        }
    }

    @Command(desc = "Logs you out!")
    @Restricted(value = Player.class, msg = "You might use /stop for this.")
    public void logout(Player context)
    {
        if (module.isLoggedIn(context.getUniqueId()))
        {
            module.logout(context.getUniqueId());
            i18n.send(context, POSITIVE, "You're now logged out.");
            return;
        }
        i18n.send(context, NEUTRAL, "You're not logged in!");
    }
}
