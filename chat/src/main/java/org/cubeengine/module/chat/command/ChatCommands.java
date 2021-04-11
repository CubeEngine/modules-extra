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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
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
import org.cubeengine.libcube.util.Triplet;
import org.cubeengine.module.chat.ChatPerm;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.SystemSubject;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import static java.util.stream.Collectors.toList;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.cubeengine.libcube.util.ComponentUtil.fromLegacy;

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
        final Audience ctxAudience = context.audience();
        if (player == null)
        {
            if (!(ctxAudience instanceof ServerPlayer))
            {
                i18n.send(ctxAudience, NEGATIVE, "You cannot change the consoles display name"); // console has no data / displayname
                return;
            }
            player = ((ServerPlayer)context.audience());
        }

        if (!ctxAudience.equals(player) && !context.hasPermission(perms.COMMAND_NICK_OTHER.getId()))
        {
            i18n.send(ctxAudience, NEGATIVE, "You are not allowed to change the nickname of another player!");
            return;
        }

        if ("-r".equalsIgnoreCase(name) || "-reset".equalsIgnoreCase(name))
        {
            player.remove(Keys.CUSTOM_NAME);
            i18n.send(ctxAudience, POSITIVE, "Display name reset to {user}", ctxAudience);
            return;
        }

        if (name.length() >= 3 && name.length() <= 16
                && Sponge.server().userManager().find(name).isPresent()
                && !context.hasPermission(perms.COMMAND_NICK_OFOTHER.getId()))
        {
            i18n.send(ctxAudience, NEGATIVE, "This name has been taken by another player!");
            return;
        }
        i18n.send(ctxAudience, POSITIVE, "Display name changed from {user} to {user}", ctxAudience, name);
        player.offer(Keys.CUSTOM_NAME, fromLegacy(name));
    }

    @Command(desc = "Sends a private message to someone", alias = {"tell", "message", "pm", "m", "t", "whisper", "w"})
    public void msg(CommandCause context, Audience player, @Greedy String message)
    {
        if (!this.sendWhisperTo(player, message, context))
        {
            i18n.send(context.audience(), NEGATIVE, "Could not find the player {user} to send the message to. Is the player offline?", player);
        }
    }

    @Command(alias = "r", desc = "Replies to the last person that whispered to you.")
    public void reply(CommandCause context, @Greedy String message)
    {
        UUID lastWhisper;
        if (context.audience() instanceof ServerPlayer)
        {
            lastWhisper = lastWhispers.get(((ServerPlayer)context.audience()).uniqueId());
        }
        else
        {
            lastWhisper = lastWhispers.get(consoleUUID);
        }
        if (lastWhisper == null)
        {
            i18n.send(context.audience(), NEUTRAL, "No one has sent you a message that you could reply to!");
            return;
        }
        Audience target;
        if (lastWhisper.equals(consoleUUID))
        {
            target = Sponge.game().systemSubject();
        }
        else
        {
            target = Sponge.server().player(lastWhisper).orElse(null);
        }
        if (!this.sendWhisperTo(target, message, context))
        {
            i18n.send(context.audience(), NEGATIVE, "Could not find the player to reply to. Is the player offline?");
        }
    }

    private boolean sendWhisperTo(Audience whisperTarget, String message, CommandCause context)
    {
        if (whisperTarget == null)
        {
            return false;
        }
        final Audience ctxAudience = context.audience();
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
                lastWhispers.put(consoleUUID, ((Player)ctxAudience).uniqueId());
                lastWhispers.put(((Player)ctxAudience).uniqueId(), consoleUUID);
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
                lastWhispers.put(((Player)ctxAudience).uniqueId(), ((Player)whisperTarget).uniqueId());
                lastWhispers.put(((Player)whisperTarget).uniqueId(), ((Player)ctxAudience).uniqueId());
            }
            else
            {
                lastWhispers.put(consoleUUID, ((Player)whisperTarget).uniqueId());
                lastWhispers.put(((Player)whisperTarget).uniqueId(), consoleUUID);
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
        i18n.send(context.audience(), POSITIVE, "The following chat codes are available:");
        Builder builder = Component.text();

        List<Triplet<NamedTextColor, Component, String>> namedColors = Arrays.asList(
                new Triplet<>(NamedTextColor.BLACK, i18n.translate(context, "Black"), "0"),
                new Triplet<>(NamedTextColor.DARK_BLUE, i18n.translate(context, "Dark Blue"), "1"),
                new Triplet<>(NamedTextColor.DARK_GREEN, i18n.translate(context, "Dark Green"), "2"),
                new Triplet<>(NamedTextColor.DARK_AQUA, i18n.translate(context, "Dark Aqua"), "3"),
                new Triplet<>(NamedTextColor.DARK_RED, i18n.translate(context, "Dark Red"), "4"),
                new Triplet<>(NamedTextColor.DARK_PURPLE, i18n.translate(context, "Dark Purple"), "5"),
                new Triplet<>(NamedTextColor.GOLD, i18n.translate(context, "Gold"), "6"),
                new Triplet<>(NamedTextColor.GRAY, i18n.translate(context, "Gray"), "7"),
                new Triplet<>(NamedTextColor.DARK_GRAY, i18n.translate(context, "Dark Gray"), "8"),
                new Triplet<>(NamedTextColor.BLUE, i18n.translate(context, "Blue"), "9"),
                new Triplet<>(NamedTextColor.GREEN, i18n.translate(context, "Green"), "a"),
                new Triplet<>(NamedTextColor.AQUA, i18n.translate(context, "Aqua"), "b"),
                new Triplet<>(NamedTextColor.RED, i18n.translate(context, "Red"), "c"),
                new Triplet<>(NamedTextColor.LIGHT_PURPLE, i18n.translate(context, "Light Purple"), "d"),
                new Triplet<>(NamedTextColor.YELLOW, i18n.translate(context, "Yellow"), "e"),
                new Triplet<>(NamedTextColor.WHITE, i18n.translate(context, "White"), "f")
        );

        final int entriesPerLine = 3;
        renderLines(namedColors, entriesPerLine, builder, color -> presentFormat(color.getSecond(), c -> c.color(color.getFirst()), color.getThird()));

        List<Triplet<TextDecoration, Component, String>> namedDecorations = Arrays.asList(
                new Triplet<>(TextDecoration.OBFUSCATED, i18n.translate(context, "Obfuscated"), "k"),
                new Triplet<>(TextDecoration.BOLD, i18n.translate(context, "Bold"), "l"),
                new Triplet<>(TextDecoration.STRIKETHROUGH, i18n.translate(context, "Strikethrough"), "m"),
                new Triplet<>(TextDecoration.UNDERLINED, i18n.translate(context, "Underlined"), "n"),
                new Triplet<>(TextDecoration.ITALIC, i18n.translate(context, "Italic"), "o")
        );

        builder.append(Component.newline());
        renderLines(namedDecorations, entriesPerLine, builder, decoration -> presentFormat(decoration.getSecond(), c -> c.decorate(decoration.getFirst()), decoration.getThird()));

        context.sendMessage(Identity.nil(), builder.build());
    }

    private static <T> void renderLines(List<T> entries, int entriesPerLine, Builder builder, Function<T, Component> render) {
        final UnmodifiableIterator<List<T>> lines = Iterators.partition(entries.iterator(), entriesPerLine);
        while (lines.hasNext()) {
            final List<Component> components = lines.next().stream()
                    .map(render)
                    .collect(toList());
            builder.append(Component.join(Component.text("   "), components));
            builder.append(Component.newline());
        }
    }

    private static Component presentFormat(Component label, Function<Component, Component> present, String legacySymbol) {
        Component example = present.apply(Component.text("■"));
        return Component.empty()
                .append(example)
                .append(Component.space())
                .append(label)
                .append(Component.text(" ("))
                .append(Component.text("&" + legacySymbol, NamedTextColor.GRAY))
                .append(Component.text(") "))
                .append(example);

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
            this.i18n.send(context.audience(), NEGATIVE, "Invalid dice. Try something like d20.");
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
        this.i18n.send(context.audience(), NEGATIVE, "Invalid dice. Try something like d20.");
    }

}
