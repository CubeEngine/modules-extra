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
package org.cubeengine.module.chopchop;

import static java.util.Collections.emptyList;
import static org.spongepowered.api.block.BlockTypes.DIRT;
import static org.spongepowered.api.block.BlockTypes.GRASS;
import static org.spongepowered.api.block.BlockTypes.LEAVES;
import static org.spongepowered.api.block.BlockTypes.LEAVES2;
import static org.spongepowered.api.data.key.Keys.REPRESENTED_ITEM;
import static org.spongepowered.api.data.key.Keys.TREE_TYPE;
import static org.spongepowered.api.data.type.TreeTypes.DARK_OAK;
import static org.spongepowered.api.data.type.TreeTypes.JUNGLE;
import static org.spongepowered.api.data.type.TreeTypes.OAK;
import static org.spongepowered.api.entity.EntityTypes.ITEM;
import static org.spongepowered.api.item.ItemTypes.APPLE;
import static org.spongepowered.api.item.ItemTypes.DIAMOND_AXE;
import static org.spongepowered.api.util.Direction.DOWN;
import static org.spongepowered.api.util.Direction.EAST;
import static org.spongepowered.api.util.Direction.NORTH;
import static org.spongepowered.api.util.Direction.NORTHEAST;
import static org.spongepowered.api.util.Direction.NORTHWEST;
import static org.spongepowered.api.util.Direction.SOUTH;
import static org.spongepowered.api.util.Direction.SOUTHEAST;
import static org.spongepowered.api.util.Direction.SOUTHWEST;
import static org.spongepowered.api.util.Direction.UP;
import static org.spongepowered.api.util.Direction.WEST;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.data.type.TreeType;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;


public class ChopListener
{
    private static final Set<Direction> dir8 = EnumSet.of(NORTH, NORTHEAST, EAST, SOUTHEAST, SOUTH, SOUTHWEST, WEST, NORTHWEST);
    private Chopchop module;

    @Inject
    public ChopListener(Chopchop module)
    {
        this.module = module;
    }

    public static boolean isLeaf(Location<World> block, TreeType species)
    {
        BlockType type = block.getBlockType();
        return !(type != LEAVES && type != LEAVES2) && block.get(TREE_TYPE).get() == species;
    }

    private static boolean isLog(Location<World> block, TreeType species)
    {
        BlockType type = block.getBlockType();
        return !(type != BlockTypes.LOG && type != BlockTypes.LOG2) && block.get(TREE_TYPE).get() == species;
    }

    @Listener
    public void onChop(final ChangeBlockEvent.Break event, @First Player player)
    {
        if (!player.getItemInHand(HandTypes.MAIN_HAND).isPresent())
        {
            return;
        }
        ItemStack axe = player.getItemInHand(HandTypes.MAIN_HAND).orElse(null);
        if (axe == null || axe.getType() != DIAMOND_AXE || axe.get(Keys.ITEM_DURABILITY).orElse(0) <= 0 ||
           !axe.get(Keys.ITEM_ENCHANTMENTS).orElse(emptyList()).contains(Enchantment.builder().type(EnchantmentTypes.PUNCH).level(5).build()))
        {
            return;
        }
        if (!module.use.check(player))
        {
            return;
        }
        Sponge.getCauseStackManager().addContext(EventContextKeys.PLAYER_SIMULATED, player.getProfile());
        for (Transaction<BlockSnapshot> transaction : event.getTransactions())
        {
            BlockType type = transaction.getOriginal().getState().getType();
            Location<World> orig = transaction.getOriginal().getLocation().get();
            BlockType belowType = orig.getBlockRelative(DOWN).getBlockType();
            if (isLog(type) && (belowType == DIRT || belowType == GRASS))
            {
                TreeType treeType = transaction.getOriginal().get(TREE_TYPE).get();
                Set<Location<World>> treeBlocks = findTreeBlocks(orig, treeType);
                if (treeBlocks.isEmpty())
                {
                    return;
                }

                int logs = 0;
                int leaves = 0;
                Set<Location> saplings = new HashSet<>();
                for (Location<World> block : treeBlocks)
                {
                    if (isLog(block.getBlockType()))
                    {
                        if (!block.equals(orig))
                        {
                            block.getExtent().playSound(SoundTypes.BLOCK_WOOD_STEP, block.getPosition(), 1);
                        }
                        logs++;
                        block.setBlockType(BlockTypes.AIR);
                        BlockType belowTyp = block.getBlockRelative(DOWN).getBlockType();
                        if (belowTyp == DIRT || belowTyp == GRASS)
                        {
                            saplings.add(block);
                        }
                    }
                    if (block.getBlockType() == LEAVES || block.getBlockType() == LEAVES2)
                    {
                        block.setBlockType(BlockTypes.AIR);
                        block.getExtent().playSound(SoundTypes.BLOCK_GRASS_STEP, block.getPosition(), 1); // TODO leaves sound?
                        leaves++;
                    }
                }

                ItemStack log = ItemStack.builder().itemType(type.getItem().get()).quantity(logs).build();
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

                BlockState sapState = BlockTypes.SAPLING.getDefaultState().with(TREE_TYPE, treeType).get();
                if (this.module.autoplant.check(player))
                {
                    leaves -= saplings.size();
                    leaves = Math.max(0, leaves);
                    transaction.setCustom(sapState.snapshotFor(transaction.getOriginal().getLocation().get()));
                    saplings.forEach(l -> l.setBlock(sapState));
                }

                final int uses = axe.get(Keys.ITEM_DURABILITY).get() - logs;
                axe.offer(Keys.ITEM_DURABILITY, uses);
                player.setItemInHand(HandTypes.MAIN_HAND, axe);

                World world = player.getWorld();
                Entity itemEntity;
                Sponge.getCauseStackManager().removeContext(EventContextKeys.PLAYER_SIMULATED);
                Sponge.getCauseStackManager().pushCause(player);
                if (apples > 0)
                {
                    ItemStack apple = ItemStack.builder().itemType(APPLE).quantity(apples).build();
                    itemEntity = world.createEntity(ITEM, orig.getPosition());
                    itemEntity.offer(REPRESENTED_ITEM, apple.createSnapshot());
                    world.spawnEntity(itemEntity);
                }

                if (leaves > 0)
                {
                    ItemStack sap = ItemStack.builder().fromBlockState(sapState).build();
                    sap.setQuantity(leaves);
                    itemEntity = world.createEntity(ITEM, orig.getPosition());
                    itemEntity.offer(REPRESENTED_ITEM, sap.createSnapshot());
                    world.spawnEntity(itemEntity);
                }

                itemEntity = world.createEntity(ITEM, orig.getPosition());
                itemEntity.offer(REPRESENTED_ITEM, log.createSnapshot());
                world.spawnEntity(itemEntity);
                return;
            }
        }
    }

    private boolean isLog(BlockType type)
    {
        return BlockTypes.LOG.equals(type) || BlockTypes.LOG2.equals(type);
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
            Location<World> relative = base.getBlockRelative(face);
            if (!trunk.contains(relative) && isLog(relative, species))
            {
                if (base.getBlockPosition().distanceSquared(relative.getBlockPosition()) <= 25)
                {
                    blocks.add(relative);
                }
            }
        }

        Location<World> up = base.getBlockRelative(UP);
        if (!trunk.contains(up) && isLog(up, species))
        {
            blocks.add(up);
        }

        for (Direction face : dir8)
        {
            Location<World> relative = up.getBlockRelative(face);
            if (!trunk.contains(relative) && isLog(relative, species))
            {
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
