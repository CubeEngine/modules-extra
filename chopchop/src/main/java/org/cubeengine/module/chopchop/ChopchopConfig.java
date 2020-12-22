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

import org.cubeengine.reflect.Section;
import org.cubeengine.reflect.annotations.Comment;
import org.cubeengine.reflect.codec.yaml.ReflectedYaml;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ChopchopConfig extends ReflectedYaml
{

    @Comment("The list of blocks to detect as soil for trees to grow on")
    public List<BlockType> soilTypes = new ArrayList<>();

    public List<Tree> trees = new ArrayList<>();

    public static class Tree implements Section {

        @Comment("The list of blocks to detect as trees logs")
        public BlockType logType;
        @Comment("The list of blocks to detect as tree leaves")
        public BlockType leafType;
        @Comment("The list of blocks to detect as tree saplings")
        public BlockType saplingType;

        public static Tree of(BlockType log, BlockType leaf, BlockType sapling)
        {
            final Tree tree = new Tree();
            tree.logType = log;
            tree.leafType = leaf;
            tree.saplingType = sapling;
            return tree;
        }
    }

    @Override
    public void onLoaded(File loadedFrom)
    {
        if (this.trees.isEmpty())
        {
            this.trees.add(Tree.of(BlockTypes.OAK_LOG.get(), BlockTypes.OAK_LEAVES.get(), BlockTypes.OAK_SAPLING.get()));
            this.trees.add(Tree.of(BlockTypes.SPRUCE_LOG.get(), BlockTypes.SPRUCE_LEAVES.get(), BlockTypes.SPRUCE_SAPLING.get()));
            this.trees.add(Tree.of(BlockTypes.BIRCH_LOG.get(), BlockTypes.BIRCH_LEAVES.get(), BlockTypes.BIRCH_SAPLING.get()));
            this.trees.add(Tree.of(BlockTypes.JUNGLE_LOG.get(), BlockTypes.JUNGLE_LEAVES.get(), BlockTypes.JUNGLE_SAPLING.get()));
            this.trees.add(Tree.of(BlockTypes.ACACIA_LOG.get(), BlockTypes.ACACIA_LEAVES.get(), BlockTypes.ACACIA_SAPLING.get()));
            this.trees.add(Tree.of(BlockTypes.DARK_OAK_LOG.get(), BlockTypes.DARK_OAK_LEAVES.get(), BlockTypes.DARK_OAK_SAPLING.get()));
            this.trees.add(Tree.of(BlockTypes.CRIMSON_STEM.get(), BlockTypes.NETHER_WART_BLOCK.get(), BlockTypes.CRIMSON_FUNGUS.get()));
            this.trees.add(Tree.of(BlockTypes.WARPED_STEM.get(), BlockTypes.WARPED_WART_BLOCK.get(), BlockTypes.WARPED_FUNGUS.get()));
        }

        if (soilTypes.isEmpty())
        {
            soilTypes.add(BlockTypes.DIRT.get());
            soilTypes.add(BlockTypes.GRASS.get());
            soilTypes.add(BlockTypes.WARPED_NYLIUM.get());
            soilTypes.add(BlockTypes.CRIMSON_NYLIUM.get());
        }
    }
}
