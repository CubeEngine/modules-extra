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
package org.cubeengine.module.freezedetection;


import java.io.File;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

import de.cubeisland.engine.logscribe.Log;
import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;

@Singleton
@Module(id = "freezedetection", name = "FreezeDetection", version = "1.0.0",
        description = "Detects server freeze and produces thread dumps",
        dependencies = @Dependency("cubeengine-core"),
        url = "http://cubeengine.org",
        authors = {"Anselm 'Faithcaio' Brehme", "Phillip Schichtel"})
public class FreezeDetection extends CubeEngineModule
{
    @Inject private TaskManager taskManager;
    private Log logger;
    @Inject private ThreadFactory tf;
    @Inject File pluginFolder;
    @Inject ModuleManager mm;

    private ScheduledExecutorService executor;
    private UUID taskId;
    private long lastHeartbeat = -1;
    private final long freezeThreshold = TimeUnit.SECONDS.toMillis(20);
    private final ConcurrentLinkedQueue<Runnable> listeners = new ConcurrentLinkedQueue<>();
    private volatile boolean freezeNotified = false;

    @Listener
    public void onEnable(GamePreInitializationEvent event)
    {
        this.logger = mm.getLoggerFor(FreezeDetection.class);
        start();
        addListener(new ThreadDumpListener(logger, pluginFolder.toPath()));
    }

    @Listener
    public void onDisable(GameStoppingEvent event)
    {
        shutdown();
    }

    public void addListener(Runnable r)
    {
        this.listeners.add(r);
    }

    public void removeListener(Runnable r)
    {
        this.listeners.remove(r);
    }

    public void start()
    {
        this.taskId = this.taskManager.runAsynchronousTimer(FreezeDetection.class, new HeartbeatLogger(), 0, 1);
        if (this.taskId == null)
        {
            throw new RuntimeException("Failed to schedule the heartbeat logging for freeze detection");
        }
        this.executor = Executors.newSingleThreadScheduledExecutor(tf);
        this.executor.scheduleAtFixedRate(new FreezeDetector(), this.freezeThreshold, this.freezeThreshold, TimeUnit.MILLISECONDS);

        this.lastHeartbeat = System.currentTimeMillis();
    }

    public void shutdown()
    {
        if (this.taskId != null)
        {
            this.taskManager.cancelTask(FreezeDetection.class, this.taskId);
            this.taskId = null;
        }
        if (this.executor != null)
        {
            this.executor.shutdown();
            try
            {
                this.executor.awaitTermination(2, TimeUnit.SECONDS);
            }
            catch (InterruptedException ignored)
            {}
            this.executor = null;
        }

        this.listeners.clear();
    }

    private class HeartbeatLogger implements Runnable
    {
        @Override
        public void run()
        {
            lastHeartbeat = System.currentTimeMillis();
            freezeNotified = false;
        }
    }

    private class FreezeDetector implements Runnable
    {
        @Override
        public void run()
        {
            if (System.currentTimeMillis() - lastHeartbeat > freezeThreshold && !freezeNotified)
            {
                freezeNotified = true;
                listeners.forEach(Runnable::run);
            }
        }
    }
}
