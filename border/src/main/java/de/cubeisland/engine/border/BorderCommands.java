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
package de.cubeisland.engine.border;

import java.util.LinkedList;

import org.bukkit.Chunk;
import org.bukkit.World;

import de.cubeisland.engine.core.command.CommandSender;
import de.cubeisland.engine.core.command.ContainerCommand;
import de.cubeisland.engine.core.command.parameterized.ParameterizedContext;
import de.cubeisland.engine.core.command.reflected.Alias;
import de.cubeisland.engine.core.command.reflected.Command;
import de.cubeisland.engine.core.util.Triplet;

public class BorderCommands extends ContainerCommand
{
    private Border module;


    public BorderCommands(Border module)
    {
        super(module, "border", "border commands");
        this.module = module;
    }

    private LinkedList<Triplet<Long,Integer,Integer>> chunksToGenerate;
    private CommandSender sender = null;
    private long tickStart;
    private int total = 0;
    private int totalDone = 0;
    private int lastPercent = 0;
    private int generated;
    private boolean running = false;

    @Alias(names = "generateBorder")
    @Command(desc = "Generates the chunks located in the border", min = 1, max = 1)
    public void generate(ParameterizedContext context)
    {
        if (running)
        {
            context.sendTranslated("&cChunk generation is already running!");
            return;
        }
        String worldName = context.getString(0);
        this.chunksToGenerate = new LinkedList<>();

        if (worldName.equals("*"))
        {
            for (World world : this.module.getCore().getWorldManager().getWorlds())
            {
                this.addChunksToGenerate(world, context.getSender());
            }
        }
        else
        {
            World world = this.module.getCore().getWorldManager().getWorld(worldName);
            if (world == null)
            {
                context.sendTranslated("&cThere is no world named &6%s", worldName);
                return;
            }
            this.addChunksToGenerate(world, context.getSender());
        }
        this.sender = context.getSender();
        this.total = this.chunksToGenerate.size();
        this.totalDone = 0;
        this.lastPercent = 0;
        this.generated = 0;
        this.scheduleGeneration();
    }

    private void addChunksToGenerate(World world, CommandSender sender)
    {
        Chunk spawnChunk = world.getSpawnLocation().getChunk();
        final int spawnX = spawnChunk.getX();
        final int spawnZ = spawnChunk.getZ();
        int radius = this.module.getConfig().radius;
        radius += sender.getServer().getViewDistance();
        int radiusSquared = radius * radius;
        int i = 0;
        long worldID = this.module.getCore().getWorldManager().getWorldId(world);
        for (int x = -radius + spawnX; x <= radius + spawnX; x++)
        {
            for (int z = -radius + spawnZ; z <= radius + spawnZ; z++)
            {
                if (this.module.getConfig().square)
                {
                    this.chunksToGenerate.add(new Triplet<>(worldID, x,z));
                    i++;
                }
                else if (Math.pow(spawnX - x, 2) + Math.pow(spawnZ - z, 2) <= radiusSquared)
                {
                    this.chunksToGenerate.add(new Triplet<>(worldID, x,z));
                    i++;
                }
            }
        }
        sender.sendTranslated("&aAdded &6%d &achunks to generate in &6%s", i, world.getName());
    }

    private void scheduleGeneration()
    {
        this.running = true;
        this.module.getCore().getTaskManager().runTaskDelayed(module, new Runnable()
        {
            @Override
            public void run()
            {
                BorderCommands.this.generate();
            }
        }, 1);
    }

    private void generate()
    {
        this.tickStart = System.currentTimeMillis();
        this.generate0();
    }

    private void generate0()
    {
        if (chunksToGenerate.isEmpty())
        {
            sender.sendTranslated("&aChunkgeneration completed! Generated &6%d&a chunks", generated);
            this.running = false;
        }
        else
        {
            // 50 ms = 1 tick
            if (System.currentTimeMillis() - this.tickStart > 20) // 20ms have passed (less than half a tick)
            {
                this.scheduleGeneration();
                return;
            }
            Triplet<Long, Integer, Integer> poll = chunksToGenerate.poll();
            World world = this.module.getCore().getWorldManager().getWorld(poll.getFirst());
            Chunk chunk = world.getChunkAt(poll.getSecond(), poll.getThird());
            if (!chunk.isLoaded())
            {
                if (chunk.load(false))
                {
                    // chunk exists!
                }
                else
                {
                    chunk.load(true); // Load & generate chunk
                    generated++;
                }
                chunk.getWorld().unloadChunk(chunk); // and unload
            }
            totalDone++;
            int percentNow = totalDone * 100 / total;
            if (percentNow > lastPercent + 3)
            {
                lastPercent = percentNow;
                this.sender.sendTranslated("&aChunkgeneration is at &6%d%% &a(&6%d/%d&a)", percentNow, totalDone, total);
            }
            this.generate0();
        }
    }
}
