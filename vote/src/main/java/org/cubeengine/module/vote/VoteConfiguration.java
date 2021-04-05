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

import java.time.Duration;
import org.cubeengine.reflect.annotations.Comment;
import org.cubeengine.reflect.codec.yaml.ReflectedYaml;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;

@SuppressWarnings("all")
public class VoteConfiguration extends ReflectedYaml
{

    @Comment({"{PLAYER} will be replaced with the player-name",
             "{AMOUNT} will be replaced with the amount of times that player voted",
             "{VOTEURL} will be replaced with the configured vote-url",
             "{TOSTREAK} will be replaced with amount of votes needed for a streak reward",
             "{REWARD} will be replaced with the reward item",
    })
    public String voteBroadcast = "&6{PLAYER} voted!";

    public String singleVoteMessage = "&aYou received a {SINGLE_VOTE_REWARD} for voting!";

    public String streakMessage = "&aYou are on a streak! Take this {STREAK_REWARD} for voting {AMOUNT} times in a row!";

    @Comment("Players will receive a bonus if they vote multiple times in given time-frame")
    public Duration voteMaxBonusTime = Duration.ofHours(36);

    @Comment("Players will receive a bonus if they vote multiple times in given time-frame")
    public Duration voteMinBonusTime = Duration.ofHours(12);

    public ItemType singleVoteReward = ItemTypes.COOKIE.get();
    public String singleVoteRewardName = "&6Vote Reward";

    public int streak = 7;
    public ItemType streakVoteReward = ItemTypes.NETHERITE_SCRAP.get();
    public String streakVoteRewardName = "&6Streak Reward";

    public String voteUrl = "";
}
