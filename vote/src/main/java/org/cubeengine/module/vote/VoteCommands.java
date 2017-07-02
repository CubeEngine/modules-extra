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
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.TimeUtil;
import org.cubeengine.libcube.service.database.Database;
import org.cubeengine.module.vote.storage.VoteModel;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.module.vote.storage.TableVote.TABLE_VOTE;
import static java.util.concurrent.TimeUnit.DAYS;

import javax.inject.Inject;

public class VoteCommands
{
    private final PeriodFormatter formatter;
    private final Vote module;
    private Database db;
    private I18n i18n;

    @Inject
    public VoteCommands(Vote module, Database db, I18n i18n)
    {
        this.module = module;
        this.db = db;
        this.i18n = i18n;
        this.formatter = new PeriodFormatterBuilder()
            .appendWeeks().appendSuffix(" week"," weeks").appendSeparator(" ")
            .appendDays().appendSuffix(" day", " days").appendSeparator(" ")
            .appendHours().appendSuffix(" hour"," hours").appendSeparator(" ")
            .appendMinutes().appendSuffix(" minute", " minutes").appendSeparator(" ")
            .appendSeconds().appendSuffix(" second", " seconds").appendSeparator(" ")
            .appendMillis().appendSuffix(" ms").toFormatter();
    }

    @Command(desc = "Shows your current vote situation")
    public void vote(CommandSource context)
    {
        if (!(context instanceof Player))
        {
            i18n.send(context, NEUTRAL, "Well you wont get any rewards.");
            if (!module.getConfig().voteUrl.isEmpty())
            {
                i18n.send(context, NEUTRAL, "But here go vote anyways: {name#voteurl}", module.getConfig().voteUrl);
            }
            return;
        }
        VoteModel voteModel = db.getDSL().selectFrom(TABLE_VOTE).where(TABLE_VOTE.ID.eq(((Player)context).getUniqueId())).fetchOne();
        if (voteModel == null)
        {
            i18n.send(context, NEUTRAL, "Sorry but you do not have any registered votes on this server!");
            return;
        }
        i18n.send(context, POSITIVE, "You current vote-count is {amount}", voteModel.getVotes());
        if (voteModel.timePassed(module.getConfig().voteBonusTime.getMillis()))
        {
            i18n.send(context, NEUTRAL, "Sadly you did not vote in the last {input#time} so your vote-count will be reset to 1",
                                   this.formatter.print(module.getConfig().voteBonusTime.toPeriod()));
        }
        else if (voteModel.timePassed(DAYS.toMillis(1)))
        {
            i18n.send(context, NEUTRAL, "Voting now will increase your consecutive votes and result in higher reward!");
        }
        else
        {
            i18n.send(context, POSITIVE, "You voted {input#time} so you will probably not be able to vote again already!",
                                   TimeUtil.format(context.getLocale(), new Date(voteModel.getLastVote())));
        }
        if (!module.getConfig().voteUrl.isEmpty())
        {
            i18n.send(context, POSITIVE, "You can vote here now: {name#voteurl}", module.getConfig().voteUrl);
        }
    }
}
