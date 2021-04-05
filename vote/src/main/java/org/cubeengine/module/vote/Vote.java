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
package org.cubeengine.module.vote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.vexsoftware.votifier.sponge8.event.VotifierEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.apache.logging.log4j.Logger;
import org.cubeengine.libcube.service.Broadcaster;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.item.inventory.ItemStack;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * A module to handle Votes coming from a {@link VotifierEvent}
 */
@Singleton
@Module(dependencies = @Dependency("nuvotifier"))
public class Vote
{
    @Inject private Broadcaster bc;
    @Inject private Logger logger;

    @ModuleConfig private VoteConfiguration config;
    @ModuleCommand private VoteCommands commands;

    @Listener
    public void onVote(VotifierEvent event)
    {
        final com.vexsoftware.votifier.model.Vote vote = event.getVote();
        final String username = event.getVote().getUsername();
        final Optional<ServerPlayer> player = Sponge.server().player(username);
        final User user = Sponge.server().userManager().find(username).orElse(null);
        final DataHolder.Mutable dh = player.map(DataHolder.Mutable.class::cast).orElse(user);
        if (dh == null)
        {
            logger.info("{} voted but is not known to the server!", username);
            return;
        }

        final int count = dh.get(VoteData.COUNT).orElse(0) + 1;
        final long lastVote = dh.get(VoteData.LAST_VOTE).orElse(System.currentTimeMillis());

        boolean isStreak = System.currentTimeMillis() - lastVote > config.voteMaxBonusTime.toMillis() && System.currentTimeMillis() - lastVote < config.voteMinBonusTime.toMillis();
        final int streak = isStreak ? dh.get(VoteData.STREAK).orElse(0) + 1 : 0;

        dh.offer(VoteData.COUNT, count);
        dh.offer(VoteData.STREAK, streak);
        dh.offer(VoteData.LAST_VOTE, System.currentTimeMillis());

        final int countToStreakReward = this.config.streak - streak % this.config.streak;



        final ItemStack reward;
        if (isStreak && streak % this.config.streak == 0)
        {
            reward = ItemStack.of(this.config.streakVoteReward);
            renameItemStack(reward, this.config.streakVoteRewardName);
        }
        else
        {
            reward = ItemStack.of(this.config.singleVoteReward);
            renameItemStack(reward, this.config.singleVoteRewardName);
        }

        Sponge.server().sendMessage(voteMessage(this.config.voteBroadcast, username, count, this.config.voteUrl, countToStreakReward, reward));
        player.ifPresent(p -> {
            p.sendMessage(voteMessage(this.config.singleVoteMessage, username, count, this.config.voteUrl, countToStreakReward, reward));
        });

        if (player.isPresent())
        {
            player.get().inventory().offer(reward);
        }
        else
        {
            user.inventory().offer(reward);
        }
    }

    public static void renameItemStack(ItemStack stack, String name) {
        if (name != null) {
            stack.offer(Keys.CUSTOM_NAME, SpongeComponents.legacyAmpersandSerializer().deserialize(name));
        }
    }

    private static final Pattern TEMPLATE_TOKENS = Pattern.compile("(\\{[^}]+}|[^{]+)");

    private static Component legacyMessageToComponent(String message) {
        return SpongeComponents.legacyAmpersandSerializer().deserialize(message);
    }

    private static Stream<Component> flatten(Component component) {
        return Stream.concat(Stream.of(component.children(emptyList())), component.children().stream().flatMap(Vote::flatten));
    }

    private static Component messageTemplateToComponent(String template, Map<String, Component> replacements) {
        final Matcher matcher = TEMPLATE_TOKENS.matcher(template);
        List<Component> out = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String token = matcher.group(0);
            if (token.startsWith("{")) {
                String varName = token.substring(1, token.length() - 1);
                Component replacement = replacements.get(varName);
                if (replacement != null) {
                    out.addAll(flatten(legacyMessageToComponent(buffer.toString())).collect(toList()));
                    buffer.setLength(0);
                    out.add(replacement);
                    continue;
                }
            }
            buffer.append(token);
        }

        if (buffer.length() != 0) {
            out.addAll(flatten(legacyMessageToComponent(buffer.toString())).collect(toList()));
        }

        Collections.reverse(out);
        return out.stream().reduce(Component.empty(), (a, b) -> b.children(Collections.singletonList(a)));
    }

    public static Component voteMessage(String raw, String username, int count, String voteUrl, int toStreak, ItemStack reward)
    {
        Map<String, Component> replacements = new HashMap<>();
        replacements.put("PLAYER", Component.text(username));
        replacements.put("COUNT", Component.text(String.valueOf(count)));
        replacements.put("VOTEURL", Component.text(voteUrl).clickEvent(ClickEvent.openUrl(voteUrl)));
        replacements.put("TOSTREAK", Component.text(String.valueOf(toStreak)));
        replacements.put("REWARD", reward.get(Keys.DISPLAY_NAME).orElseThrow(() -> new IllegalArgumentException("ItemStack should always have a display name!")));

        return messageTemplateToComponent(raw, replacements);
    }

    public VoteConfiguration getConfig()
    {
        return config;
    }
}
