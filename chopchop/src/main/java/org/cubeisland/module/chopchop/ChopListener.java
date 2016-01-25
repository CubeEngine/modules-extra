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
package org.cubeisland.module.chopchop;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.base.Optional;
import org.cubeengine.service.task.TaskManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.manipulator.block.TreeData;
import org.spongepowered.api.data.manipulator.item.DurabilityData;
import org.spongepowered.api.data.manipulator.item.EnchantmentData;
import org.spongepowered.api.data.type.TreeType;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.event.block.BlockBreakEvent;
import org.spongepowered.api.event.entity.player.PlayerBreakBlockEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.spongepowered.api.block.BlockTypes.*;
import static org.spongepowered.api.block.BlockTypes.DIRT;
import static org.spongepowered.api.block.BlockTypes.GRASS;
import static org.spongepowered.api.block.BlockTypes.LEAVES;
import static org.spongepowered.api.block.BlockTypes.LEAVES2;
import static org.spongepowered.api.block.BlockTypes.LOG2;
import static org.spongepowered.api.data.type.TreeTypes.*;
import static org.spongepowered.api.entity.EntityTypes.DROPPED_ITEM;
import static org.spongepowered.api.item.Enchantments.PUNCH;
import static org.spongepowered.api.item.ItemTypes.*;
import static org.spongepowered.api.item.ItemTypes.LOG;
import static org.spongepowered.api.item.ItemTypes.SAPLING;
import static org.spongepowered.api.util.Direction.*;


public class ChopListener
{
    private static final Set<Direction> dir8 = EnumSet.of(NORTH, NORTHEAST, EAST, SOUTHEAST, SOUTH, SOUTHWEST, WEST, NORTHWEST);
    private Chopchop module;
    private Game game;
    private TaskManager tm;

    public ChopListener(Chopchop module, Game game, TaskManager tm)
    {
        this.module = module;
        this.game = game;
        this.tm = tm;
    }

    private static TreeType getSpecies(Location block)
    {
        return block.getData(TreeData.class).transform(TreeData::getValue).orNull();
    }

    public static boolean isLeaf(Location block, TreeType species)
    {
        return !(block.getBlockType() != LEAVES && block.getBlockType() != LEAVES2) && getSpecies(block) == species;
    }

    private static boolean isLog(Location block, TreeType species)
    {
        return !(block.getBlockType() != LOG && block.getBlockType() != LOG2) && getSpecies(block) == species;
    }

    @Subscribe
    public void onChop(final PlayerBreakBlockEvent event)
    {
        if (!event.getUser().getItemInHand().isPresent())
        {
            return;
        }
        ItemStack axe = event.getUser().getItemInHand().get();
        Optional<DurabilityData> axeDura = axe.getData(DurabilityData.class);
        if (!isChopChop(event.getBlock(), axe) || axeDura.transform(DurabilityData::getDurability).or(0) <= 0)
        {
            return;
        }
        TreeType species = getSpecies(event.getBlock());
        Set<Location> treeBlocks = findTreeBlocks(event, species);
        if (!treeBlocks.isEmpty())
        {
            event.setCancelled(true);
            int logs = 0;
            int leaves = 0;
            Set<Location> saplings = new HashSet<>();
            for (Location block : treeBlocks)
            {
                if (block.getBlockType() == LOG || block.getBlockType() == LOG2)
                {
                    if (!block.equals(event.getBlock()))
                    {
                        ((World)block.getExtent()).playSound(SoundTypes.STEP_WOOD, block.getPosition(), 1);
                    }
                    logs++;
                    block.setBlockType(AIR);
                    if (block.getRelative(DOWN).getBlockType() == DIRT || block.getRelative(DOWN).getBlockType() == GRASS)
                    {
                        saplings.add(block);
                    }
                }
                if (block.getBlockType() == LEAVES || block.getBlockType() == LEAVES2)
                {
                    block.setBlockType(AIR);
                    ((World)block.getExtent()).playSound(SoundTypes.STEP_GRASS, block.getPosition(), 1); // TODO leaves sound?
                    leaves++;
                }
            }

            ItemStack log = game.getRegistry().getItemBuilder().itemType(LOG).quantity(logs).build();
            log.offer(log.getOrCreate(TreeData.class).get().setValue(species));

            int apples = 0;
            if (species == JUNGLE)
            {
                leaves = leaves / 40;
            }
            else
            {
                if (species == DARK_OAK || species == OAK)
                {
                    apples = leaves / 200;
                }
                leaves = leaves / 20;
            }
            if (leaves == 0)
            {
                leaves = 1;
            }

            for (Location block : saplings)
            {
                if (leaves > 0)
                {
                    block.setBlockType(SAPLING.getBlock());
                    block.offer(block.getOrCreate(TreeData.class).get().setValue(species));
                    leaves--;
                }
            }

            final int uses = axeDura.get().getDurability() - logs;
            axeDura.get().setDurability(uses);
            tm.runTaskDelayed(module, () -> {
                // break current axe / if stacked only one!
                axe.offer(axeDura.get()); // TODO check if this works as intended
            }, 1);

            ItemStack sap = game.getRegistry().getItemBuilder().itemType(SAPLING).quantity(leaves).build();
            ItemStack apple = game.getRegistry().getItemBuilder().itemType(APPLE).quantity(apples).build();

            World world = event.getUser().getWorld();
            Item itemEntity = (Item)world.createEntity(DROPPED_ITEM, event.getBlock().getPosition()).get();
            itemEntity.offer(itemEntity.getItemData().setValue(sap));
            world.spawnEntity(itemEntity);

            itemEntity = (Item)world.createEntity(DROPPED_ITEM, event.getBlock().getPosition()).get();
            itemEntity.offer(itemEntity.getItemData().setValue(log));
            world.spawnEntity(itemEntity);

            itemEntity = (Item)world.createEntity(DROPPED_ITEM, event.getBlock().getPosition()).get();
            itemEntity.offer(itemEntity.getItemData().setValue(apple));
            world.spawnEntity(itemEntity);
        }
    }

