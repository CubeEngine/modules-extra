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

import com.google.inject.Inject;
import net.kyori.adventure.sound.Sound;
import org.cubeengine.libcube.util.ItemUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.data.type.WoodType;
import org.spongepowered.api.data.type.WoodTypes;
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
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.math.vector.Vector3i;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChopListener
{
    private static final Set<Direction> dir8 = EnumSet.of(NORTH, NORTHEAST, EAST, SOUTHEAST, SOUTH, SOUTHWEST, WEST, NORTHWEST);
    private Chopchop module;
    private Map<WoodType, BlockType> leafTypes = new HashMap<>();

    @Inject
    public ChopListener(Chopchop module)
    {
        this.module = module;
    }

    private boolean isLeaf(ServerLocation block, WoodType species)
    {
        BlockType type = block.getBlockType();
        boolean match = module.getConfig().leafTypes.contains(type);
        final WoodType woodType = block.getBlock().get(Keys.WOOD_TYPE).orElse(null);
        return match && woodType == species;
    }

    private boolean isLog(ServerLocation block, WoodType species)
    {
        BlockType type = block.getBlockType();
        boolean match = module.getConfig().logTypes.contains(type);
        final WoodType woodType = block.getBlock().get(Keys.WOOD_TYPE).orElse(null);
        return match && woodType == species;
    }

    private boolean isSoil(BlockType belowType) {
        return module.getConfig().soilTypes.contains(belowType);
    }

    @Listener
    public void onChop(final ChangeBlockEvent.Break event, @First ServerPlayer player)
    {
        if (player.getItemInHand(HandTypes.MAIN_HAND).isEmpty() || event.getCause().getContext().containsKey(EventContextKeys.SIMULATED_PLAYER))
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
        for (Transaction<BlockSnapshot> transaction : event.getTransactions())
        {
            BlockType type = transaction.getOriginal().getState().getType();
            ServerLocation orig = transaction.getOriginal().getLocation().get();
            BlockType belowType = orig.add(DOWN.asBlockOffset()).getBlockType();
            if (isLog(type) && isSoil(belowType))
            {
                WoodType treeType = transaction.getOriginal().getState().get(Keys.WOOD_TYPE).get();
                Set<ServerLocation> treeBlocks = findTreeBlocks(orig, treeType);
                if (treeBlocks.isEmpty())
                {
                    return;
                }

                int logs = 0;
                int leaves = 0;
                Set<Location> saplings = new HashSet<>();
                for (ServerLocation block : treeBlocks)
                {
                    if (isLog(block.getBlockType()))
                    {
                        if (!block.equals(orig))
                        {
                            block.getWorld().playSound(Sound.sound(SoundTypes.BLOCK_WOOD_STEP, Sound.Source.NEUTRAL, 1, 0), block.getPosition());
                        }
                        logs++;
                        block.setBlockType(BlockTypes.AIR.get());
                        BlockType belowTyp = block.add(DOWN.asBlockOffset()).getBlockType();
                        if (isSoil(belowTyp))
                        {
                            saplings.add(block);
                        }
                    }
                    if (this.module.getConfig().leafTypes.contains(block.getBlockType()))
                    {
                        block.setBlockType(BlockTypes.AIR.get());
                        block.getWorld().playSound(Sound.sound(SoundTypes.BLOCK_GRASS_STEP, Sound.Source.NEUTRAL, 1, 0), block.getPosition()); // TODO leaves sound?
                        leaves++;
                    }
                }

                ItemStack log = ItemStack.builder().itemType(type.getItem().get()).quantity(logs).build();
//                log.offer(TREE_TYPE, treeType);

                int apples = 0;
                if (treeType == WoodTypes.JUNGLE)
                {
                    leaves = leaves / 40;
                }
                else
                {
                    if (treeType == WoodTypes.DARK_OAK || treeType == WoodTypes.OAK)
                    {
                        apples = leaves / 200;
                    }
                    leaves = leaves / 20;
                }
                if (leaves == 0)
                {
                    leaves = 1;
                }

                final BlockType saplingType = this.leafTypes.get(treeType);
                if (saplingType != null) {
                    final BlockState sapState = saplingType.getDefaultState();
                    if (this.module.autoplantPerm.check(player))
                    {
                        leaves -= saplings.size();
                        leaves = Math.max(0, leaves);
                        transaction.setCustom(sapState.snapshotFor(transaction.getOriginal().getLocation().get()));
                        saplings.forEach(l -> l.setBlock(sapState));
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

    private boolean isLog(BlockType type)
    {
        return this.module.getConfig().logTypes.contains(type);
    }

    private Set<ServerLocation> findTreeBlocks(ServerLocation block, WoodType species)
    {
        HashSet<ServerLocation> blocks = new HashSet<>();
        Set<ServerLocation> logs = new HashSet<>();
        Set<ServerLocation> leaves = new HashSet<>();

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

    private void findLeaves(Set<ServerLocation> logs, Set<ServerLocation> finalLeaves, WoodType species)
    {
        Set<ServerLocation> leaves = new HashSet<>();
        for (ServerLocation log : logs)
        {
            for (int x = -4; x <= 4; x++)
                for (int y = -4; y <= 4; y++)
                    for (int z = -4; z <= 4; z++)
                    {
                        ServerLocation relative = log.add(x, y, z);
                        if (isLeaf(relative, species))
                        {
                            leaves.add(relative);
                        }
                    }
        }
        Set<ServerLocation> lastLayer = new HashSet<>(logs);
        do
        {
            Set<ServerLocation> curLayer = lastLayer;
            lastLayer = new HashSet<>();
            for (Location layer : curLayer)
            {
                Vector3i layerPos = layer.getBlockPosition();
                for (ServerLocation leaf : leaves)
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

    private void findTrunk(ServerLocation root, ServerLocation base, WoodType species, Set<ServerLocation> trunk)
    {
        Set<ServerLocation> blocks = new HashSet<>();
        for (Direction face : dir8)
        {
            ServerLocation relative = base.add(face.asBlockOffset());
            if (!trunk.contains(relative) && isLog(relative, species))
            {
                if (base.getBlockPosition().distanceSquared(relative.getBlockPosition()) <= 25)
                {
                    blocks.add(relative);
                }
            }
        }

        ServerLocation up = base.add(UP.asBlockOffset());
        if (!trunk.contains(up) && isLog(up, species))
        {
            blocks.add(up);
        }

        for (Direction face : dir8)
        {
            ServerLocation relative = up.add(face.asBlockOffset());
            if (!trunk.contains(relative) && isLog(relative, species))
            {
                if (root.getBlockPosition().distanceSquared(relative.getBlockPosition()) <= 256)
                {
                    blocks.add(relative);
                }
            }
        }

        trunk.addAll(blocks);
        for (ServerLocation block : blocks)
        {
            findTrunk(root, block, species, trunk);
        }
    }

    public void init() {
        Sponge.getRegistry().getCatalogRegistry().getAllOf(BlockType.class).stream().filter(t -> t != BlockTypes.AIR.get()).forEach(blockType -> {
            if (module.getConfig().saplingTypes.contains(blockType)) {
                blockType.getDefaultState().get(Keys.WOOD_TYPE).ifPresent(woodType -> this.leafTypes.put(woodType, blockType));
            }
        });

    }
}
