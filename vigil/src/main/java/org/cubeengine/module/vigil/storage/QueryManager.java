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
package org.cubeengine.module.vigil.storage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;

import com.flowpowered.math.vector.Vector3d;
import com.mongodb.*;
import org.cubeengine.module.vigil.Receiver;
import org.cubeengine.module.vigil.report.*;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.entity.living.player.Player;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.stream.Collectors.toList;
import static org.cubeengine.module.vigil.report.Action.DATA;
import static org.cubeengine.module.vigil.report.Report.WORLD;
import static org.cubeengine.module.vigil.report.Report.X;
import static org.cubeengine.module.vigil.report.Report.Y;
import static org.cubeengine.module.vigil.report.block.BlockReport.BLOCK_CHANGES;

public class QueryManager
{
    private final Queue<Action> actions = new ConcurrentLinkedQueue<>();
    private final ExecutorService storeExecuter;
    private Future<?> storeFuture;
    private final ExecutorService queryExecuter;
    private Future<?> queryFuture;
    private final Semaphore latch = new Semaphore(1);

    private int batchSize = 2000; // TODO config
    private DBCollection db;
    private ReportManager reportManager;
    private I18n i18n;

    public QueryManager(ThreadFactory tf, DBCollection db, ReportManager reportManager, I18n i18n)
    {
        this.db = db;
        this.reportManager = reportManager;
        this.i18n = i18n;
        storeExecuter = newSingleThreadExecutor(tf);
        queryExecuter = newSingleThreadExecutor(tf);
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
        if (latch.availablePermits() > 0 && (storeFuture == null || storeFuture.isDone()))
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
            latch.acquire();
            if (actions.isEmpty())
            {
                return;
            }

            for (int i = 0; i < max && !actions.isEmpty(); i++)
            {
                storing.offer(actions.poll());
            }

            List<DBObject> storeList = storing.stream().map(Action::getDBObject).collect(toList());
            db.insert(storeList);
        }
        catch (MongoTimeoutException e)
        {
            System.out.println(e.getMessage());
        }
        catch (Exception e)
        {
            e.printStackTrace(); //TODO log in logger!
            actions.addAll(storing); // readd actions to store later // TODO this may cause duplicates!!
        }
        finally
        {
            // Release latch up to 1 permit
            if (latch.availablePermits() == 0)
            {
                latch.release();
            }
            // More actions available ; rerun
            if (!actions.isEmpty())
            {
                storeFuture = storeExecuter.submit(() -> store(this.batchSize));
            }
        }
    }

    public void queryAndShow(Object lookup, Player player) // TODO lookup object
    {
        // Build query from lookup
        BasicDBObject query = new BasicDBObject();
        query.put(DATA + "." + BLOCK_CHANGES + "." + Report.LOCATION + "." + WORLD.asString("_"), player.getWorld().getUniqueId().toString());
        if (lookup instanceof Vector3d)
        {
            query.put(DATA + "." + BLOCK_CHANGES + "." + Report.LOCATION + "." + X.asString("_"), ((Vector3d) lookup).getFloorX());
            query.put(DATA + "." + BLOCK_CHANGES + "." + Report.LOCATION + "." + Y.asString("_"), ((Vector3d) lookup).getFloorY());
            query.put(DATA + "." + BLOCK_CHANGES + "." + Report.LOCATION + "." + Report.Z.asString("_"), ((Vector3d) lookup).getFloorZ());
        }
        DBCursor results = db.find(query);

        List<ReportActions> reportActions = new ArrayList<>();
        ReportActions last = null;
        for (DBObject result : results)
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

        new Receiver(player, i18n, lookup).sendReports(reportActions);
    }
}