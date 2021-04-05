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

import java.util.Date;
import com.google.inject.Inject;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.TimeUtil;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import static java.util.concurrent.TimeUnit.DAYS;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Command(name = "vote", desc = "CubeEngine Vote Listener")
public class VoteCommands extends DispatcherCommand
{
    private final Vote module;
    private I18n i18n;

    @Inject
    public VoteCommands(Vote module, I18n i18n)
    {
        super();
        this.module = module;
        this.i18n = i18n;
    }

    @Command(desc = "Shows your current vote situation")
    public void info(ServerPlayer context)
    {
//        if (!(context instanceof Player))
//        {
//            i18n.send(context, NEUTRAL, "Well you wont get any rewards.");
//            if (!module.getConfig().voteUrl.isEmpty())
//            {
//                i18n.send(context, NEUTRAL, "But here go vote anyways: {name#voteurl}", module.getConfig().voteUrl);
//            }
//            return;
//        }
        final Long lastVote = context.get(VoteData.LAST_VOTE).orElse(null);
        final int count = context.get(VoteData.COUNT).orElse(0);
        final int streak = context.get(VoteData.STREAK).orElse(0);

        if (lastVote == null)
        {
            i18n.send(context, NEUTRAL, "Sorry but you do not have any registered votes on this server!");
            return;
        }
        i18n.send(context, POSITIVE, "You current vote-count is {amount} with a streak of {amount} votes", count, streak);
        final long voteDelta = System.currentTimeMillis() - lastVote;
        if (voteDelta > module.getConfig().streakTimeout.toMillis())
        {
            i18n.send(context, NEUTRAL, "Sadly you did not vote in the last {input#time} so your vote-count will be reset to 1",
                                   TimeUtil.format(context.locale(), module.getConfig().streakTimeout.toMillis()));
        }
        else if (voteDelta <= DAYS.toMillis(1))
        {
            i18n.send(context, NEUTRAL, "Voting now will increase your vote streak and result in higher rewards!");
        }
        else
        {
            i18n.send(context, POSITIVE, "You voted {input#time} so you will probably not be able to vote again already!",
                                   TimeUtil.format(context.locale(), new Date(lastVote)));
        }
        if (!module.getConfig().voteUrl.isEmpty())
        {
            i18n.send(context, POSITIVE, "You can vote here now: {name#voteurl}", module.getConfig().voteUrl);
        }
    }
}
