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
package org.cubeengine.module.vote;

import java.util.Date;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.libcube.util.TimeUtil;
import org.cubeengine.libcube.service.command.CommandContext;
import org.cubeengine.libcube.service.database.Database;
import org.cubeengine.libcube.service.user.User;
import org.cubeengine.module.vote.storage.VoteModel;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.module.vote.storage.TableVote.TABLE_VOTE;
import static java.util.concurrent.TimeUnit.DAYS;

public class VoteCommands
{
    private final PeriodFormatter formatter;
    private final Vote module;
    private Database db;

    public VoteCommands(Vote module, Database db)
    {
        this.module = module;
        this.db = db;
        this.formatter = new PeriodFormatterBuilder()
            .appendWeeks().appendSuffix(" week"," weeks").appendSeparator(" ")
            .appendDays().appendSuffix(" day", " days").appendSeparator(" ")
            .appendHours().appendSuffix(" hour"," hours").appendSeparator(" ")
            .appendMinutes().appendSuffix(" minute", " minutes").appendSeparator(" ")
            .appendSeconds().appendSuffix(" second", " seconds").appendSeparator(" ")
            .appendMillis().appendSuffix(" ms").toFormatter();
    }

    @Command(desc = "Shows your current vote situation")
    public void vote(CommandContext context)
    {
        if (!(context.getSource() instanceof User))
        {
            context.sendTranslated(NEUTRAL, "Well you wont get any rewards.");
            if (!module.getConfig().voteUrl.isEmpty())
            {
                context.sendTranslated(NEUTRAL, "But here go vote anyways: {name#voteurl}", module.getConfig().voteUrl);
            }
            return;
        }
        VoteModel voteModel = db.getDSL().selectFrom(TABLE_VOTE).where(TABLE_VOTE.USERID.eq(((User)context.getSource()).getEntity().getId())).fetchOne();
        if (voteModel == null)
        {
            context.sendTranslated(NEUTRAL, "Sorry but you do not have any registered votes on this server!");
            return;
        }
        context.sendTranslated(POSITIVE, "You current vote-count is {amount}", voteModel.getVotes());
        if (voteModel.timePassed(module.getConfig().voteBonusTime.getMillis()))
        {
            context.sendTranslated(NEUTRAL, "Sadly you did not vote in the last {input#time} so your vote-count will be reset to 1",
                                   this.formatter.print(module.getConfig().voteBonusTime.toPeriod()));
        }
        else if (voteModel.timePassed(DAYS.toMillis(1)))
        {
            context.sendTranslated(NEUTRAL, "Voting now will increase your consecutive votes and result in higher reward!");
        }
        else
        {
            context.sendTranslated(POSITIVE, "You voted {input#time} so you will probably not be able to vote again already!",
                                   TimeUtil.format(context.getSource().getLocale(), new Date(voteModel.getLastVote())));
        }
        if (!module.getConfig().voteUrl.isEmpty())
        {
            context.sendTranslated(POSITIVE, "You can vote here now: {name#voteurl}", module.getConfig().voteUrl);
        }
    }
}
