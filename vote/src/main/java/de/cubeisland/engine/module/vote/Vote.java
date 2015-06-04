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
package de.cubeisland.engine.module.vote;

import com.vexsoftware.votifier.model.VotifierEvent;
import de.cubeisland.engine.module.service.command.CommandManager;
import de.cubeisland.engine.module.core.module.Module;
import de.cubeisland.engine.module.service.Economy;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.core.util.ChatFormat;
import de.cubeisland.engine.module.vote.storage.TableVote;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jooq.DSLContext;

import de.cubeisland.engine.module.core.util.formatter.MessageType.NONE;
import static de.cubeisland.engine.module.vote.storage.TableVote.TABLE_VOTE;
import static java.lang.Math.pow;

/**
 * A module to handle Votes coming from a {@link VotifierEvent}
 */
public class Vote extends Module implements Listener
{
    private VoteConfiguration config;
    private Economy economy;

    @Override
    public void onEnable()
    {
        this.getCore().getDB().registerTable(TableVote.class);
        this.config = this.loadConfig(VoteConfiguration.class);
        this.getCore().getEventManager().registerListener(this, this);
        CommandManager cm = this.getCore().getCommandManager();
        cm.addCommands(cm, this, new VoteCommands(this));
        this.economy = getCore().getModuleManager().getServiceManager().getServiceImplementation(Economy.class);
    }

    @EventHandler
    private void onVote(VotifierEvent event)
    {
        final com.vexsoftware.votifier.model.Vote vote = event.getVote();
        final User user = getCore().getUserManager().findExactUser(vote.getUsername());
        if (user == null)
        {
            if (vote.getUsername() == null || vote.getUsername().trim().isEmpty())
            {
                this.getLog().info("{} voted but is not known to the server!", vote.getUsername());
            }
            return;
        }
        final DSLContext dsl = getCore().getDB().getDSL();
        getCore().getDB().queryOne(dsl.selectFrom(TABLE_VOTE).where(TABLE_VOTE.USERID.eq(user.getEntity().getKey()))).thenAcceptAsync((voteModel) -> {
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
            this.getCore().getUserManager().broadcastMessage(NONE, ChatFormat.parseFormats(this.config.voteBroadcast)
                                                                             .replace("{PLAYER}", vote.getUsername())
                                                                             .replace("{MONEY}", moneyFormat)
                                                                             .replace("{AMOUNT}", String.valueOf(voteAmount))
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
