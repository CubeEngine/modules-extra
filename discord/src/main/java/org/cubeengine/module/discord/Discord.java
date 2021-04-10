package org.cubeengine.module.discord;

import com.google.inject.Singleton;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Webhook;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.shard.MemberRequestFilter;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import org.cubeengine.processor.Module;
import org.reactivestreams.Publisher;
import org.spongepowered.api.Server;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Singleton
@Module
public class Discord {
    @Listener
    public void onServerStart(StartedEngineEvent<Server> event) {

    }

    @Listener
    public void onServerStop(StoppingEngineEvent<Server> event) {

    }

    public static void main(String[] args) {
        final DiscordClient client = DiscordClient.create(args[0]);
        client.gateway()
                .setEnabledIntents(IntentSet.of(Intent.GUILDS,
                        Intent.GUILD_MEMBERS,
                        Intent.GUILD_VOICE_STATES,
                        Intent.GUILD_MESSAGES,
                        Intent.GUILD_MESSAGE_REACTIONS,
                        Intent.DIRECT_MESSAGES))
                .setInitialPresence(info -> ClientPresence.online(ClientActivity.watching("You")))
                .setMemberRequestFilter(MemberRequestFilter.none())
                .withGateway(gateway -> {
                    final Flux<?> messageProcessing = gateway.getGuilds()
                            .flatMap(Guild::getChannels)
                            .flatMap(Discord::getTextChannel)
                            .filter(c -> c.getName().equals("minecraft"))
                            .flatMap(channel -> {
                                return channel.getWebhooks()
                                        .filter(w -> "THE WEBHOOK!".equals(w.getName().orElse(null)))
                                        .collectList()
                                        .flatMap(webhooks -> {
                                            if (webhooks.isEmpty()) {
                                                return channel.createWebhook(w -> w.setName("THE WEBHOOK!").setReason("Because I want it!"));
                                            } else {
                                                return Mono.just(webhooks.get(0));
                                            }
                                        });
                            })
                            .flatMap(webhook -> {
                                return gateway.getUsers()
                                        .flatMap(user -> gateway.on(MessageCreateEvent.class).filter(m -> m.getMessage().getAuthor().map(u -> !u.getId().equals(user.getId())).orElse(true)))
                                        .filter(m -> m.getMessage().getAuthor().map(u -> !u.isBot()).orElse(true))
                                        .flatMap(m -> getTextChannel(m.getMessage()).filter(c -> c.getName().equals("minecraft")).map(c -> m))
                                        .flatMap(m -> Discord.handleEvent(m, webhook));
                            });

                    return Mono.when(messageProcessing, gateway.onDisconnect());
                }).block();
    }

    public static Mono<TextChannel> getTextChannel(Message message) {
        return message.getChannel().flatMap(Discord::getTextChannel);
    }

    public static Mono<TextChannel> getTextChannel(Channel channel) {
        if (channel instanceof TextChannel) {
            return Mono.just((TextChannel) channel);
        } else {
            return Mono.empty();
        }
    }

    public static Publisher<?> handleEvent(MessageCreateEvent event, Webhook webhook) {
        return Mono.justOrEmpty(event.getMessage().getAuthor())
                .flatMap(author ->
                    webhook.executeAndWait(m -> m.setUsername(author.getUsername()).setAvatarUrl(author.getAvatarUrl()).setContent(event.getMessage().getContent()))
                );
//        return Mono.just(event.getMessage())
//                .flatMap(Discord::getTextChannel)
//                .flatMap(c -> c.createMessage(spec -> spec.setMessageReference(event.getMessage().getId()).setEmbed(e -> e.setAuthor("test", "https://schich.tel", null)).setContent("Echo: " + event.getMessage().getContent())));

    }
}