    private Set<Location> findTreeBlocks(BlockBreakEvent event, TreeType species)
    {
        HashSet<Location> blocks = new HashSet<>();
        Set<Location> logs = new HashSet<>();
        Set<Location> leaves = new HashSet<>();

        logs.add(event.getBlock());
        findTrunk(event.getBlock(), event.getBlock(), species, logs);
        findLeaves(logs, leaves, species);

        if (leaves.isEmpty())
        {
            return blocks;
        }

        blocks.addAll(logs);
        blocks.addAll(leaves);

        return blocks;
    }

    private void findLeaves(Set<Location> logs, Set<Location> finalLeaves, TreeType species)
    {
        Set<Location> leaves = new HashSet<>();
        for (Location log : logs)
        {
            for (int x = -4; x <= 4; x++)
                for (int y = -4; y <= 4; y++)
                    for (int z = -4; z <= 4; z++)
                    {
                        Location relative = log.add(x, y, z);
                        if (isLeaf(relative, species))
                        {
                            leaves.add(relative);
                        }
                    }
        }
        Set<Location> lastLayer = new HashSet<>(logs);
        do
        {
            Set<Location> curLayer = lastLayer;
            lastLayer = new HashSet<>();
            for (Location layer : curLayer)
            {
                Vector3i layerPos = layer.getBlockPosition();
                for (Location leaf : leaves)
                {
                    Vector3i diff = layerPos.sub(leaf.getBlockPosition()).abs();
                    if (diff.getX() + diff.getY() + diff.getZ() == 1 // cardinal or upright
                    || (diff.getX() + diff.getY() == 2 && diff.getX() == diff.getY())) // ordinal
                    {
                        lastLayer.add(leaf);
                    }
                }
                leaves.removeAll(lastLayer);
            }
            finalLeaves.addAll(lastLayer);
        }
        while (!lastLayer.isEmpty());
    }

    private void findTrunk(Location root, Location base, TreeType species, Set<Location> trunk)
    {
        Location loc = new Location(null, 0, 0, 0);
        Set<Location> blocks = new HashSet<>();
        for (Direction face : dir8)
        {
            Location relative = base.getRelative(face);
            if (!trunk.contains(relative) && isLog(relative, species))
            {
                relative = relative.add(0, root.getY(), 0);
                if (root.getBlockPosition().distanceSquared(loc.getBlockPosition()) <= 25)
                {
                    blocks.add(relative);
                }
            }
        }

        Location up = base.getRelative(UP);
        if (!trunk.contains(up) && isLog(up, species))
        {
            blocks.add(up);
        }

        for (Direction face : dir8)
        {
            Location relative = up.getRelative(face);
            if (!trunk.contains(relative) && isLog(relative, species))
            {
                relative = relative.add(0, root.getY(), 0);
                if (root.getBlockPosition().distanceSquared(loc.getBlockPosition()) <= 256)
                {
                    blocks.add(relative);
                }
            }
        }

        trunk.addAll(blocks);
        for (Location block : blocks)
        {
            findTrunk(root, block, species, trunk);
        }
    }

    private boolean isChopChop(Location block, ItemStack item)
    {
        if (item == null)
        {
            return false;
        }
        if (block.getBlockType() == LOG || block.getBlockType() == LOG2)
        {
            if (item.getItem() == DIAMOND_AXE && item.getOrCreate(EnchantmentData.class).get().get(PUNCH).or(0) == 5)
            {
                if (block.getRelative(DOWN).getBlockType() == DIRT || block.getRelative(DOWN).getBlockType() == GRASS)
                {
                    return true;
                }
            }
        }
        return false;
    }
}
