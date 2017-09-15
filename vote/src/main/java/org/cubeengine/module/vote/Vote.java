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

import static java.lang.Math.pow;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NONE;
import static org.cubeengine.module.vote.storage.TableVote.TABLE_VOTE;

import com.vexsoftware.votifier.sponge.event.VotifierEvent;
import de.cubeisland.engine.logscribe.Log;
import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.InjectService;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.Broadcaster;
import org.cubeengine.libcube.service.command.ModuleCommand;
import org.cubeengine.libcube.service.database.Database;
import org.cubeengine.libcube.service.database.ModuleTables;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.util.ChatFormat;
import org.cubeengine.module.vote.storage.TableVote;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.jooq.DSLContext;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A module to handle Votes coming from a {@link VotifierEvent}
 */
@Singleton
@Module(dependencies = @Dependency("nuvotifier"))
@ModuleTables(TableVote.class)
public class Vote extends CubeEngineModule
{
    @Inject private Database db;
    @Inject private Broadcaster bc;
    @Inject private ModuleManager mm;
    private Log logger;

    @ModuleConfig private VoteConfiguration config;
    @ModuleCommand private VoteCommands commands;
    @InjectService private EconomyService economy;

    @Listener
    public void onPreInit(GamePostInitializationEvent event)
    {
        this.logger = mm.getLoggerFor(Vote.class);
    }

    @Listener
    public void onVote(VotifierEvent event) throws ExecutionException, InterruptedException
    {
        final com.vexsoftware.votifier.model.Vote vote = event.getVote();
        UserStorageService uss = Sponge.getServiceManager().provideUnchecked(UserStorageService.class);
        Optional<User> user = uss.get(vote.getUsername());
        if (!user.isPresent())
        {
            if (vote.getUsername() == null || vote.getUsername().trim().isEmpty())
            {
                logger.info("{} voted but is not known to the server!", vote.getUsername());
            }
            return;
        }
        final DSLContext dsl = db.getDSL();
        db.queryOne(dsl.selectFrom(TABLE_VOTE).where(TABLE_VOTE.ID.eq(user.get().getUniqueId()))).thenAcceptAsync((voteModel) -> {
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
                voteModel = dsl.newRecord(TABLE_VOTE).newVote(user.get());
                voteModel.insert();
            }
            UniqueAccount acc = economy.getOrCreateAccount(user.get().getUniqueId()).get();
            final int voteAmount = voteModel.getVotes();
            double money = this.config.voteReward * pow(1 + 1.5 / voteAmount, voteAmount - 1);
            acc.deposit(economy.getDefaultCurrency(), new BigDecimal(money), Cause.of(EventContext.empty(), event.getVote()));
            Text moneyFormat = economy.getDefaultCurrency().format(new BigDecimal(money));
            bc.broadcastMessage(NONE, ChatFormat.parseFormats(this.config.voteBroadcast)
                    .replace("{PLAYER}", vote.getUsername())
                    .replace("{MONEY}", moneyFormat.toPlain()).replace("{AMOUNT}", String.valueOf(voteAmount))
                    .replace("{VOTEURL}", this.config.voteUrl));
            user.get().getPlayer().ifPresent(p -> p.sendMessage(Text.of(ChatFormat.parseFormats(this.config.voteMessage
                                                         .replace("{PLAYER}", vote.getUsername())
                                                         .replace("{MONEY}", moneyFormat.toPlain())
                                                         .replace("{AMOUNT}", String.valueOf(voteAmount))
                                                         .replace("{VOTEURL}", this.config.voteUrl)))));
        });
    }

    public VoteConfiguration getConfig()
    {
        return config;
    }
}
