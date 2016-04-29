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
package org.cubeengine.module.border;

import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.util.Triplet;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.task.TaskManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.World;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Command(name = "border", desc = "border commands")
public class BorderCommands extends ContainerCommand
{
    private final Border module;
    private I18n i18n;
    private TaskManager tm;

    public BorderCommands(Border module, I18n i18n, TaskManager tm)
    {
        super(module);
        this.module = module;
        this.i18n = i18n;
        this.tm = tm;
    }

    private LinkedList<Triplet<UUID,Integer,Integer>> chunksToGenerate;
    private LinkedList<Triplet<World,Integer,Integer>> chunksToUnload;
    private CommandSource sender = null;
    private int total = 0;
    private int totalDone = 0;
    private long lastNotify;
    private int generated;
    private boolean running = false;

    @Command(desc = "Sets the center of the border")
    public void setCenter(CommandSource context, @Optional Integer chunkX, @Optional Integer chunkZ,
                          @Default @Named({"in", "world", "w"}) World world, @Flag boolean spawn)
    {
        Chunk center;
        if (spawn)
        {
            this.module.getConfig(world).setCenter(world.getSpawnLocation().getChunk(), true);
            i18n.sendTranslated(context, POSITIVE, "Center for Border in {world} set to world spawn!", world);
            return;
        }
        else if (chunkZ != null)
        {
            center = world.getChunkAt(chunkX, chunkZ);
        }
        else if (context instanceof Player)
        {
            center = ((Player)context).getLocation().getChunk();
        }
        else
        {
            i18n.sendTranslated(context, NEGATIVE, "You need to specify the chunk coordinates or use the -spawn flag");
            return;
        }
        this.module.getConfig(world).setCenter(center, false);
        i18n.sendTranslated(context, POSITIVE, "Center for Border in {world} set!", world);
    }

    @Alias(value = "generateBorder")
    @Command(desc = "Generates the chunks located in the border")
    public void generate(CommandSource context, String world)
    {
        if (running)
        {
            i18n.sendTranslated(context, NEGATIVE, "Chunk generation is already running!");
            return;
        }
        this.chunksToGenerate = new LinkedList<>();
        this.chunksToUnload = new LinkedList<>();
        if ("*".equals(world))
        {
            for (World w : Sponge.getServer().getWorlds())
            {
                this.addChunksToGenerate(w, context);
            }
        }
        else
        {
            World w = Sponge.getServer().getWorld(world).orElse(null);
            if (w == null)
            {
                i18n.sendTranslated(context, NEGATIVE, "World {input} not found!", world);
                return;
            }
            this.addChunksToGenerate(w, context);
        }
        this.sender = context;
        this.total = this.chunksToGenerate.size();
        this.totalDone = 0;
        this.lastNotify = System.currentTimeMillis();
        this.generated = 0;
        this.scheduleGeneration(1);
    }

    private void addChunksToGenerate(World world, CommandSource sender)
    {
        BorderConfig config = this.module.getConfig(world);
        Chunk spawnChunk = world.getSpawnLocation().getChunk();
        final int spawnX = spawnChunk.getPosition().getX();
        final int spawnZ = spawnChunk.getPosition().getZ();
        int radius = config.radius;
        radius += Sponge.getServer().;
        int radiusSquared = radius * radius;
        int chunksAdded = 0;
        UUID worldID = world.getUniqueId()
        // Construct Spiral
        int curLen = 1;
        int curX = spawnX;
        int curZ = spawnZ;
        int dir = 1;
        while (curLen <= radius * 2)
        {
            for (int i = 0; i < curLen; i++)
            {
                curX += dir;
                if (addIfInBorder(config, worldID, curX, curZ, spawnX, spawnZ, radius,  radiusSquared))
                {
                    chunksAdded++;
                }
            }
            for (int i = 0; i < curLen; i++)
            {
                curZ += dir;
                if (addIfInBorder(config, worldID, curX, curZ, spawnX, spawnZ, radius, radiusSquared))
                {
                    chunksAdded++;
                }
            }
            curLen++;
            dir = -dir;
        }
        i18n.sendTranslated(sender, POSITIVE, "Added {amount} chunks to generate in {world}", chunksAdded, world);
    }

    private boolean addIfInBorder(BorderConfig config, UUID worldId, int x, int z, int spawnX, int spawnZ, int radius, int radiusSquared)
    {
        if (config.square)
        {
            if (Math.abs(spawnX - x) <= radius && Math.abs(spawnZ - z) <= radius)
            {
                this.chunksToGenerate.add(new Triplet<>(worldId, x, z));
                return true;
            }
        }
        else if (Math.pow(spawnX - x, 2) + Math.pow(spawnZ - z, 2) <= radiusSquared)
        {
            this.chunksToGenerate.add(new Triplet<>(worldId, x, z));
            return true;
        }
        return false;
    }

    private void scheduleGeneration(int inTicks)
    {
        this.running = true;
        tm.runTaskDelayed(module, BorderCommands.this::generate, inTicks);
    }
    private static final int TIMELIMIT = 40;

    private void generate()
    {
        long tickStart = System.currentTimeMillis();
        Runtime rt = Runtime.getRuntime();
        int freeMemory = (int)((rt.maxMemory() - rt.totalMemory() + rt.freeMemory()) / 1048576);// 1024*1024 = 1048576 (bytes in 1 MB)
        if (freeMemory < 300) // less than 300 MB memory left
        {
            this.scheduleGeneration(20 * 10); // Take a 10 second break
            i18n.sendTranslated(sender, NEGATIVE, "Available Memory getting low! Pausing Chunk Generation");
            rt.gc();
            return;
        }
        while (System.currentTimeMillis() - tickStart < TIMELIMIT)
        {
            if (chunksToGenerate.isEmpty())
            {
                break;
            }
            Triplet<UUID, Integer, Integer> poll = chunksToGenerate.poll();
            World world = Sponge.getServer().getWorld(poll.getFirst()).orElse(null);
            if (!world.getChunk(poll.getSecond(), 0, poll.getThird()).isPresent())
            {
                if (!world.loadChunk(poll.getSecond(), 0, poll.getThird(), false).isPresent())
                {
                    world.loadChunk(poll.getSecond(), 0, poll.getThird(), true);
                    generated++;
                }
                this.chunksToUnload.add(new Triplet<>(world, poll.getSecond(), poll.getThird()));
            }
            if (this.chunksToUnload.size() > 8)
            {
                Triplet<World, Integer, Integer> toUnload = chunksToUnload.poll();
                toUnload.getFirst().unloadChunk(chunk);
            }
            totalDone++;

            if (lastNotify + TimeUnit.SECONDS.toMillis(5) < System.currentTimeMillis())
            {
                this.lastNotify = System.currentTimeMillis();
                int percentNow = totalDone * 100 / total;
                i18n.sendTranslated(sender, POSITIVE, "Chunk generation is at {integer#percent}% ({amount#done}/{amount#total})", percentNow, totalDone, total);
            }
        }
        if (!chunksToGenerate.isEmpty())
        {
            this.scheduleGeneration(1);
        }
        else
        {
            for (Triplet<World, Integer, Integer> triplet : chunksToUnload)
            {
                triplet.getFirst().unloadChunk(triplet.getSecond(), triplet.getThird());
            }
            i18n.sendTranslated(sender, POSITIVE, "Chunk generation completed! Generated {amount} chunks", generated);
            rt.gc();
            this.running = false;
        }
    }
}
