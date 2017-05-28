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
package org.cubeengine.module.vigil.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;

import com.mongodb.MongoTimeoutException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.cubeengine.module.vigil.Lookup;
import org.cubeengine.module.vigil.Receiver;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.Report;
import org.cubeengine.module.vigil.report.ReportActions;
import org.cubeengine.module.vigil.report.ReportManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.text.chat.ChatTypes;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.stream.Collectors.toList;

public class QueryManager
{
    private final Queue<Action> actions = new ConcurrentLinkedQueue<>();
    private final ExecutorService storeExecuter;
    private PluginContainer plugin;
    private Future<?> storeFuture;
    private final SpongeExecutorService queryShowExecutor;
    private Map<UUID, Future> queryFuture = new HashMap<>();
    private final Semaphore storeLatch = new Semaphore(1);

    private int batchSize = 2000; // TODO config
    private MongoCollection<Document> db;
    private ReportManager reportManager;
    private I18n i18n;

    public QueryManager(ThreadFactory tf, MongoCollection<Document> db, ReportManager reportManager, I18n i18n, PluginContainer plugin)
    {
        this.db = db;
        this.reportManager = reportManager;
        this.i18n = i18n;
        storeExecuter = newSingleThreadExecutor(tf);
        queryShowExecutor = Sponge.getScheduler().createSyncExecutor(plugin);
        this.plugin = plugin;
    }


    /**
     * Queues in an action to be persisted
     *
     * @param action the action to be persisted
     */
    public void report(Action action)
    {
        actions.add(action);
        // Start inserting queued actions ; if not already running
        if (storeLatch.availablePermits() > 0 && (storeFuture == null || storeFuture.isDone()))
        {
            storeFuture = storeExecuter.submit(() -> store(batchSize));
        }
    }

    /**
     * Attempts to store up to {@code max} {@link Action}s
     * @param max the max amount of actions to store in one batch insert
     */
    private void store(int max)
    {
        // TODO add statistics

        final Queue<Action> storing = new LinkedList<>();

        try
        {
            storeLatch.acquire();
            if (actions.isEmpty())
            {
                return;
            }

            for (int i = 0; i < max && !actions.isEmpty(); i++)
            {
                storing.offer(actions.poll());
            }

            List<Document> storeList = storing.stream().map(Action::getDocument).collect(toList());
            db.insertMany(storeList);
        }
        catch (MongoTimeoutException e)
        {
            System.out.println(e.getMessage());
        }
        catch (Exception e)
        {
            System.out.print(e.getMessage() + "\n");
            //e.printStackTrace(); //TODO log in logger!
            actions.addAll(storing); // read actions to store later // TODO this may cause duplicates!!
        }
        finally
        {
            // Release latch up to 1 permit
            if (storeLatch.availablePermits() == 0)
            {
                storeLatch.release();
            }
            // More actions available ; rerun
            if (!actions.isEmpty())
            {
                storeFuture = storeExecuter.submit(() -> store(this.batchSize));
            }
        }
    }

    public void queryAndShow(Lookup lookup, Player player) // TODO lookup object
    {
        // TODO lookup cancel previous?
        Future future = queryFuture.get(player.getUniqueId());
        if (future != null && !future.isDone())
        {
            i18n.send(ChatTypes.ACTION_BAR, player, MessageType.NEGATIVE,"There is another lookup active!");
            return;
        }
        Query query = buildQuery(lookup);

        future = CompletableFuture.supplyAsync(() -> lookup(lookup, query)) // Async MongoDB Lookup
                .thenApply(result -> this.prepareReports(lookup, player, result)) // Still Async Prepare Reports
                .thenAcceptAsync(r -> this.show(lookup, player, r), queryShowExecutor); // Resync to show information
        queryFuture.put(player.getUniqueId(), future);
    }

    private FindIterable<Document> lookup(Lookup lookup, Query query)
    {
        lookup.time(Lookup.LookupTiming.LOOKUP);
        FindIterable<Document> documents = query.find(db);
        lookup.time(Lookup.LookupTiming.LOOKUP);
        return documents;
    }

    private List<ReportActions> prepareReports(Lookup lookup, Player player, FindIterable<Document> results)
    {
        lookup.time(Lookup.LookupTiming.REPORT);

        results.sort(new Document("date", -1));
        List<ReportActions> reportActions = new ArrayList<>();
        ReportActions last = null;
        for (Document result : results)
        {
            Action action = new Action(result);
            Report report = reportManager.reportOf(action);
            if (last == null)
            {
                last = new ReportActions(report);
                reportActions.add(last);
            }
            if (!last.add(action, report, lookup))
            {
                last = new ReportActions(report);
                reportActions.add(last);
                last.add(action, report, lookup);
            }
        }

        lookup.time(Lookup.LookupTiming.REPORT);
        return reportActions;
    }

    private void show(Lookup lookup, Player player, List<ReportActions> reportActions)
    {
        new Receiver(player, i18n, lookup).sendReports(reportActions);
    }

    private Query buildQuery(Lookup lookup)
    {
        // Build query from lookup
        Query query = new Query();

        // TODO lookup settings
        query.world(lookup.getWorld());

        if (lookup.getPosition() != null)
        {
            query.position(lookup.getPosition());
        }
        return query;
    }

    private void prepareReports()
    {

    }

    public void purge()
    {
        this.db.deleteMany(new Document());
    }
}
