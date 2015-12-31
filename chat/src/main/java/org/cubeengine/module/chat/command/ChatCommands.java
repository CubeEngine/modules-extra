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
package org.cubeengine.module.chat.command;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Greed;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.chat.Chat;
import org.cubeengine.module.core.util.ChatFormat;
import org.cubeengine.service.command.CommandContext;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.user.Broadcaster;
import org.cubeengine.service.user.UserManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.data.manipulator.mutable.DisplayNameData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;

import static org.cubeengine.butler.parameter.Parameter.INFINITE;
import static org.cubeengine.service.i18n.formatter.MessageType.*;


public class ChatCommands
{
    private final Chat module;
    private Game game;
    private UserManager um;
    private CommandManager cm;
    private I18n i18n;
    private Broadcaster bc;
    private UUID consoleUUID = UUID.nameUUIDFromBytes(":console".getBytes());

    private Map<UUID, UUID> lastWhispers = new HashMap<>();

    public ChatCommands(Chat module, Game game, UserManager um, CommandManager cm, I18n i18n, Broadcaster broadcaster)
    {
        this.module = module;
        this.game = game;
        this.um = um;
        this.cm = cm;
        this.i18n = i18n;
        this.bc = broadcaster;
    }

    @Command(desc = "Allows you to emote")
    public void me(CommandSource context, @Greed(INFINITE) String message)
    {
        bc.broadcastStatus(message, context);
    }

    @Command(desc = "Changes your display name")
    public void nick(CommandSource context, @Label("<name>|-reset") String name, @Optional Player player)
    {
        if (player == null)
        {
            if (!(context instanceof Player))
            {
                i18n.sendTranslated(context, NEGATIVE, "You cannot change the consoles display name"); // TODO You cannot?!? why oh whyy
                return;
            }
            player = ((Player)context);
        }

        if (!context.equals(player) && !context.hasPermission(module.perms().COMMAND_NICK_OTHER.getId()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to change the nickname of another player!");
            return;
        }

        if ("-r".equalsIgnoreCase(name) || "-reset".equalsIgnoreCase(name))
        {
            DisplayNameData display = player.getOrCreate(DisplayNameData.class).get();
            display.displayName().set(Text.of(context.getName()));
            player.offer(display);
            i18n.sendTranslated(context, POSITIVE, "Display name reset to {user}", context);
            return;
        }
        if (um.getByName(name).isPresent() && !context.hasPermission(module.perms().COMMAND_NICK_OFOTHER.getId()))
        {
            i18n.sendTranslated(context, NEGATIVE, "This name has been taken by another player!");
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "Display name changed from {user} to {user}", context, name);
        DisplayNameData display = player.getOrCreate(DisplayNameData.class).get();
        display.displayName().set(ChatFormat.fromLegacy(name, '&'));
        player.offer(display);
    }

    @Command(desc = "Sends a private message to someone", alias = {"tell", "message", "pm", "m", "t", "whisper", "w"})
    public void msg(CommandSource context, CommandSource player, @Greed(INFINITE) String message)
    {
        if (!this.sendWhisperTo(player, message, context))
        {
            i18n.sendTranslated(context, NEGATIVE, "Could not find the player {user} to send the message to. Is the player offline?", player);
        }
    }

    @Command(alias = "r", desc = "Replies to the last person that whispered to you.")
    public void reply(CommandSource context, @Greed(INFINITE) String message)
    {
        UUID lastWhisper;
        if (context instanceof Player)
        {
            lastWhisper = lastWhispers.get(((Player)context).getUniqueId());
        }
        else
        {
            lastWhisper = lastWhispers.get(consoleUUID);
        }
        if (lastWhisper == null)
        {
            i18n.sendTranslated(context, NEUTRAL, "No one has sent you a message that you could reply to!");
            return;
        }
        CommandSource target;
        if (lastWhisper.equals(consoleUUID))
        {
            target = game.getServer().getConsole();
        }
        else
        {
            target = game.getServer().getPlayer(lastWhisper).orElse(null);
        }
        if (!this.sendWhisperTo(target, message, context))
        {
            i18n.sendTranslated(context, NEGATIVE, "Could not find the player to reply to. Is the player offline?");
        }
    }

    private boolean sendWhisperTo(CommandSource whisperTarget, String message, CommandSource context)
    {
        if (whisperTarget == null)
        {
            return false;
        }
        if (whisperTarget instanceof ConsoleSource)
        {
            if (context instanceof ConsoleSource)
            {
                i18n.sendTranslated(context, NEUTRAL, "Talking to yourself?");
                return true;
            }
            if (context instanceof Player)
            {
                i18n.sendTranslated(whisperTarget, NEUTRAL, "{sender} -> {text:You}: {message:color=WHITE}", context,
                                    message);
                i18n.sendTranslated(context, NEUTRAL, "{text:You} -> {user}: {message:color=WHITE}",
                                    whisperTarget.getName(), message);
                lastWhispers.put(consoleUUID, ((Player)context).getUniqueId());
                lastWhispers.put(((Player)context).getUniqueId(), consoleUUID);
                return true;
            }
            i18n.sendTranslated(context, NONE, "Who are you!?");
            return true;
        }

        if (whisperTarget instanceof Player)
        {
            if (context.equals(whisperTarget))
            {
                i18n.sendTranslated(context, NEUTRAL, "Talking to yourself?");
                return true;
            }
            i18n.sendTranslated(whisperTarget, NONE, "{sender} -> {text:You}: {message:color=WHITE}", context.getName(), message);
            /*
            if (user.get(ChatAttachment.class).isAfk()) // TODO afk auch hier rein?
            {
                i18n.sendTranslated(context, NEUTRAL, "{user} is afk!", user);
            }
            */
            i18n.sendTranslated(context, NEUTRAL, "{text:You} -> {user}: {message:color=WHITE}", whisperTarget, message);
            if (context instanceof Player)
            {
                lastWhispers.put(((Player)context).getUniqueId(), ((Player)whisperTarget).getUniqueId());
            }
            else
            {
                lastWhispers.put(consoleUUID, ((Player)whisperTarget).getUniqueId());
            }
            return true;
        }
        return false;
    }

    @Command(desc = "Broadcasts a message")
    public void broadcast(CommandContext context, @Greed(INFINITE) String message)
    {
        this.bc.broadcastMessage(NEUTRAL, "[{text:Broadcast}] {input}", message);
    }

    @Command(desc = "Displays the colors")
    public void chatcolors(CommandSource context)
    {
        i18n.sendTranslated(context, POSITIVE, "The following chat codes are available:");
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (ChatFormat chatFormat : ChatFormat.values())
        {
            if (i++ % 3 == 0)
            {
                builder.append("\n");
            }
            builder.append(" ").append(chatFormat.getChar()).append(" ").append(chatFormat.toString()).append(chatFormat.name()).append(ChatFormat.RESET);
        }
        context.sendMessage(Text.of(builder.toString()));
        i18n.sendTranslated(context, POSITIVE, "To use these type {text:&} followed by the code above");
    }


    @Command(alias = "roll", desc = "Shows a random number from 0 to 100")
    public void rand(CommandSource context)
    {
        this.bc.broadcastTranslatedStatus(NEUTRAL, "rolled a {integer}!", context, new Random().nextInt(100));
    }

}
