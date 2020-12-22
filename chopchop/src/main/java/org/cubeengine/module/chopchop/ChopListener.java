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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import com.google.inject.Inject;
import net.kyori.adventure.sound.Sound;
import org.cubeengine.libcube.util.ItemUtil;
import org.cubeengine.module.chopchop.ChopchopConfig.Tree;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.transaction.Operations;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;

import static java.util.Collections.emptyList;
import static org.spongepowered.api.item.ItemTypes.APPLE;
import static org.spongepowered.api.item.ItemTypes.DIAMOND_AXE;
import static org.spongepowered.api.util.Direction.*;

public class ChopListener
{
    private static final Set<Direction> dir8 = EnumSet.of(NORTH, NORTHEAST, EAST, SOUTHEAST, SOUTH, SOUTHWEST, WEST, NORTHWEST);
    private Chopchop module;

    @Inject
    public ChopListener(Chopchop module)
    {
        this.module = module;
    }

    private boolean isLeaf(ServerWorld world, Vector3i pos, Tree species)
    {
        final BlockState state = world.getBlock(pos);
        final BlockType type = state.getType();
        return species.leafType == type;
    }

    private boolean isLog(ServerWorld world, Vector3i pos, Tree species)
    {
        final BlockState state = world.getBlock(pos);
        final BlockType type = state.getType();
        return species.logType == type;
    }

    private boolean isSoil(BlockType belowType) {
        return module.getConfig().soilTypes.contains(belowType);
    }

    @Listener
    public void onChop(final ChangeBlockEvent.All event, @First ServerPlayer player)
    {

        if (event.getTransactions(Operations.BREAK.get()).count() != 1 ||
            player.getItemInHand(HandTypes.MAIN_HAND).isEmpty() ||
            event.getCause().getContext().containsKey(EventContextKeys.SIMULATED_PLAYER))
        {
            return;
        }
        ItemStack axe = player.getItemInHand(HandTypes.MAIN_HAND);
        if (axe == null || !axe.getType().isAnyOf(DIAMOND_AXE) || axe.get(Keys.ITEM_DURABILITY).orElse(0) <= 0 ||
           !axe.get(Keys.APPLIED_ENCHANTMENTS).orElse(emptyList()).contains(Enchantment.builder().type(EnchantmentTypes.PUNCH).level(5).build()))
        {
            return;
        }
        if (!module.usePerm.check(player))
        {
            return;
        }
        Sponge.getServer().getCauseStackManager().addContext(EventContextKeys.SIMULATED_PLAYER, player.getProfile());
        int leafSounds = 0;
        int logSounds = 0;

        for (Transaction<BlockSnapshot> transaction : event.getTransactions())
        {
            BlockType type = transaction.getOriginal().getState().getType();
            ServerLocation orig = transaction.getOriginal().getLocation().get();
            ServerWorld world = orig.getWorld();
            BlockType belowType = orig.relativeTo(DOWN).getBlockType();
            if (isLog(type) && isSoil(belowType))
            {
                final Tree treeType = getTreeType(type);
                Set<Vector3i> treeBlocks = findTreeBlocks(world, orig.getBlockPosition(), treeType);
                if (treeBlocks.isEmpty())
                {
                    return;
                }

                int logs = 0;
                int leaves = 0;
                Set<Vector3i> saplings = new HashSet<>();
                for (Vector3i pos : treeBlocks)
                {
                    if (isLog(world.getBlock(pos).getType()))
                    {
                        if (!pos.equals(orig.getBlockPosition()))
                        {
                            logSounds++;
                            if (logSounds > 5) {
                                world.playSound(Sound.sound(SoundTypes.BLOCK_WOOD_BREAK, Sound.Source.NEUTRAL, 0.5f, 0.8f), pos.toDouble());
                            }
                        }
                        logs++;
                        world.removeBlock(pos);
                        BlockType belowTyp = world.getBlock(pos.add(DOWN.asBlockOffset())).getType();
                        if (isSoil(belowTyp))
                        {
                            saplings.add(pos);
                        }
                    }
                    if (isLeaf(world, pos, treeType))
                    {
                        world.removeBlock(pos);
                        leafSounds++;
                        if (leafSounds > 3) {
                            world.playSound(Sound.sound(SoundTypes.BLOCK_GRASS_BREAK, Sound.Source.NEUTRAL, 0.5f, 0.8f), pos.toDouble()); // TODO leaves sound?
                        }
                        leaves++;
                    }
                }

                ItemStack log = ItemStack.builder().itemType(type.getItem().get()).quantity(logs).build();

                int apples = 0;
                if (treeType.leafType == BlockTypes.JUNGLE_LEAVES.get())
                {
                    leaves = leaves / 40;
                }
                else
                {
                    if (treeType.leafType == BlockTypes.DARK_OAK_LEAVES.get() || treeType.leafType == BlockTypes.OAK_LEAVES.get())
                    {
                        apples = leaves / 200;
                    }
                    leaves = leaves / 20;
                }
                if (leaves == 0)
                {
                    leaves = 1;
                }

                final BlockType saplingType = treeType.saplingType;
                if (saplingType != null)
                {
                    final BlockState sapState = saplingType.getDefaultState();
                    if (this.module.autoplantPerm.check(player))
                    {
                        leaves -= saplings.size();
                        leaves = Math.max(0, leaves);
                        transaction.setCustom(sapState.snapshotFor(transaction.getOriginal().getLocation().get()));
                        saplings.forEach(p -> world.setBlock(p, sapState));
                    }
                }

                final int uses = axe.get(Keys.ITEM_DURABILITY).get() - logs;
                axe.offer(Keys.ITEM_DURABILITY, uses);
                player.setItemInHand(HandTypes.MAIN_HAND, axe);

                Sponge.getServer().getCauseStackManager().removeContext(EventContextKeys.SIMULATED_PLAYER);
                Sponge.getServer().getCauseStackManager().pushCause(player);
                if (apples > 0)
                {
                    ItemStack apple = ItemStack.builder().itemType(APPLE).quantity(apples).build();
                    ItemUtil.spawnItem(orig, apple);
                }

                if (leaves > 0)
                {
                    if (saplingType != null && saplingType.getItem().isPresent()) {
                        ItemUtil.spawnItem(orig, ItemStack.of(saplingType.getItem().get(), leaves));
                    }
                }

                ItemUtil.spawnItem(orig, log);
                return;
            }
        }
    }

