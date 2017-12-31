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

import static java.util.Collections.singletonList;
import static org.spongepowered.api.block.BlockTypes.DROPPER;
import static org.spongepowered.api.block.BlockTypes.GLASS;
import static org.spongepowered.api.block.BlockTypes.GLASS_PANE;
import static org.spongepowered.api.block.BlockTypes.PISTON;
import static org.spongepowered.api.block.BlockTypes.QUARTZ_BLOCK;
import static org.spongepowered.api.block.BlockTypes.STAINED_GLASS;
import static org.spongepowered.api.block.BlockTypes.STAINED_GLASS_PANE;

import com.flowpowered.math.vector.Vector3d;
import org.cubeengine.module.itemduct.data.DuctData;
import org.cubeengine.module.itemduct.data.DuctDataBuilder;
import org.cubeengine.module.itemduct.data.ImmutableDuctData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleOptions;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe;
import org.spongepowered.api.item.recipe.crafting.Ingredient;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class ItemDuctManager
{
    private boolean init = false;

    private ItemStack activatorItem;
    private Set<BlockType> pipeTypes = new HashSet<>();
    private Set<Direction> directions = EnumSet.of(Direction.DOWN, Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    private int maxDepth = 10;

    public void setup(PluginContainer plugin, ItemductConfig config)
    {
        if (!this.init)
        {
            DataRegistration.<DuctData, ImmutableDuctData>builder()
                    .dataClass(DuctData.class).immutableClass(ImmutableDuctData.class)
                    .builder(new DuctDataBuilder()).manipulatorId("duct")
                    .dataName("CubeEngine ItemDuct Data")
                    .buildAndRegister(plugin);

            DuctData.FILTERS.getQuery();

            Ingredient hopper = Ingredient.of(ItemTypes.HOPPER);
            activatorItem = ItemStack.of(ItemTypes.HOPPER, 1);
            activatorItem.offer(Keys.ITEM_ENCHANTMENTS, singletonList(Enchantment.builder().type(EnchantmentTypes.LOOTING).level(1).build()));
            activatorItem.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GOLD, "ItemDuct Activator"));
            activatorItem.offer(Keys.HIDE_ENCHANTMENTS, true);
            Sponge.getRegistry().getCraftingRecipeRegistry().register(
                    CraftingRecipe.shapedBuilder().rows()
                            .row(hopper, hopper, hopper)
                            .row(hopper, Ingredient.of(ItemTypes.DIAMOND), hopper)
                            .row(hopper, hopper, hopper)
                            .result(activatorItem).build("ItemDuctActivator", plugin));
        }

        this.reload(config);

        this.init = true;
    }

    public void reload(ItemductConfig config)
    {
        this.pipeTypes.clear();
        this.pipeTypes.add(GLASS);
        this.pipeTypes.add(GLASS_PANE);
        this.pipeTypes.add(STAINED_GLASS);
        this.pipeTypes.add(STAINED_GLASS_PANE);
        this.pipeTypes.add(QUARTZ_BLOCK);

        this.maxDepth = config.maxDepth;
    }

    public Network findNetwork(Location<World> start)
    {
        Direction dir = start.get(Keys.DIRECTION).orElse(Direction.NONE);
        if ((!start.getBlockType().equals(DROPPER)))
        {
            dir = dir.getOpposite();
        }
        Network network = new Network(this);
        if (pipeTypes.contains(start.getRelative(dir).getBlockType()))
        {
            start = start.getRelative(dir);
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
        Map<Direction, Location<World>> map = new HashMap<>();
        Queue<Location<World>> next = new LinkedList<>();
        next.offer(last.loc);
        do
        {
            // TODO limit direction with glass panes
            Set<Direction> dirs = directions;
            for (Direction dir : dirs)
            {
                if (!dir.equals(last.from.getOpposite()))
                {
                    Location<World> rel = last.loc.getRelative(dir);
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
                        if (rel.getRelative(dir).get(DuctData.class).map(d -> d.get(dir.getOpposite()).isPresent()).orElse(false))
                        {
                            network.exitPoints.put(rel, rel.getRelative(dir).get(DuctData.class).get());
                        }
                    }
                    // Storage Chest
                    if (last.storage)
                    {
                        rel.getTileEntity().ifPresent(te -> {
                            if (te instanceof Carrier) {
                                network.storage.add(rel);
                            }
                        });
                    }
                }

            }

            if (map.size() > 1)
            {
                for (Map.Entry<Direction, Location<World>> entry : map.entrySet())
                {
                    findNetwork(new LastDuct(entry.getValue(), entry.getKey()), network, depth + 1);
                }
            }
            else if (map.size() == 1)
            {
                for (Map.Entry<Direction, Location<World>> entry : map.entrySet())
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

    public void playEffect(Location<World> loc)
    {
        ParticleEffect badEffect = ParticleEffect.builder().type(ParticleTypes.REDSTONE_DUST).build();
        ParticleEffect goodEffect = ParticleEffect.builder().type(ParticleTypes.REDSTONE_DUST).option(ParticleOptions.COLOR, Color.GREEN).build();
        ParticleEffect neutralEffect = ParticleEffect.builder().type(ParticleTypes.REDSTONE_DUST).option(ParticleOptions.COLOR, Color.YELLOW).build();
        ParticleEffect smoke = ParticleEffect.builder().type(ParticleTypes.LARGE_SMOKE).build();
        Vector3d center = loc.getPosition().add(0.5, 0.5, 0.5);
        for (Direction effectDir : Direction.values())
        {
            if (effectDir.isCardinal() || effectDir.isUpright())
            {
                loc.getExtent().spawnParticles(goodEffect, center.add(effectDir.asOffset().div(1.9)));
            }
        }
        loc.getExtent().playSound(SoundTypes.BLOCK_DISPENSER_DISPENSE, loc.getPosition(), 1);

        Network network = findNetwork(loc);
        //System.out.print("Network: Pipes " +  network.pipes.size() + " Exits " + network.exitPoints.size() + " Storage " + network.storage.size() + "\n");

        for (Location<World> pipe : network.pipes)
        {
            Vector3d pos = pipe.getPosition().add(0.5, 0.5, 0.5);
            if (pipe.getBlockType().equals(QUARTZ_BLOCK))
            {
                for (Direction effectDir : Direction.values())
                {
                    if (effectDir.isCardinal() || effectDir.isUpright())
                    {
                        pipe.getExtent().spawnParticles(network.errors.isEmpty() ? goodEffect : neutralEffect, pos.add(effectDir.asOffset().div(1.9)));
                    }
                }
            }
            else
            {
                pipe.getExtent().spawnParticles(network.errors.isEmpty() ? goodEffect : neutralEffect, pos);
            }
            if (network.exitPoints.isEmpty() && network.storage.isEmpty())
            {
                pipe.getExtent().spawnParticles(smoke, pos);
            }
        }

        for (Location<World> error : network.errors)
        {
            error.getExtent().spawnParticles(badEffect, error.getPosition().add(0.5,0.5,0.5));
            error.getExtent().spawnParticles(smoke, error.getPosition().add(0.5,0.5,0.5));
        }

        for (Location<World> exit : network.exitPoints.keySet())
        {
            center = exit.getPosition().add(0.5,0.5,0.5);
            for (Direction effectDir : Direction.values())
            {
                if (effectDir.isCardinal() || effectDir.isUpright())
                {
                    exit.getExtent().spawnParticles(goodEffect, center.add(effectDir.asOffset().div(1.9)));
                }
            }
            // exit.getExtent().playSound(SoundTypes.BLOCK_DISPENSER_DISPENSE, exit.getPosition(), 1);
        }
    }

    private class LastDuct
    {
        public Location<World> loc;
        public DyeColor color;
        public boolean cross;
        public Direction from;

        public boolean storage;

        public LastDuct(Location<World> loc, Direction from)
        {
            this.update(loc, from);
        }

        public void update(Location<World> loc, Direction from)
        {
            this.from = from;
            this.loc = loc;
            this.color = loc.get(Keys.DYE_COLOR).orElse(null);
            this.cross = GLASS_PANE.equals(loc.getBlockType()) || STAINED_GLASS_PANE.equals(loc.getBlockType());
            this.storage = loc.getBlockType().equals(QUARTZ_BLOCK);
        }

        public boolean isCompatible(Location<World> rel)
        {
            if (!pipeTypes.contains(rel.getBlockType()))
            {
                return false;
            }
            if (storage)
            {
                return rel.getBlockType().equals(QUARTZ_BLOCK);
            }
            if (rel.getBlockType().equals(QUARTZ_BLOCK))
            {
                return false;
            }
            DyeColor color = rel.get(Keys.DYE_COLOR).orElse(null);
            return color == null || color == this.color || this.color == null;
        }

    }
}
