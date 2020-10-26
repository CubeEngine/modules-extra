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
package org.cubeengine.module.chat.command;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.cubeengine.libcube.service.Broadcaster;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Greedy;
import org.cubeengine.libcube.service.command.annotation.Label;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.ChatFormat;
import org.cubeengine.module.chat.ChatPerm;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.SystemSubject;
import org.spongepowered.api.adventure.AdventureRegistry.OfType;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Singleton
public class ChatCommands
{
    private I18n i18n;
    private Broadcaster bc;
    private AfkCommand afkCmd;
    private ChatPerm perms;
    private UUID consoleUUID = UUID.nameUUIDFromBytes(":console".getBytes());

    private Map<UUID, UUID> lastWhispers = new HashMap<>();

    @Inject
    public ChatCommands(I18n i18n, Broadcaster broadcaster, AfkCommand afkCmd, ChatPerm perms)
    {
        this.i18n = i18n;
        this.bc = broadcaster;
        this.afkCmd = afkCmd;
        this.perms = perms;
    }

    @Command(desc = "Allows you to emote")
    public void me(CommandCause context, @Greedy String message)
    {
        bc.broadcastStatus(message, context);
    }

    @Command(desc = "Changes your display name")
    public void nick(CommandCause context, @Label("<name>|-reset") String name, @Option ServerPlayer player)
    {
        // TODO this only works when ChatFormat uses {DISPLAYNAME}
        final Audience ctxAudience = context.getAudience();
        if (player == null)
        {
            if (!(ctxAudience instanceof ServerPlayer))
            {
                i18n.send(ctxAudience, NEGATIVE, "You cannot change the consoles display name"); // console has no data / displayname
                return;
            }
            player = ((ServerPlayer)context.getAudience());
        }

        if (!ctxAudience.equals(player) && !context.hasPermission(perms.COMMAND_NICK_OTHER.getId()))
        {
            i18n.send(ctxAudience, NEGATIVE, "You are not allowed to change the nickname of another player!");
            return;
        }

        if ("-r".equalsIgnoreCase(name) || "-reset".equalsIgnoreCase(name))
        {
            player.remove(Keys.DISPLAY_NAME);
            i18n.send(ctxAudience, POSITIVE, "Display name reset to {user}", ctxAudience);
            return;
        }

        if (name.length() >= 3 && name.length() <= 16
                && Sponge.getServer().getUserManager().get(name).isPresent()
                && !context.hasPermission(perms.COMMAND_NICK_OFOTHER.getId()))
        {
            i18n.send(ctxAudience, NEGATIVE, "This name has been taken by another player!");
            return;
        }
        i18n.send(ctxAudience, POSITIVE, "Display name changed from {user} to {user}", ctxAudience, name);
        player.offer(Keys.DISPLAY_NAME, ChatFormat.fromLegacy(name, '&'));
    }

    @Command(desc = "Sends a private message to someone", alias = {"tell", "message", "pm", "m", "t", "whisper", "w"})
    public void msg(CommandCause context, Audience player, @Greedy String message)
    {
        if (!this.sendWhisperTo(player, message, context))
        {
            i18n.send(context.getAudience(), NEGATIVE, "Could not find the player {user} to send the message to. Is the player offline?", player);
        }
    }

    @Command(alias = "r", desc = "Replies to the last person that whispered to you.")
    public void reply(CommandCause context, @Greedy String message)
    {
        UUID lastWhisper;
        if (context.getAudience() instanceof ServerPlayer)
        {
            lastWhisper = lastWhispers.get(((ServerPlayer)context.getAudience()).getUniqueId());
        }
        else
        {
            lastWhisper = lastWhispers.get(consoleUUID);
        }
        if (lastWhisper == null)
        {
            i18n.send(context.getAudience(), NEUTRAL, "No one has sent you a message that you could reply to!");
            return;
        }
        Audience target;
        if (lastWhisper.equals(consoleUUID))
        {
            target = Sponge.getGame().getSystemSubject();
        }
        else
        {
            target = Sponge.getServer().getPlayer(lastWhisper).orElse(null);
        }
        if (!this.sendWhisperTo(target, message, context))
        {
            i18n.send(context.getAudience(), NEGATIVE, "Could not find the player to reply to. Is the player offline?");
        }
    }