    private Tree getTreeType(BlockType logType)
    {
        for (Tree tree : this.module.getConfig().trees)
        {
            if (tree.logType == logType)
            {
                return tree;
            }
        }
        return null;
    }

    private boolean isLog(BlockType type)
    {
        return this.module.getConfig().trees.stream().anyMatch(tree -> tree.logType == type);
    }

    private Set<Vector3i> findTreeBlocks(ServerWorld world, Vector3i pos, Tree species)
    {
        Set<Vector3i> blocks = new HashSet<>();
        Set<Vector3i> logs = new HashSet<>();
        Set<Vector3i> leaves = new HashSet<>();

        logs.add(pos);
        findTrunk(world, pos, pos, species, logs);
        findLeaves(world, logs, leaves, species);

        if (leaves.isEmpty())
        {
            return blocks;
        }

        blocks.addAll(logs);
        blocks.addAll(leaves);

        return blocks;
    }

    private void findLeaves(ServerWorld world, Set<Vector3i> logs, Set<Vector3i> finalLeaves, Tree species)
    {
        Set<Vector3i> leaves = new HashSet<>();
        for (Vector3i log : logs)
        {
            for (int x = -4; x <= 4; x++)
                for (int y = -4; y <= 4; y++)
                    for (int z = -4; z <= 4; z++)
                    {
                        Vector3i relative = log.add(x, y, z);
                        if (isLeaf(world, relative, species))
                        {
                            leaves.add(relative);
                        }
                    }
        }
        Set<Vector3i> lastLayer = new HashSet<>(logs);
        do
        {
            Set<Vector3i> curLayer = lastLayer;
            lastLayer = new HashSet<>();
            for (Vector3i layer : curLayer)
            {
                for (Vector3i leaf : leaves)
                {
                    Vector3i diff = layer.sub(leaf).abs();
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

    private void findTrunk(ServerWorld world, Vector3i root, Vector3i base, Tree species, Set<Vector3i> trunk)
    {
        Set<Vector3i> blocks = new HashSet<>();
        for (Direction face : dir8)
        {
            Vector3i relative = base.add(face.asBlockOffset());
            if (!trunk.contains(relative) && isLog(world, relative, species))
            {
                if (base.distanceSquared(relative) <= 25)
                {
                    blocks.add(relative);
                }
            }
        }

        Vector3i up = base.add(UP.asBlockOffset());
        if (!trunk.contains(up) && isLog(world, up, species))
        {
            blocks.add(up);
        }

        for (Direction face : dir8)
        {
            Vector3i relative = up.add(face.asBlockOffset());
            if (!trunk.contains(relative) && isLog(world, relative, species))
            {
                if (root.distanceSquared(relative) <= 256)
                {
                    blocks.add(relative);
                }
            }
        }

        trunk.addAll(blocks);
        for (Vector3i block : blocks)
        {
            findTrunk(world, root, block, species, trunk);
        }
    }

}
