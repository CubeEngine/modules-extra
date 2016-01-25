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
package org.cubeisland.module.vote;

import javax.inject.Inject;
import com.vexsoftware.votifier.model.VotifierEvent;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import org.cubeengine.service.filesystem.FileManager;
import org.cubeengine.module.core.sponge.EventManager;
import org.cubeengine.module.core.util.ChatFormat;
import org.cubeengine.service.Economy;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.user.User;
import org.cubeengine.service.user.UserManager;
import org.cubeisland.module.vote.storage.TableVote;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jooq.DSLContext;

import static org.cubeengine.service.i18n.formatter.MessageType.NONE;
import static org.cubeisland.module.vote.storage.TableVote.TABLE_VOTE;
import static java.lang.Math.pow;

/**
 * A module to handle Votes coming from a {@link VotifierEvent}
 */
@ModuleInfo(name = "Vote", description = "Get rewards for votes")
public class Vote extends Module implements Listener
{
    private VoteConfiguration config;

    @Inject private Economy economy;
    @Inject private Database db;
    @Inject private EventManager em;
    @Inject private CommandManager cm;
    @Inject private FileManager fm;
    @Inject private UserManager um;
    @Inject private Log logger;

    @Enable
    public void onEnable()
    {
        db.registerTable(TableVote.class);
        this.config = fm.loadConfig(this, VoteConfiguration.class);
        em.registerListener(this, this);
        cm.addCommands(this, new VoteCommands(this, db));
    }

    @EventHandler
    private void onVote(VotifierEvent event)
    {
        final com.vexsoftware.votifier.model.Vote vote = event.getVote();
        final User user = um.findExactUser(vote.getUsername());
        if (user == null)
        {
            if (vote.getUsername() == null || vote.getUsername().trim().isEmpty())
            {
                logger.info("{} voted but is not known to the server!", vote.getUsername());
            }
            return;
        }
        final DSLContext dsl = db.getDSL();
        db.queryOne(dsl.selectFrom(TABLE_VOTE).where(TABLE_VOTE.USERID.eq(user.getEntity().getId()))).thenAcceptAsync((voteModel) -> {
            if (voteModel != null)
            {
                if (voteModel.timePassed(config.voteBonusTime.getMillis()))
                {
                    voteModel.setVotes(1);
                }
                else
                {
                    voteModel.addVote();
                }
                voteModel.update();
            }
            else
            {
                voteModel = dsl.newRecord(TABLE_VOTE).newVote(user);
                voteModel.insert();
            }
            economy.createAccount(user.getUniqueId());
            final int voteAmount = voteModel.getVotes();
            double money = this.config.voteReward * pow(1 + 1.5 / voteAmount, voteAmount - 1);
            economy.deposit(user.getUniqueId(), money);
            String moneyFormat = economy.format(money);
            um.broadcastMessage(NONE, ChatFormat.parseFormats(this.config.voteBroadcast)
                    .replace("{PLAYER}", vote.getUsername())
                    .replace("{MONEY}", moneyFormat).replace("{AMOUNT}", String.valueOf(voteAmount))
                    .replace("{VOTEURL}", this.config.voteUrl));
            user.sendMessage(ChatFormat.parseFormats(this.config.voteMessage
                                                         .replace("{PLAYER}", vote.getUsername())
                                                         .replace("{MONEY}", moneyFormat)
                                                         .replace("{AMOUNT}", String.valueOf(voteAmount))
                                                         .replace("{VOTEURL}", this.config.voteUrl)));
        });
    }

    public VoteConfiguration getConfig()
    {
        return config;
    }
}
