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

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.vexsoftware.votifier.sponge8.event.VotifierEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    public void onVote(VotifierEvent event) throws ExecutionException, InterruptedException
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

        Sponge.server().sendMessage(voteMessage(this.config.voteBroadcast, username, count, this.config.voteUrl, countToStreakReward));

        player.ifPresent(p -> {
            p.sendMessage(voteMessage(this.config.voteMessage, username, count, this.config.voteUrl, countToStreakReward));
        });

        if (isStreak && streak % this.config.streak == 0)
        {
            final ItemStack streakReward = ItemStack.of(this.config.streakReward);
            if (player.isPresent())
            {
                player.get().inventory().offer(streakReward);
            }
            else
            {
                user.inventory().offer(streakReward);
            }
        }
        else
        {
            final ItemStack onlineReward = ItemStack.of(this.config.onlineReward);
            onlineReward.offer(Keys.CUSTOM_NAME, Component.text("Vote-Cookie", NamedTextColor.GOLD));
            player.ifPresent(serverPlayer -> serverPlayer.inventory().offer(onlineReward));
        }
    }

    public static Component voteMessage(String raw, String username, int count, String voteurl, int toStreak)
    {
        final String replaced = raw.replace("{PLAYER}", username)
                                   .replace("{COUNT}", String.valueOf(count))
                                   .replace("{VOTEURL}", voteurl)
                                   .replace("{TOSTREAK}", String.valueOf(toStreak))
                ;
        return SpongeComponents.legacyAmpersandSerializer().deserialize(replaced);
    }

    public VoteConfiguration getConfig()
    {
        return config;
    }
}
