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
package de.cubeisland.engine.module.chat.command;

import java.util.UUID;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Default;
import de.cubeisland.engine.butler.parametric.Greed;
import de.cubeisland.engine.butler.parametric.Label;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.module.chat.Chat;
import de.cubeisland.engine.module.chat.ChatAttachment;
import de.cubeisland.engine.module.chat.listener.AfkListener;
import de.cubeisland.engine.module.core.util.ChatFormat;
import de.cubeisland.engine.module.service.command.CommandContext;
import de.cubeisland.engine.module.service.command.CommandManager;
import de.cubeisland.engine.module.service.command.CommandSender;
import de.cubeisland.engine.module.service.command.sender.ConsoleCommandSender;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.service.user.UserManager;
import org.spongepowered.api.data.manipulator.DisplayNameData;
import org.spongepowered.api.text.Texts;

import static de.cubeisland.engine.butler.parameter.Parameter.INFINITE;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.*;
import static de.cubeisland.engine.module.service.command.CommandSender.NON_PLAYER_UUID;

public class ChatCommands
{
    private final Chat module;
    private UserManager um;
    private CommandManager cm;
    private AfkListener afkListener;
    private UUID lastWhisperOfConsole = null;

    public ChatCommands(Chat module, UserManager um, CommandManager cm)
    {
        this.module = module;
        this.um = um;
        this.cm = cm;
        this.afkListener = afkListener;
    }

    @Command(desc = "Allows you to emote")
    public void me(CommandSender context, @Greed(INFINITE) String message)
    {
        um.broadcastStatus(message, context);  // TODO message can be null somehow
    }

    @Command(desc = "Changes your display name")
    public void nick(CommandSender context, @Label("<name>|-reset") String name, @Optional User player)
    {
        if (player == null)
        {
            if (!(context instanceof User))
            {
                context.sendTranslated(NEGATIVE, "You cannot change the consoles display name"); // TODO You cannot?!?
                return;
            }
            player = (User)context;
        }

        if (!context.equals(player) && !module.perms().COMMAND_NICK_OTHER.isAuthorized(context))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to change the nickname of another player!");
            return;
        }

        if ("-r".equalsIgnoreCase(name) || "-reset".equalsIgnoreCase(name))
        {
            DisplayNameData display = player.asPlayer().getOrCreate(DisplayNameData.class).get();
            display.setDisplayName(Texts.of(context.getName()));
            player.asPlayer().offer(display);
            context.sendTranslated(POSITIVE, "Display name reset to {user}", context);
            return;
        }
        if (um.findExactUser(name) != null && !module.perms().COMMAND_NICK_OFOTHER.isAuthorized(context))
        {
            context.sendTranslated(NEGATIVE, "This name has been taken by another player!");
            return;
        }
        context.sendTranslated(POSITIVE, "Display name changed from {user} to {user}", context, name);
        DisplayNameData display = player.asPlayer().getOrCreate(DisplayNameData.class).get();
        display.setDisplayName(ChatFormat.fromLegacy(name, '&'));
        player.asPlayer().offer(display);
    }

    @Command(desc = "Sends a private message to someone", alias = {"tell", "message", "pm", "m", "t", "whisper", "w"})
    public void msg(CommandContext context, CommandSender player, @Greed(INFINITE) String message)
    {
        if (player instanceof ConsoleCommandSender)
        {
            sendWhisperTo(NON_PLAYER_UUID, message, context.getSource());
            return;
        }
        if (!this.sendWhisperTo(player.getUniqueId(), message, context.getSource()))
        {
            context.sendTranslated(NEGATIVE, "Could not find the player {user} to send the message to. Is the player offline?", player);
        }
    }

    @Command(alias = "r", desc = "Replies to the last person that whispered to you.")
    public void reply(CommandSender context, @Greed(INFINITE) String message)
    {
        UUID lastWhisper;
        if (context instanceof User)
        {
            lastWhisper = ((User)context).get(ChatAttachment.class).getLastWhisper();
        }
        else
        {
            lastWhisper = lastWhisperOfConsole;
        }
        if (lastWhisper == null)
        {
            context.sendTranslated(NEUTRAL, "No one has sent you a message that you could reply to!");
            return;
        }
        if (!this.sendWhisperTo(lastWhisper, message, context))
        {
            context.sendTranslated(NEGATIVE, "Could not find the player to reply to. Is the player offline?");
        }
    }

    private boolean sendWhisperTo(UUID whisperTarget, String message, CommandSender context)
    {
        if (NON_PLAYER_UUID.equals(whisperTarget))
        {
            if (context instanceof ConsoleCommandSender)
            {
                context.sendTranslated(NEUTRAL, "Talking to yourself?");
                return true;
            }
            if (context instanceof User)
            {
                ConsoleCommandSender console = cm.getConsoleSender();
                console.sendTranslated(NEUTRAL, "{sender} -> {text:You}: {message:color=WHITE}", context, message);
                context.sendTranslated(NEUTRAL, "{text:You} -> {user}: {message:color=WHITE}", Texts.toPlain(console.getDisplayName()), message);
                this.lastWhisperOfConsole = context.getUniqueId();
                ((User)context).get(ChatAttachment.class).setLastWhisper(NON_PLAYER_UUID);
                return true;
            }
            context.sendTranslated(NONE, "Who are you!?");
            return true;
        }
        User user = um.getExactUser(whisperTarget);
        if (!user.getPlayer().isPresent())
        {
            return false;
        }
        if (context.equals(user))
        {
            context.sendTranslated(NEUTRAL, "Talking to yourself?");
            return true;
        }
        user.sendTranslated(NONE, "{sender} -> {text:You}: {message:color=WHITE}", context.getName(), message);
        if (user.get(ChatAttachment.class).isAfk()) // TODO afk auch hier rein?
        {
            context.sendTranslated(NEUTRAL, "{user} is afk!", user);
        }
        context.sendTranslated(NEUTRAL, "{text:You} -> {user}: {message:color=WHITE}", user, message);
        if (context instanceof User)
        {
            ((User)context).get(ChatAttachment.class).setLastWhisper(user.getUniqueId());
        }
        else
        {
            this.lastWhisperOfConsole = user.getUniqueId();
        }
        user.get(ChatAttachment.class).setLastWhisper(context.getUniqueId());
        return true;
    }

    @Command(desc = "Broadcasts a message")
    public void broadcast(CommandContext context, @Greed(INFINITE) String message)
    {
        this.um.broadcastMessage(NEUTRAL, "[{text:Broadcast}] {input}", message);
    }



    @Command(desc = "Displays the colors")
    public void chatcolors(CommandSender context)
    {
        context.sendTranslated(POSITIVE, "The following chat codes are available:");
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
        context.sendMessage(builder.toString());
        context.sendTranslated(POSITIVE, "To use these type {text:&} followed by the code above");
    }



}
