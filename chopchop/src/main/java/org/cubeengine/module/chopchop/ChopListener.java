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
package org.cubeengine.module.chopchop;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.meta.ItemEnchantment;
import org.spongepowered.api.data.type.TreeType;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static java.util.Collections.emptyList;
import static org.spongepowered.api.block.BlockTypes.*;
import static org.spongepowered.api.block.BlockTypes.DIRT;
import static org.spongepowered.api.block.BlockTypes.GRASS;
import static org.spongepowered.api.block.BlockTypes.LEAVES;
import static org.spongepowered.api.block.BlockTypes.LEAVES2;
import static org.spongepowered.api.block.BlockTypes.LOG2;
import static org.spongepowered.api.data.key.Keys.REPRESENTED_ITEM;
import static org.spongepowered.api.data.key.Keys.TREE_TYPE;
import static org.spongepowered.api.data.type.TreeTypes.*;
import static org.spongepowered.api.entity.EntityTypes.ITEM;
import static org.spongepowered.api.item.Enchantments.PUNCH;
import static org.spongepowered.api.item.ItemTypes.*;
import static org.spongepowered.api.item.ItemTypes.LOG;
import static org.spongepowered.api.item.ItemTypes.SAPLING;
import static org.spongepowered.api.util.Direction.*;


public class ChopListener
{
    private static final Set<Direction> dir8 = EnumSet.of(NORTH, NORTHEAST, EAST, SOUTHEAST, SOUTH, SOUTHWEST, WEST, NORTHWEST);

    public static boolean isLeaf(Location<World> block, TreeType species)
    {
        BlockType type = block.getBlockType();
        return !(type != LEAVES && type != LEAVES2) && block.get(TREE_TYPE).get() == species;
    }

    private static boolean isLog(Location<World> block, TreeType species)
    {
        BlockType type = block.getBlockType();
        return !(type != LOG && type != LOG2) && block.get(TREE_TYPE).get() == species;
    }