    private boolean sendWhisperTo(Audience whisperTarget, String message, CommandCause context)
    {
        if (whisperTarget == null)
        {
            return false;
        }
        final Audience ctxAudience = context.getAudience();
        if (whisperTarget instanceof SystemSubject)
        {
            if (ctxAudience instanceof SystemSubject)
            {
                i18n.send(ctxAudience, NEUTRAL, "Talking to yourself?");
                return true;
            }
            if (ctxAudience instanceof Player)
            {
                i18n.send(whisperTarget, NEUTRAL, "{sender} -> {text:You}: {message:color=WHITE}", ctxAudience, message);
                i18n.send(ctxAudience, NEUTRAL, "{text:You} -> {user}: {message:color=WHITE}", whisperTarget, message);
                lastWhispers.put(consoleUUID, ((Player)ctxAudience).getUniqueId());
                lastWhispers.put(((Player)ctxAudience).getUniqueId(), consoleUUID);
                return true;
            }
            i18n.send(ctxAudience, Style.empty(), "Who are you!?");
            return true;
        }

        if (whisperTarget instanceof Player)
        {
            if (ctxAudience.equals(whisperTarget))
            {
                i18n.send(ctxAudience, NEUTRAL, "Talking to yourself?");
                return true;
            }
            i18n.send(whisperTarget, Style.empty(), "{sender} -> {text:You}: {message:color=WHITE}", ctxAudience, message);
            if (afkCmd.isAfk(((Player) whisperTarget)))
            {
                i18n.send(ctxAudience, NEUTRAL, "{user} is afk!", whisperTarget);
            }
            i18n.send(ctxAudience, NEUTRAL, "{text:You} -> {user}: {message:color=WHITE}", whisperTarget, message);
            if (ctxAudience instanceof Player)
            {
                lastWhispers.put(((Player)ctxAudience).getUniqueId(), ((Player)whisperTarget).getUniqueId());
                lastWhispers.put(((Player)whisperTarget).getUniqueId(), ((Player)ctxAudience).getUniqueId());
            }
            else
            {
                lastWhispers.put(consoleUUID, ((Player)whisperTarget).getUniqueId());
                lastWhispers.put(((Player)whisperTarget).getUniqueId(), consoleUUID);
            }
            return true;
        }
        return false;
    }

    @Command(desc = "Broadcasts a message")
    public void broadcast(CommandCause context, @Greedy String message)
    {
        this.bc.broadcastMessage(NEUTRAL, "[{text:Broadcast}] {input}", message);
    }

    @Command(desc = "Displays the colors")
    public void chatcolors(CommandCause context)
    {
        i18n.send(context.getAudience(), POSITIVE, "The following chat codes are available:");
        Builder builder = Component.text();
        int i = 0;
        final OfType<NamedTextColor> namedColors = Sponge.getGame().getRegistry().getAdventureRegistry().getNamedColors();
        for (String key : namedColors.keys())
        {
            final NamedTextColor namedTextColor = namedColors.getValue(key).get();

        }

        for (Entry<Character, NamedTextColor> color : ChatFormat.namedColors.entrySet())
        {
            builder.append(Component.space())
                   .append(Component.text(color.getValue().toString()).color(color.getValue()))
                   .append(Component.text(" (" + color.getKey() + ")"));
            if (i++ % 3 == 0)
            {
                builder.append(Component.newline());
            }
        }
        builder.append(Component.newline());
        for (Entry<Character, TextDecoration> decoration : ChatFormat.textDecorations.entrySet())
        {
            builder.append(Component.space())
                   .append(Component.text(decoration.getValue().toString()).decorate(decoration.getValue()))
                   .append(Component.text(" (" + decoration.getKey() + ")"));
            if (i++ % 3 == 0)
            {
                builder.append(Component.newline());
            }
        }
        context.sendMessage(Identity.nil(), builder.build());
    }


    @Command(alias = "roll", desc = "Shows a random number from 0 to 100")
    public void rand(CommandCause context, @Option String dice)
    {
        if (dice == null)
        {
            dice = "d100";
        }
        if (!dice.toLowerCase().startsWith("d"))
        {
            this.i18n.send(context.getAudience(), NEGATIVE, "Invalid dice. Try something like d20.");
            return;
        }
        dice = dice.substring(1);
        try
        {
            int diceNumber = Integer.parseInt(dice);
            if (diceNumber <= 500)
            {
                this.bc.broadcastTranslatedStatus(NEUTRAL, "rolled a {integer} ({name})!", context, new Random().nextInt(diceNumber) + 1, "D" + dice);
                return;
            }
        }
        catch (NumberFormatException ignored)
        {
        }
        this.i18n.send(context.getAudience(), NEGATIVE, "Invalid dice. Try something like d20.");
    }

}
