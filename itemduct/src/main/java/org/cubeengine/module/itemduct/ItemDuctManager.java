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
package org.cubeengine.module.itemduct;

import static org.spongepowered.api.block.BlockTypes.BLACK_STAINED_GLASS;
import static org.spongepowered.api.block.BlockTypes.BLACK_STAINED_GLASS_PANE;
import static org.spongepowered.api.block.BlockTypes.DROPPER;
import static org.spongepowered.api.block.BlockTypes.GLASS;
import static org.spongepowered.api.block.BlockTypes.GLASS_PANE;
import static org.spongepowered.api.block.BlockTypes.PISTON;
import static org.spongepowered.api.block.BlockTypes.QUARTZ_BLOCK;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.TextComponent;
import org.cubeengine.module.itemduct.data.DuctData;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleOptions;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.math.vector.Vector3d;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class ItemDuctManager
{
    private Set<BlockType> pipeTypes = new HashSet<>();
    private Set<BlockType> directionalPipeTypes = new HashSet<>();
    private Set<BlockType> storagePipeTypes = new HashSet<>();
    private Set<Direction> directions = EnumSet.of(Direction.DOWN, Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    private int maxDepth = 10;


    public void setup(ItemductConfig config)
    {
        this.reload(config);
    }

    public void updateUses(ItemStack item)
    {
        int uses = item.get(DuctData.USES).orElse(0);
        if (uses > 0)
        {
            item.offer(Keys.LORE, Collections.singletonList(TextComponent.of("Uses: ").append(TextComponent.of(uses))));
        }
        else if (uses == -1)
        {
            item.offer(Keys.LORE, Collections.singletonList(TextComponent.of("Uses: Infinite")));
        }
    }

    public void reload(ItemductConfig config)
    {
        // TODO all glass types
        this.pipeTypes.clear();
        this.pipeTypes.add(GLASS.get());
        this.directionalPipeTypes.add(BLACK_STAINED_GLASS.get());

        this.pipeTypes.add(GLASS_PANE.get());
        this.directionalPipeTypes.add(BLACK_STAINED_GLASS_PANE.get());

        this.pipeTypes.addAll(this.directionalPipeTypes);

        this.storagePipeTypes.clear();
        this.storagePipeTypes.add(QUARTZ_BLOCK.get());

        this.maxDepth = config.maxDepth;
    }

    public Network findNetwork(ServerLocation start)
    {
        Direction dir = start.get(Keys.DIRECTION).orElse(Direction.NONE);
        if ((!start.getBlockType().equals(DROPPER)))
        {
            dir = dir.getOpposite();
        }
        Network network = new Network(this);
        ServerLocation rel = start.add(dir.asBlockOffset());
        if (pipeTypes.contains(rel.getBlockType()) || storagePipeTypes.contains(rel.getBlockType()))
        {
            start = rel;
            network.pipes.add(start);
            findNetwork(new LastDuct(start, dir), network, 0);
        }
        return network;
    }

    private void findNetwork(LastDuct last, Network network, int depth)
    {
        if (depth > maxDepth)
        {
            network.errors.add(last.loc);
            return;
        }
        Map<Direction, ServerLocation> map = new HashMap<>();
        Queue<ServerLocation> next = new LinkedList<>();
        next.offer(last.loc);
        do
        {
            // TODO limit direction with glass panes
            Set<Direction> dirs = directions;
            for (Direction dir : dirs)
            {
                if (!dir.equals(last.from.getOpposite()))
                {
                    ServerLocation rel = last.loc.add(dir.asBlockOffset());
                    if (last.isCompatible(rel))
                    {
                        if (network.pipes.contains(rel)) // No loops allowed
                        {
                            network.errors.add(rel);
                            network.errors.add(last.loc);
                        }
                        else
                        {
                            network.pipes.add(rel);
                            map.put(dir, rel);
                        }
                    }
                    // ExitPiston?
                    if (rel.getBlockType().equals(PISTON) && rel.get(Keys.DIRECTION).orElse(Direction.NONE).equals(dir))
                    {
                        final ServerLocation relLoc = rel.add(dir.asBlockOffset());
                        if (relLoc.get(DuctData.FILTERS).map(d -> d.get(dir.getOpposite()) != null).orElse(false))
                        {
                            network.exitPoints.put(rel, relLoc.get(DuctData.FILTERS).get());
                        }
                    }
                    // Storage Chest
                    if (last.storage)
                    {
                        rel.getBlockEntity().ifPresent(te -> {
                            if (te instanceof Carrier) {
                                network.storage.add(rel);
                            }
                        });
                    }
                }

            }

            if (map.size() > 1)
            {
                for (Map.Entry<Direction, ServerLocation> entry : map.entrySet())
                {
                    findNetwork(new LastDuct(entry.getValue(), entry.getKey()), network, depth + 1);
                }
            }
            else if (map.size() == 1)
            {
                for (Map.Entry<Direction, ServerLocation> entry : map.entrySet())
                {
                    last.update(entry.getValue(), entry.getKey());
                }
                next.offer(last.loc);
            }
            // else nothing found here
            next.poll();
            map.clear();
        }
        while (!next.isEmpty());

    }

    public void playEffect(ServerLocation loc)
    {
        ParticleEffect badEffect = ParticleEffect.builder().type(ParticleTypes.BARRIER).build();
        ParticleEffect goodEffect = ParticleEffect.builder().type(ParticleTypes.DUST).option(ParticleOptions.COLOR, Color.GREEN).build();
        ParticleEffect neutralEffect = ParticleEffect.builder().type(ParticleTypes.DUST).option(ParticleOptions.COLOR, Color.YELLOW).build();
        ParticleEffect smoke = ParticleEffect.builder().type(ParticleTypes.LARGE_SMOKE).build();
        Vector3d center = loc.getPosition().add(0.5, 0.5, 0.5);
        for (Direction effectDir : Direction.values())
        {
            if (effectDir.isCardinal() || effectDir.isUpright())
            {
                loc.getWorld().spawnParticles(goodEffect, center.add(effectDir.asOffset().div(1.9)));
            }
        }

        loc.getWorld().playSound(Sound.of(SoundTypes.BLOCK_DISPENSER_DISPENSE, Sound.Source.NEUTRAL, 1, 0), loc.getPosition());

        Network network = findNetwork(loc);
        //System.out.print("Network: Pipes " +  network.pipes.size() + " Exits " + network.exitPoints.size() + " Storage " + network.storage.size() + "\n");

        for (ServerLocation pipe : network.pipes)
        {
            Vector3d pos = pipe.getPosition().add(0.5, 0.5, 0.5);
            if (storagePipeTypes.contains(pipe.getBlockType()))
            {
                for (Direction effectDir : Direction.values())
                {
                    if (effectDir.isCardinal() || effectDir.isUpright())
                    {
                        pipe.getWorld().spawnParticles(network.errors.isEmpty() ? goodEffect : neutralEffect, pos.add(effectDir.asOffset().div(1.9)));
                    }
                }
            }
            else
            {
                pipe.getWorld().spawnParticles(network.errors.isEmpty() ? goodEffect : neutralEffect, pos);
            }
            if (network.exitPoints.isEmpty() && network.storage.isEmpty())
            {
                pipe.getWorld().spawnParticles(smoke, pos);
            }
        }

        for (ServerLocation error : network.errors)
        {
            error.getWorld().spawnParticles(badEffect, error.getPosition().add(0.5,0.5,0.5));
            error.getWorld().spawnParticles(smoke, error.getPosition().add(0.5,0.5,0.5));
        }

        for (ServerLocation exit : network.exitPoints.keySet())
        {
            center = exit.getPosition().add(0.5,0.5,0.5);
            for (Direction effectDir : Direction.values())
            {
                if (effectDir.isCardinal() || effectDir.isUpright())
                {
                    exit.getWorld().spawnParticles(goodEffect, center.add(effectDir.asOffset().div(1.9)));
                }
            }
            // exit.getExtent().playSound(SoundTypes.BLOCK_DISPENSER_DISPENSE, exit.getPosition(), 1);
        }
    }

    private class LastDuct
    {
        public ServerLocation loc;
        public DyeColor color;
        public boolean cross;
        public Direction from;

        public boolean storage;

        public LastDuct(ServerLocation loc, Direction from)
        {
            this.update(loc, from);
        }

        public void update(ServerLocation loc, Direction from)
        {
            this.from = from;
            this.loc = loc;
            this.color = loc.get(Keys.DYE_COLOR).orElse(null);
            this.cross = directionalPipeTypes.contains(loc.getBlockType());
            this.storage = storagePipeTypes.contains(loc.getBlockType());
        }

        public boolean isCompatible(ServerLocation rel)
        {
            if (!pipeTypes.contains(rel.getBlockType()) && !storagePipeTypes.contains(rel.getBlockType()))
            {
                return false;
            }
            if (storage)
            {
                return storagePipeTypes.contains(rel.getBlockType());
            }
            if (storagePipeTypes.contains(rel.getBlockType()))
            {
                return false;
            }
            DyeColor color = rel.get(Keys.DYE_COLOR).orElse(null);
            return color == null || color == this.color || this.color == null;
        }

    }
}
