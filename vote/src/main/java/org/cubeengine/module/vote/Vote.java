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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.vexsoftware.votifier.sponge8.event.VotifierEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.apache.logging.log4j.Logger;
import org.cubeengine.libcube.service.Broadcaster;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.util.Pair;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.item.inventory.ItemStack;

import static java.util.Collections.singletonList;

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
    public void onEnable(RegisterDataEvent event) {
        VoteData.register(event);
    }

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

        final long lastVote = dh.get(VoteData.LAST_VOTE).orElse(0L);
        final long millisSinceLastVote = System.currentTimeMillis() - lastVote;
        if (millisSinceLastVote < config.voteCooldownTime.toMillis()) {
            return;
        }


        final int count = dh.get(VoteData.COUNT).orElse(0) + 1;
        dh.offer(VoteData.COUNT, count);
        dh.offer(VoteData.LAST_VOTE, System.currentTimeMillis());

        final int streak;
        final boolean isStreakVote = millisSinceLastVote < config.streakTimeout.toMillis();
        if (isStreakVote) {
            streak = dh.get(VoteData.STREAK).orElse(0) + 1;
        } else {
            streak = 1;
        }
        dh.offer(VoteData.STREAK, streak);

        final int countToStreakReward = (this.config.streak - streak) % this.config.streak;

        final ItemStack reward;
        if (isStreakVote && streak % this.config.streak == 0)
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

    private static Component deepAppend(Component target, Component component) {
        final List<Component> children = target.children();
        if (children.isEmpty()) {
            return target.children(singletonList(component));
        }

        List<Component> newChildren = new ArrayList<>(children);
        Component newLastChild = deepAppend(newChildren.get(children.size() - 1), component);
        newChildren.set(children.size() - 1, newLastChild);
        return target.children(newChildren);
    }

    private static Component messageTemplateToComponent(String template, Map<String, Component> replacements) {

        final Matcher matcher = TEMPLATE_TOKENS.matcher(template);
        List<Pair<Component, Boolean>> out = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String token = matcher.group(0);
            if (token.startsWith("{")) {
                String varName = token.substring(1, token.length() - 1);
                Component replacement = replacements.get(varName);
                if (replacement != null) {
                    out.add(new Pair<>(legacyMessageToComponent(buffer.toString()), true));
                    buffer.setLength(0);
                    out.add(new Pair<>(replacement, false));
                    continue;
                }
            }
            buffer.append(token);
        }

        if (buffer.length() != 0) {
            out.add(new Pair<>(legacyMessageToComponent(buffer.toString()), true));
        }

        Collections.reverse(out);
        return out.stream().reduce((next, previous) -> {
            if (previous.getRight()) {
                return new Pair<>(deepAppend(previous.getLeft(), next.getLeft()), false);
            } else {
                return new Pair<>(Component.text().append(previous.getLeft()).append(next.getLeft()).build(), false);
            }
        }).map(Pair::getLeft).orElse(Component.empty());
    }

    public static Component voteMessage(String raw, String username, int count, String voteUrl, int toStreak, ItemStack reward)
    {
        Map<String, Component> replacements = new HashMap<>();
        replacements.put("PLAYER", Component.text(username));
        replacements.put("COUNT", Component.text(String.valueOf(count)));
        // TODO replacements.put("STREAK", Component.text(String.valueOf(count)));
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
