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
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Webhook;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.shard.MemberRequestFilter;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.util.AllowedMentions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.cubeengine.libcube.InjectService;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.ComponentUtil;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Game;
import org.spongepowered.api.Server;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.event.message.PlayerChatEvent;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.plugin.PluginContainer;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static discord4j.rest.util.AllowedMentions.Type.USER;
import static net.kyori.adventure.text.event.ClickEvent.openUrl;
import static org.cubeengine.libcube.util.ComponentUtil.autoLink;
import static org.cubeengine.libcube.util.ComponentUtil.stripLegacy;

@Singleton
@Module
public class Discord {
    public static final String DEFAULT_CHAT_FORMAT = "{NAME}: {MESSAGE}";
    private final AtomicReference<GatewayDiscordClient> client = new AtomicReference<>(null);
    private final AtomicReference<Webhook> webhook = new AtomicReference<>(null);
    private final AtomicReference<Server> server = new AtomicReference<>(null);

    private static final Pattern EMOJI_FROM_DISCORD = Pattern.compile("<(:[^: ]+:)\\d+>");
    private static final Pattern EMOJI_FROM_MINECRAFT = Pattern.compile(":([^: ]+):");

    @InjectService
    private PermissionService ps;

    @ModuleConfig
    private DiscordConfig config;

    @ModuleCommand
    private DiscordCommands commands;

    @Inject
    private PluginContainer pluginContainer;

    @Inject
    private Game game;

    @Inject
    private I18n i18n;

    @Listener
    public void onServerStart(StartedEngineEvent<Server> event) {
        server.set(event.engine());
        Mono.justOrEmpty(config.botToken)
                .flatMap(token ->DiscordClient.create(token)
                        .gateway()
                        .setEnabledIntents(IntentSet.of(Intent.GUILDS,
                                Intent.GUILD_MEMBERS,
                                Intent.GUILD_VOICE_STATES,
                                Intent.GUILD_MESSAGES,
                                Intent.GUILD_MESSAGE_REACTIONS,
                                Intent.DIRECT_MESSAGES))
                        .setInitialPresence(info -> ClientPresence.online(ClientActivity.watching("You")))
                        .setMemberRequestFilter(MemberRequestFilter.none())
                        .login()
                )
                .subscribe(this::clientConnected);
    }

    public DiscordConfig getConfig() {
        return config;
    }

    private static String toPlainString(Component component) {
        return SpongeComponents.plainSerializer().serialize(component);
    }

    private void clientConnected(GatewayDiscordClient client) {
        this.client.set(client);

        Mono.justOrEmpty(config.webhook)
                .flatMap(webhook -> Mono.justOrEmpty(webhook.id).flatMap(id -> Mono.justOrEmpty(webhook.token).flatMap(token ->
                    client.getWebhookByIdWithToken(Snowflake.of(id), token)
                ))).subscribe(this.webhook::set);

        client.getUsers()
                .flatMap(user -> client.on(MessageCreateEvent.class).filter(m -> m.getMessage().getAuthor().map(u -> !u.getId().equals(user.getId())).orElse(true)))
                .filter(m -> m.getMessage().getAuthor().map(u -> !u.isBot()).orElse(true))
                .flatMap(m -> m.getMessage().getChannel().ofType(TextChannel.class).filter(c -> c.getName().equals(config.channel)).map(c -> m))
                .subscribe(this::onDiscordChat);
    }

    @Listener
    public void onRegisterData(RegisterDataEvent event) {
        DiscordData.register(event);
    }

    @Listener
    public void onServerStop(StoppingEngineEvent<Server> event) {
        server.set(null);
        GatewayDiscordClient client = this.client.getAndSet(null);
        if (client != null) {
            client.logout();
        }
    }

    @Listener(order = Order.LAST)
    public void onMinecraftChat(PlayerChatEvent event, @Root ServerPlayer player) {
        if (player.get(DiscordData.MUTED).orElse(false)) {
            return;
        }

        Task task = Task.builder()
                .plugin(this.pluginContainer)
                .execute(() -> {
                    final Webhook w = webhook.get();
                    w.getGuild().flatMapMany(Guild::getEmojis).collectList().flatMap(emojis -> {
                        final Map<String, String> emojiLookup = emojis.stream().collect(Collectors.toMap(GuildEmoji::getName, e -> e.getId().asString()));

                        // stripLegacy() should be done by a chat plugin, not by us: https://github.com/SpongePowered/SpongeAPI/issues/2259
                        String content = StringUtils.replaceWithCallback(EMOJI_FROM_MINECRAFT, stripLegacy(toPlainString(event.originalMessage())), match -> {
                            final String emojiId = emojiLookup.get(match.group(1));
                            if (emojiId != null) {
                                return "<" + match.group() + emojiId + ">";
                            }
                            return match.group();
                        });

                        return w.executeAndWait(spec -> spec
                                .setContent(content)
                                .setUsername(toPlainString(player.displayName().get()))
                                .setAvatarUrl("https://crafatar.com/avatars/" + player.uniqueId().toString() + "?overlay")
                                .setAllowedMentions(AllowedMentions.builder().parseType(USER).build())
                        );
                    }).subscribe();
                })
                .build();

        game.asyncScheduler().submit(task);
    }

    private void onDiscordChat(MessageCreateEvent event) {
        final Message message = event.getMessage();
        message.getAuthor().ifPresent(author -> {
            Mono.justOrEmpty(event.getGuildId()).flatMap(author::asMember).subscribe(member -> {
                member.getColor().subscribe(color -> {
                    final Server server = this.server.get();
                    if (server != null) {
                        final Component userName = Component.text(member.getDisplayName(), TextColor.color(color.getRed(), color.getGreen(), color.getBlue()));
                        final Component attachmentStrings = message.getAttachments().stream().reduce((Component) Component.empty(), (component, attachment) ->
                                Component.empty()
                                        .append(Component.text("[")
                                                .append(Component.text(attachment.getFilename(), NamedTextColor.DARK_RED))
                                                .append(Component.text("]"))
                                                .clickEvent(openUrl(attachment.getUrl()))
                                                .hoverEvent(Component.text("Open Attachment").asHoverEvent()))
                                        .append(Component.space()),
                                Component::append);
                        final String format = Optional.ofNullable(config.defaultChatFormat).orElse(DEFAULT_CHAT_FORMAT);

                        final Task task = Task.builder()
                                .execute(() -> broadcastMessage(server, format, userName, EMOJI_FROM_DISCORD.matcher(message.getContent()).replaceAll("$1"), attachmentStrings))
                                .plugin(pluginContainer)
                                .build();
                        server.scheduler().submit(task);
                    }
                });
            });
        });
    }

    private void broadcastMessage(Server server, String template, Component name, String message, Component attachments) {
        for (ServerPlayer onlinePlayer : server.onlinePlayers()) {
            if (!onlinePlayer.get(DiscordData.MUTED).orElse(false)) {
                Component content = attachments.append(autoLink(message, i18n.translate(onlinePlayer, "Open Link")));

                Map<String, Component> replacements = new HashMap<>();
                replacements.put("NAME", name);
                replacements.put("MESSAGE", autoLink(message, content));

                onlinePlayer.sendMessage(ComponentUtil.legacyMessageTemplateToComponent(template, replacements));
            }
        }
    }
}
