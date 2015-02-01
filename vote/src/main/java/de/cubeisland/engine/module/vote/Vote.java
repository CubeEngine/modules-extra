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

import java.sql.Timestamp;
import com.vexsoftware.votifier.model.VotifierEvent;
import de.cubeisland.engine.core.module.Module;
import de.cubeisland.engine.core.module.service.Economy;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.ChatFormat;
import de.cubeisland.engine.module.vote.storage.TableVote;
import de.cubeisland.engine.module.vote.storage.VoteModel;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jooq.DSLContext;
import org.jooq.types.UShort;

import static de.cubeisland.engine.core.util.formatter.MessageType.NONE;
import static de.cubeisland.engine.module.vote.storage.TableVote.TABLE_VOTE;
import static java.lang.Math.pow;

public class Vote extends Module implements Listener
{
    private VoteConfiguration config;
    DSLContext dsl;
    private Economy economy;

    @Override
    public void onEnable()
    {
        this.getCore().getDB().registerTable(TableVote.class);
        this.config = this.loadConfig(VoteConfiguration.class);
        this.getCore().getEventManager().registerListener(this, this);
        this.getCore().getCommandManager().addCommands(this.getCore().getCommandManager(), this, new VoteCommands(this));
        this.dsl = this.getCore().getDB().getDSL();
        this.economy = getCore().getModuleManager().getServiceManager().getServiceImplementation(Economy.class);
    }

    @EventHandler
    private void onVote(VotifierEvent event)
    {
        final com.vexsoftware.votifier.model.Vote vote = event.getVote();
        if (this.getCore().getUserManager().findExactUser(vote.getUsername()) != null)
        {
            User user = getCore().getUserManager().findExactUser(vote.getUsername());
            VoteModel voteModel = this.dsl.selectFrom(TABLE_VOTE).where(TABLE_VOTE.USERID.eq(user.getEntity().getKey())).fetchOne();
            if (voteModel == null)
            {
                voteModel = this.dsl.newRecord(TABLE_VOTE).newVote(user);
                voteModel.insertAsync();
            }
            else
            {
                if (System.currentTimeMillis() - voteModel.getValue(TABLE_VOTE.LASTVOTE).getTime() > this.config.voteBonusTime.getMillis())
                {
                    voteModel.setValue(TABLE_VOTE.VOTEAMOUNT, UShort.valueOf(1));
                }
                else
                {
                    voteModel.setValue(TABLE_VOTE.VOTEAMOUNT, UShort.valueOf(voteModel.getValue(TABLE_VOTE.VOTEAMOUNT).intValue() + 1));
                }
                voteModel.setValue(TABLE_VOTE.LASTVOTE, new Timestamp(System.currentTimeMillis()));
                voteModel.updateAsync();
            }
            economy.createAccount(user.getUniqueId());
            final int voteAmount = voteModel.getValue(TABLE_VOTE.VOTEAMOUNT).intValue();
            double money = this.config.voteReward * pow(1 + 1.5 / voteAmount, voteAmount - 1);
            economy.deposit(user.getUniqueId(), money);
            String moneyFormat = economy.format(money);
            this.getCore().getUserManager().broadcastMessage(NONE, this.config.voteBroadcast
                .replace("{PLAYER}", vote.getUsername())
                .replace("{MONEY}", moneyFormat)
                .replace("{AMOUNT}", String.valueOf(voteAmount))
                .replace("{VOTEURL}", this.config.voteUrl));
            user.sendMessage(ChatFormat.parseFormats(this.config.voteMessage
                .replace("{PLAYER}", vote.getUsername())
                .replace("{MONEY}", moneyFormat)
                .replace("{AMOUNT}", String.valueOf(voteAmount))
                .replace("{VOTEURL}", this.config.voteUrl)));
        }
        else if (vote.getUsername() == null || vote.getUsername().trim().isEmpty())
        {
            this.getLog().info("{} voted but is not known to the server!", vote.getUsername());
        }
    }

    public VoteConfiguration getConfig()
    {
        return config;
    }
}
