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
package de.cubeisland.engine.module.chopchop;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.TreeSpecies;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Leaves;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Tree;

import static org.bukkit.Effect.STEP_SOUND;
import static org.bukkit.Material.*;
import static org.bukkit.TreeSpecies.*;
import static org.bukkit.block.BlockFace.*;
import static org.bukkit.enchantments.Enchantment.ARROW_KNOCKBACK;

public class ChopListener
{
    private static final Set<BlockFace> dir8 = EnumSet.of(NORTH, NORTH_EAST, EAST, SOUTH_EAST, SOUTH, SOUTH_WEST, WEST,
                                                          NORTH_WEST);
    private Chopchop module;

    public ChopListener(Chopchop module)
    {
        this.module = module;
    }

    private static TreeSpecies getSpecies(Block block)
    {
        MaterialData data = block.getState().getData();
        if (data instanceof Tree)
        {
            return ((Tree)data).getSpecies();
        }
        else if (block.getType() == LOG_2)
        {
            if ((data.getData() & 1) == 1)
            {
                return DARK_OAK;
            }
            else if ((data.getData() & 0) == 0)
            {
                return ACACIA;
            }
        }
        return null;
    }

    public static boolean isLeaf(Block block, TreeSpecies species)
    {
        MaterialData data = block.getState().getData();
        if (block.getType() == LEAVES)
        {
            if ((data.getData() & 4) == 4)
            {
                return false;
            }
            return data instanceof Tree && ((Tree)data).getSpecies() == species
                || data instanceof Leaves && ((Leaves)data).getSpecies() == species;
        }
        else if (block.getType() == LEAVES_2)
        {
            if ((data.getData() & 4) == 4)
            {
                return false;
            }
            if (species == DARK_OAK && (data.getData() & 1) == 1)
            {
                return true;
            }
            else if (species == ACACIA && (data.getData() & 0) == 0)
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isLog(Block block, TreeSpecies species)
    {
        return (block.getType() == LOG || block.getType() == LOG_2) && getSpecies(block) == species;
    }

    @EventHandler
    public void onChop(final BlockBreakEvent event)
    {
        final ItemStack axe = event.getPlayer().getItemInHand();
        if (isChopChop(event.getBlock(), axe))
        {
            if (axe.getDurability() >= 1561)
            {
                return;
            }
            TreeSpecies species = getSpecies(event.getBlock());
            Set<Block> treeBlocks = findTreeBlocks(event, species);
            if (!treeBlocks.isEmpty())
            {
                event.setCancelled(true);
                int logs = 0;
                int leaves = 0;
                Location loc = new Location(null, 0, 0, 0);
                Set<Block> saplings = new HashSet<>();
                for (Block block : treeBlocks)
                {
                    switch (block.getType())
                    {
                        case LOG:
                        case LOG_2:
                            if (!block.equals(event.getBlock()))
                            {
                                block.getLocation(loc).getWorld().playEffect(loc, STEP_SOUND, LOG.getId());
                            }
                            logs++;
                            block.setType(AIR);
                            if (block.getRelative(DOWN).getType() == DIRT || block.getRelative(DOWN).getType() == GRASS)
                            {
                                saplings.add(block);
                            }

                            break;
                        case LEAVES:
                        case LEAVES_2:
                            block.setType(AIR);
                            block.getLocation(loc).getWorld().playEffect(loc, STEP_SOUND, LEAVES.getId());
                            leaves++;
                            break;
                    }
                }

                ItemStack log;
                switch (species)
                {
                    case GENERIC:
                    case REDWOOD:
                    case BIRCH:
                    case JUNGLE:
                        log = new ItemStack(LOG, logs, species.getData());
                        break;
                    case ACACIA:
                        log = new ItemStack(LOG_2, logs, (short)0);
                        break;
                    case DARK_OAK:
                        log = new ItemStack(LOG_2, logs, (short)1);
                        break;
                    default:
                        return;
                }
                Tree sapType = new Tree(SAPLING);
                sapType.setSpecies(species);
                int apples = 0;
                if (species == JUNGLE)
                {
                    leaves = leaves / 40;
                }
                else
                {
                    if (species == DARK_OAK || species == GENERIC)
                    {
                        apples = leaves / 200;
                    }
                    leaves = leaves / 20;
                }
                if (leaves == 0)
                {
                    leaves = 1;
                }

                for (Block block : saplings)
                {
                    if (leaves > 0)
                    {
                        block.setType(SAPLING);
                        block.setData((byte)0);
                        BlockState state = block.getState();
                        Tree data = (Tree)state.getData();
                        data.setSpecies(species);
                        state.setData(data);
                        state.update();
                        leaves--;
                    }
                }

                final int uses = axe.getDurability() + logs;
                module.getCore().getTaskManager().runTaskDelayed(module, new Runnable()
                {
                    @Override
                    public void run()
                    {
                        axe.setDurability((short)(uses));
                        if (uses >= 1561)
                        {
                            if (axe.getAmount() == 1)
                            {
                                axe.setAmount(0);
                                axe.setDurability((short)1561);
                                event.getPlayer().updateInventory();
                            }
                            else
                            {
                                axe.setAmount(axe.getAmount() - 1);
                            }
                        }
                    }
                }, 1);

                ItemStack sap = sapType.toItemStack(leaves);

                event.getBlock().getLocation(loc);
                event.getBlock().getWorld().dropItemNaturally(loc, log);
                if (sap.getAmount() > 0)
                {
                    event.getBlock().getWorld().dropItemNaturally(loc, sap);
                }
                if (apples > 0)
                {
                    event.getBlock().getWorld().dropItemNaturally(loc, new ItemStack(APPLE, apples));
                }
            }
        }
    }

    private Set<Block> findTreeBlocks(BlockBreakEvent event, TreeSpecies species)
    {
        HashSet<Block> blocks = new HashSet<>();
        Set<Block> logs = new HashSet<>();
        Set<Block> leaves = new HashSet<>();

        logs.add(event.getBlock());
        findTrunk(event.getBlock().getLocation(), event.getBlock(), species, logs);
        findLeaves(logs, leaves, species);

        if (leaves.isEmpty())
        {
            return blocks;
        }

        blocks.addAll(logs);
        blocks.addAll(leaves);

        return blocks;
    }

    private void findLeaves(Set<Block> logs, Set<Block> finalLeaves, TreeSpecies species)
    {
        Set<Block> leaves = new HashSet<>();
        for (Block log : logs)
        {
            for (int x = -4; x <= 4; x++)
                for (int y = -4; y <= 4; y++)
                    for (int z = -4; z <= 4; z++)
                    {
                        Block relative = log.getRelative(x, y, z);
                        if (isLeaf(relative, species))
                        {
                            leaves.add(relative);
                        }
                    }
        }
        Set<Block> lastLayer = new HashSet<>(logs);
        do
        {
            Set<Block> curLayer = lastLayer;
            lastLayer = new HashSet<>();
            for (Block layer : curLayer)
            {
                for (Block leaf : leaves)
                {
                    BlockFace face = layer.getFace(leaf);
                    if (dir8.contains(face) || face == UP || face == DOWN)
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

    private void findTrunk(Location root, Block base, TreeSpecies species, Set<Block> trunk)
    {
        Location loc = new Location(null, 0, 0, 0);
        Set<Block> blocks = new HashSet<>();
        for (BlockFace face : dir8)
        {
            Block relative = base.getRelative(face);
            if (!trunk.contains(relative) && isLog(relative, species))
            {
                relative.getLocation(loc).setY(root.getY());
                if (root.distanceSquared(loc) <= 25)
                {
                    blocks.add(relative);
                }
            }
        }

        Block up = base.getRelative(UP);
        if (!trunk.contains(up) && isLog(up, species))
        {
            blocks.add(up);
        }

        for (BlockFace face : dir8)
        {
            Block relative = up.getRelative(face);
            if (!trunk.contains(relative) && isLog(relative, species))
            {
                relative.getLocation(loc).setY(root.getY());
                if (root.distanceSquared(loc) <= 256)
                {
                    blocks.add(relative);
                }
            }
        }

        trunk.addAll(blocks);
        for (Block block : blocks)
        {
            findTrunk(root, block, species, trunk);
        }
    }

    private boolean isChopChop(Block block, ItemStack item)
    {
        if (block.getType() == LOG || block.getType() == LOG_2)
        {
            if (item.getType() == DIAMOND_AXE && item.getEnchantmentLevel(ARROW_KNOCKBACK) == 5)
            {
                if (block.getRelative(DOWN).getType() == DIRT || block.getRelative(DOWN).getType() == GRASS)
                {
                    return true;
                }
            }
        }
        return false;
    }
}
