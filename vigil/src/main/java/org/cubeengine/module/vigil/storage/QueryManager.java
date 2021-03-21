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
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.MongoTimeoutException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.cubeengine.module.bigdata.Bigdata;
import org.cubeengine.module.vigil.Lookup;
import org.cubeengine.module.vigil.Receiver;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.Report;
import org.cubeengine.module.vigil.report.ReportActions;
import org.cubeengine.module.vigil.report.ReportManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.TaskExecutorService;
import org.spongepowered.plugin.PluginContainer;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.stream.Collectors.toList;

@Singleton
public class QueryManager
{
    private final Queue<Action> actions = new ConcurrentLinkedQueue<>();
    private final ExecutorService storeExecuter;
    private PluginContainer plugin;
    private Future<?> storeFuture;
    private TaskExecutorService queryShowExecutor;
    private Map<UUID, CompletableFuture<Void>> queryFuture = new HashMap<>();
    private final Semaphore storeLatch = new Semaphore(1);

    private int batchSize = 2000; // TODO config
    private MongoCollection<Document> mongo;
    private Bigdata bigdata;
    private ReportManager reportManager;
    private I18n i18n;

    private List<Consumer<Action>> callbacks = new ArrayList<>();

    private Map<UUID, Lookup> lastLookups = new HashMap<>();

    @Inject
    public QueryManager(ThreadFactory tf, Bigdata bigdata, ReportManager reportManager, I18n i18n, PluginContainer plugin)
    {
        this.bigdata = bigdata;
        this.reportManager = reportManager;
        this.i18n = i18n;
        this.storeExecuter = newSingleThreadExecutor(tf);
        this.plugin = plugin;
    }

    public void initQueryShowExecutor()
    {
        if (this.queryShowExecutor == null)
        {
            this.queryShowExecutor = Sponge.server().scheduler().createExecutor(plugin);
        }
    }

    public MongoCollection<Document> vigilCollection()
    {
        if (this.mongo == null)
        {
            this.mongo = bigdata.getDatabase().getCollection("vigil");
            this.mongo.createIndex(Indexes.hashed("type"));
            this.mongo.createIndex(Indexes.descending("date"));
            this.mongo.createIndex(Indexes.ascending("data.location.Position_X", "data.location.Position_Z", "data.location.Position_Y"));
            this.mongo.createIndex(Indexes.hashed("data.location.WorldUuid"));
        }
        return this.mongo;
    }

    /**
     * Queues in an action to be persisted
     *
     * @param action the action to be persisted
     */
    public void report(Action action)
    {
        if (!this.storeExecuter.isShutdown())
        {
            actions.add(action);
            // Start inserting queued actions ; if not already running
            if (storeLatch.availablePermits() > 0 && (storeFuture == null || storeFuture.isDone()))
            {
                storeFuture = storeExecuter.submit(() -> store(batchSize));
            }

            callbacks.forEach(c -> c.accept(action));
        }
    }

    public void addCallback(Consumer<Action> callback) {
        this.callbacks.add(callback);
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
            vigilCollection().insertMany(storeList);
        }
        catch (MongoTimeoutException e)
        {
            System.out.println(e.getMessage());
        }
        catch (Exception e)
        {

            // TODO better exception handling
            System.err.print("[Vigil] " + e.getMessage() + "\n");
            System.err.println("The following documents were discarded:");
            for (Action action : storing)
            {
                System.err.println(action.getDocument().toString());
            }
//            actions.addAll(storing); // read actions to store later // TODO this may cause duplicates!!
        }
        finally
        {
            // Release latch up to 1 pe rmit
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
        this.initQueryShowExecutor();
        this.lastLookups.put(player.uniqueId(), lookup);

        // TODO lookup cancel previous?
        CompletableFuture<Void> future = queryFuture.get(player.uniqueId());
        if (future != null && !future.isDone())
        {
            i18n.send(ChatType.ACTION_BAR, player, MessageType.NEGATIVE, "There is another lookup active!");
            return;
        }
        Query query = buildQuery(lookup);

        future = CompletableFuture.supplyAsync(() -> lookup(lookup, query)) // Async MongoDB Lookup
                                                                               .thenApply(result -> this.prepareReports(lookup, player, result)) // Still Async Prepare Reports
                                                                               .thenAcceptAsync(r -> this.show(lookup, player, r), queryShowExecutor)// Resync to show information
            .exceptionally(t -> {
                plugin.getLogger().error("Error showing reports", t);
                return null;
            });
        queryFuture.put(player.uniqueId(), future);
    }

    private List<Action> lookup(Lookup lookup, Query query)
    {
        lookup.time(Lookup.LookupTiming.LOOKUP);
        List<Action> actions = new ArrayList<>();
        FindIterable<Document> results = query.find(vigilCollection()).sort(new Document("date", -1));
        for (Document result : results)
        {
            actions.add(new Action(result));
        }
        lookup.time(Lookup.LookupTiming.LOOKUP);
        return actions;
    }

    private List<ReportActions> prepareReports(Lookup lookup, Player player, List<Action> results)
    {
        lookup.time(Lookup.LookupTiming.REPORT);

        List<ReportActions> reportActions = new ArrayList<>();
        ReportActions last = null;
        for (Action action : results)
        {
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

        List<String> reportFilters = lookup.getSettings().getReports();
        if (!reportFilters.isEmpty())
        {
            query.reportFilters(reportFilters);
        }

        if (lookup.getRadius() != 0)
        {
            query.radius(lookup.getPosition(), lookup.getRadius());
        }
        else if (lookup.getPosition() != null)
        {
            query.position(lookup.getPosition());
        }

        if (lookup.prepared() != null)
        {
            query.prepared(lookup.prepared());
        }

        return query;
    }

    public void purge()
    {
        this.vigilCollection().deleteMany(new Document());
    }

    public Optional<Lookup> getLast(Player player)
    {
        return Optional.ofNullable(this.lastLookups.get(player.uniqueId()));
    }

    public void shutdown()
    {
        System.out.println("Shutting down Vigil");
        if (this.queryShowExecutor != null)
        {
            this.queryShowExecutor.shutdown();
        }
        try
        {
            if (this.storeExecuter != null)
            {
                this.storeExecuter.shutdown();
                this.storeExecuter.awaitTermination(30, TimeUnit.SECONDS);
            }
        }
        catch (InterruptedException e)
        {
            throw new IllegalStateException(e);
        }
    }
}
