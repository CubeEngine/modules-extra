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
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Greed;
import de.cubeisland.engine.butler.parametric.Label;
import de.cubeisland.engine.butler.parametric.Optional;
import org.cubeengine.module.chat.Chat;
import org.cubeengine.module.core.util.ChatFormat;
import org.cubeengine.service.command.CommandContext;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.user.Broadcaster;
import org.cubeengine.service.user.MultilingualCommandSource;
import org.cubeengine.service.user.MultilingualPlayer;
import org.cubeengine.service.user.UserManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.manipulator.mutable.DisplayNameData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.source.ConsoleSource;

import static de.cubeisland.engine.butler.parameter.Parameter.INFINITE;
import static org.cubeengine.service.i18n.formatter.MessageType.*;


public class ChatCommands
{
    private final Chat module;
    private Game game;
    private UserManager um;
    private CommandManager cm;
    private I18n i18n;
    private Broadcaster bc;
    private UUID consoleUUID = UUID.fromString(":console");

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
    public void me(MultilingualCommandSource context, @Greed(INFINITE) String message)
    {
        bc.broadcastStatus(message, context.getSource());
    }

    @Command(desc = "Changes your display name")
    public void nick(MultilingualCommandSource context, @Label("<name>|-reset") String name, @Optional Player player)
    {
        if (player == null)
        {
            if (!(context instanceof MultilingualPlayer))
            {
                context.sendTranslated(NEGATIVE, "You cannot change the consoles display name"); // TODO You cannot?!? why oh whyy
                return;
            }
            player = ((MultilingualPlayer)context).original();
        }

        if (!context.equals(player) && !context.getSource().hasPermission(module.perms().COMMAND_NICK_OTHER.getId()))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to change the nickname of another player!");
            return;
        }

        if ("-r".equalsIgnoreCase(name) || "-reset".equalsIgnoreCase(name))
        {
            DisplayNameData display = player.getOrCreate(DisplayNameData.class).get();
            display.displayName().set(Texts.of(context.getSource().getName()));
            player.offer(display);
            context.sendTranslated(POSITIVE, "Display name reset to {user}", context);
            return;
        }
        if (um.getByName(name).isPresent() && !context.hasPermission(module.perms().COMMAND_NICK_OFOTHER.getId()))
        {
            context.sendTranslated(NEGATIVE, "This name has been taken by another player!");
            return;
        }
        context.sendTranslated(POSITIVE, "Display name changed from {user} to {user}", context, name);
        DisplayNameData display = player.getOrCreate(DisplayNameData.class).get();
        display.displayName().set(ChatFormat.fromLegacy(name, '&'));
        player.offer(display);
    }

    @Command(desc = "Sends a private message to someone", alias = {"tell", "message", "pm", "m", "t", "whisper", "w"})
    public void msg(MultilingualCommandSource context, CommandSource player, @Greed(INFINITE) String message)
    {
        if (!this.sendWhisperTo(player, message, context))
        {
            context.sendTranslated(NEGATIVE, "Could not find the player {user} to send the message to. Is the player offline?", player);
        }
    }

    @Command(alias = "r", desc = "Replies to the last person that whispered to you.")
    public void reply(MultilingualCommandSource context, @Greed(INFINITE) String message)
    {
        UUID lastWhisper;
        if (context instanceof MultilingualPlayer)
        {
            lastWhisper = lastWhispers.get(((MultilingualPlayer)context).original().getUniqueId());
        }
        else
        {
            lastWhisper = lastWhispers.get(consoleUUID);
        }
        if (lastWhisper == null)
        {
            context.sendTranslated(NEUTRAL, "No one has sent you a message that you could reply to!");
            return;
        }
        CommandSource target;
        if (lastWhisper.equals(consoleUUID))
        {
            target = game.getServer().getConsole();
        }
        else
        {
            target = game.getServer().getPlayer(lastWhisper).orNull();
        }
        if (!this.sendWhisperTo(target, message, context))
        {
            context.sendTranslated(NEGATIVE, "Could not find the player to reply to. Is the player offline?");
        }
    }

    private boolean sendWhisperTo(CommandSource whisperTarget, String message, MultilingualCommandSource context)
    {
        if (whisperTarget == null)
        {
            return false;
        }
        if (whisperTarget instanceof ConsoleSource)
        {
            if (context instanceof ConsoleSource)
            {
                context.sendTranslated(NEUTRAL, "Talking to yourself?");
                return true;
            }
            if (context instanceof MultilingualPlayer)
            {
                i18n.sendTranslated(whisperTarget, NEUTRAL, "{sender} -> {text:You}: {message:color=WHITE}", context, message);
                context.sendTranslated(NEUTRAL, "{text:You} -> {user}: {message:color=WHITE}", whisperTarget.getName(),
                                       message);
                lastWhispers.put(consoleUUID, ((MultilingualPlayer)context).original().getUniqueId());
                lastWhispers.put(((MultilingualPlayer)context).original().getUniqueId(), consoleUUID);
                return true;
            }
            context.sendTranslated(NONE, "Who are you!?");
            return true;
        }

        if (whisperTarget instanceof Player)
        {
            MultilingualPlayer user = i18n.getMultilingual(((Player)whisperTarget));
            if (context.equals(user))
            {
                context.sendTranslated(NEUTRAL, "Talking to yourself?");
                return true;
            }
            user.sendTranslated(NONE, "{sender} -> {text:You}: {message:color=WHITE}", context.getSource().getName(),
                                message);
            /*
            if (user.get(ChatAttachment.class).isAfk()) // TODO afk auch hier rein?
            {
                context.sendTranslated(NEUTRAL, "{user} is afk!", user);
            }
            */
            context.sendTranslated(NEUTRAL, "{text:You} -> {user}: {message:color=WHITE}", user, message);
            if (context.getSource() instanceof Player)
            {
                lastWhispers.put(((Player)context.getSource()).getUniqueId(), user.getSource().getUniqueId());
            }
            else
            {
                lastWhispers.put(consoleUUID, user.getSource().getUniqueId());
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
    public void chatcolors(MultilingualCommandSource context)
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
        context.getSource().sendMessage(Texts.of(builder.toString()));
        context.sendTranslated(POSITIVE, "To use these type {text:&} followed by the code above");
    }


    @Command(alias = "roll", desc = "Shows a random number from 0 to 100")
    public void rand(CommandSource context)
    {
        this.bc.broadcastTranslatedStatus(NEUTRAL, "rolled a {integer}!", context, new Random().nextInt(100));
    }

}