    @Listener
    public void onChop(final ChangeBlockEvent.Break event, @First Player player)
    {
        if (!player.getItemInHand().isPresent())
        {
            return;
        }
        ItemStack axe = player.getItemInHand().orElse(null);
        if (axe == null || axe.getItem() != DIAMOND_AXE || axe.get(Keys.ITEM_DURABILITY).orElse(0) <= 0 ||
           !axe.get(Keys.ITEM_ENCHANTMENTS).orElse(emptyList()).contains(new ItemEnchantment(PUNCH, 5)))
        {
            return;
        }
        for (Transaction<BlockSnapshot> transaction : event.getTransactions())
        {
            BlockType type = transaction.getOriginal().getState().getType();
            Location<World> orig = transaction.getOriginal().getLocation().get();
            BlockType belowType = orig.getRelative(DOWN).getBlockType();
            if (isLog(type) && (belowType == DIRT || belowType == GRASS))
            {
                TreeType treeType = transaction.getOriginal().get(TREE_TYPE).get();
                Set<Location<World>> treeBlocks = findTreeBlocks(orig, treeType);
                if (treeBlocks.isEmpty())
                {
                    return;
                }

                event.setCancelled(true);
                int logs = 0;
                int leaves = 0;
                Set<Location> saplings = new HashSet<>();
                for (Location<World> block : treeBlocks)
                {
                    if (isLog(block.getBlockType()))
                    {
                        if (!block.equals(orig))
                        {
                            block.getExtent().playSound(SoundTypes.STEP_WOOD, block.getPosition(), 1);
                        }
                        logs++;
                        block.setBlockType(AIR);
                        BlockType belowTyp = block.getRelative(DOWN).getBlockType();
                        if (belowTyp == DIRT || belowTyp == GRASS)
                        {
                            saplings.add(block);
                        }
                    }
                    if (block.getBlockType() == LEAVES || block.getBlockType() == LEAVES2)
                    {
                        block.setBlockType(AIR);
                        block.getExtent().playSound(SoundTypes.STEP_GRASS, block.getPosition(), 1); // TODO leaves sound?
                        leaves++;
                    }
                }

                ItemStack log = ItemStack.builder().itemType(LOG).quantity(logs).build();
                log.offer(TREE_TYPE, treeType);

                int apples = 0;
                if (treeType == JUNGLE)
                {
                    leaves = leaves / 40;
                }
                else
                {
                    if (treeType == DARK_OAK || treeType == OAK)
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
                        block.setBlockType(BlockTypes.SAPLING);
                        block.offer(TREE_TYPE, treeType);
                        leaves--;
                    }
                }


                final int uses = axe.get(Keys.ITEM_DURABILITY).get() - logs;
                axe.offer(Keys.ITEM_DURABILITY, uses);
                player.setItemInHand(axe);

                ItemStack sap = ItemStack.builder().itemType(SAPLING).quantity(leaves).build();
                ItemStack apple = ItemStack.builder().itemType(APPLE).quantity(apples).build();

                World world = player.getWorld();
                Entity itemEntity = world.createEntity(ITEM, orig.getPosition()).get();
                itemEntity.offer(REPRESENTED_ITEM, sap.createSnapshot());
                Cause playerCause = Cause.of(NamedCause.source(player));
                world.spawnEntity(itemEntity, playerCause);

                itemEntity = world.createEntity(ITEM, orig.getPosition()).get();
                itemEntity.offer(REPRESENTED_ITEM, log.createSnapshot());
                world.spawnEntity(itemEntity, playerCause);

                itemEntity = world.createEntity(ITEM, orig.getPosition()).get();
                itemEntity.offer(REPRESENTED_ITEM, apple.createSnapshot());
                world.spawnEntity(itemEntity, playerCause);

                return;
            }
        }
    }

    private boolean isLog(BlockType type)
    {
        return type == LOG || type == LOG2;
    }

    private Set<Location<World>> findTreeBlocks(Location<World> block, TreeType species)
    {
        HashSet<Location<World>> blocks = new HashSet<>();
        Set<Location<World>> logs = new HashSet<>();
        Set<Location<World>> leaves = new HashSet<>();

        logs.add(block);
        findTrunk(block, block, species, logs);
        findLeaves(logs, leaves, species);

        if (leaves.isEmpty())
        {
            return blocks;
        }

        blocks.addAll(logs);
        blocks.addAll(leaves);

        return blocks;
    }

    private void findLeaves(Set<Location<World>> logs, Set<Location<World>> finalLeaves, TreeType species)
    {
        Set<Location<World>> leaves = new HashSet<>();
        for (Location<World> log : logs)
        {
            for (int x = -4; x <= 4; x++)
                for (int y = -4; y <= 4; y++)
                    for (int z = -4; z <= 4; z++)
                    {
                        Location<World> relative = log.add(x, y, z);
                        if (isLeaf(relative, species))
                        {
                            leaves.add(relative);
                        }
                    }
        }
        Set<Location<World>> lastLayer = new HashSet<>(logs);
        do
        {
            Set<Location<World>> curLayer = lastLayer;
            lastLayer = new HashSet<>();
            for (Location layer : curLayer)
            {
                Vector3i layerPos = layer.getBlockPosition();
                for (Location<World> leaf : leaves)
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

    private void findTrunk(Location<World> root, Location<World> base, TreeType species, Set<Location<World>> trunk)
    {
        Set<Location<World>> blocks = new HashSet<>();
        for (Direction face : dir8)
        {
            Location<World> relative = base.getRelative(face);
            if (!trunk.contains(relative) && isLog(relative, species))
            {
                relative = relative.add(0, root.getY(), 0);
                if (root.getBlockPosition().distanceSquared(relative.getBlockPosition()) <= 25)
                {
                    blocks.add(relative);
                }
            }
        }

        Location<World> up = base.getRelative(UP);
        if (!trunk.contains(up) && isLog(up, species))
        {
            blocks.add(up);
        }

        for (Direction face : dir8)
        {
            Location<World> relative = up.getRelative(face);
            if (!trunk.contains(relative) && isLog(relative, species))
            {
                relative = relative.add(0, root.getY(), 0);
                if (root.getBlockPosition().distanceSquared(relative.getBlockPosition()) <= 256)
                {
                    blocks.add(relative);
                }
            }
        }

        trunk.addAll(blocks);
        for (Location<World> block : blocks)
        {
            findTrunk(root, block, species, trunk);
        }
    }

}
