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
package org.cubeengine.module.itemduct.data;

import static org.spongepowered.api.block.BlockTypes.BLACK_STAINED_GLASS;
import static org.spongepowered.api.block.BlockTypes.BLACK_STAINED_GLASS_PANE;
import static org.spongepowered.api.block.BlockTypes.BLUE_STAINED_GLASS;
import static org.spongepowered.api.block.BlockTypes.BLUE_STAINED_GLASS_PANE;
import static org.spongepowered.api.block.BlockTypes.BROWN_STAINED_GLASS;
import static org.spongepowered.api.block.BlockTypes.BROWN_STAINED_GLASS_PANE;
import static org.spongepowered.api.block.BlockTypes.CHISELED_QUARTZ_BLOCK;
import static org.spongepowered.api.block.BlockTypes.CYAN_STAINED_GLASS;
import static org.spongepowered.api.block.BlockTypes.CYAN_STAINED_GLASS_PANE;
import static org.spongepowered.api.block.BlockTypes.GLASS;
import static org.spongepowered.api.block.BlockTypes.GLASS_PANE;
import static org.spongepowered.api.block.BlockTypes.GRAY_STAINED_GLASS;
import static org.spongepowered.api.block.BlockTypes.GRAY_STAINED_GLASS_PANE;
import static org.spongepowered.api.block.BlockTypes.GREEN_STAINED_GLASS;
import static org.spongepowered.api.block.BlockTypes.GREEN_STAINED_GLASS_PANE;
import static org.spongepowered.api.block.BlockTypes.LIGHT_BLUE_STAINED_GLASS;
import static org.spongepowered.api.block.BlockTypes.LIGHT_BLUE_STAINED_GLASS_PANE;
import static org.spongepowered.api.block.BlockTypes.LIGHT_GRAY_STAINED_GLASS;
import static org.spongepowered.api.block.BlockTypes.LIGHT_GRAY_STAINED_GLASS_PANE;
import static org.spongepowered.api.block.BlockTypes.LIME_STAINED_GLASS;
import static org.spongepowered.api.block.BlockTypes.LIME_STAINED_GLASS_PANE;
import static org.spongepowered.api.block.BlockTypes.MAGENTA_STAINED_GLASS;
import static org.spongepowered.api.block.BlockTypes.MAGENTA_STAINED_GLASS_PANE;
import static org.spongepowered.api.block.BlockTypes.ORANGE_STAINED_GLASS;
import static org.spongepowered.api.block.BlockTypes.ORANGE_STAINED_GLASS_PANE;
import static org.spongepowered.api.block.BlockTypes.PINK_STAINED_GLASS;
import static org.spongepowered.api.block.BlockTypes.PINK_STAINED_GLASS_PANE;
import static org.spongepowered.api.block.BlockTypes.PURPLE_STAINED_GLASS;
import static org.spongepowered.api.block.BlockTypes.PURPLE_STAINED_GLASS_PANE;
import static org.spongepowered.api.block.BlockTypes.QUARTZ_BLOCK;
import static org.spongepowered.api.block.BlockTypes.QUARTZ_PILLAR;
import static org.spongepowered.api.block.BlockTypes.RED_STAINED_GLASS;
import static org.spongepowered.api.block.BlockTypes.RED_STAINED_GLASS_PANE;
import static org.spongepowered.api.block.BlockTypes.SMOOTH_QUARTZ;
import static org.spongepowered.api.block.BlockTypes.STICKY_PISTON;
import static org.spongepowered.api.block.BlockTypes.WHITE_STAINED_GLASS;
import static org.spongepowered.api.block.BlockTypes.WHITE_STAINED_GLASS_PANE;
import static org.spongepowered.api.block.BlockTypes.YELLOW_STAINED_GLASS;
import static org.spongepowered.api.block.BlockTypes.YELLOW_STAINED_GLASS_PANE;

import com.google.inject.Singleton;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;

import java.util.HashSet;
import java.util.Set;

@Singleton
public class ItemductBlocks
{
    private Set<BlockType> pipeTypes = new HashSet<>();
    private Set<BlockType> directionalPipeTypes = new HashSet<>();
    private Set<BlockType> storagePipeTypes = new HashSet<>();

    public void init() {
        this.pipeTypes.clear();
        this.pipeTypes.add(GLASS.get());
        this.pipeTypes.add(WHITE_STAINED_GLASS.get());
        this.pipeTypes.add(ORANGE_STAINED_GLASS.get());
        this.pipeTypes.add(MAGENTA_STAINED_GLASS.get());
        this.pipeTypes.add(LIGHT_BLUE_STAINED_GLASS.get());
        this.pipeTypes.add(YELLOW_STAINED_GLASS.get());
        this.pipeTypes.add(LIME_STAINED_GLASS.get());
        this.pipeTypes.add(PINK_STAINED_GLASS.get());
        this.pipeTypes.add(GRAY_STAINED_GLASS.get());
        this.pipeTypes.add(LIGHT_GRAY_STAINED_GLASS.get());
        this.pipeTypes.add(CYAN_STAINED_GLASS.get());
        this.pipeTypes.add(BLUE_STAINED_GLASS.get());
        this.pipeTypes.add(PURPLE_STAINED_GLASS.get());
        this.pipeTypes.add(BLACK_STAINED_GLASS.get());
        this.pipeTypes.add(BROWN_STAINED_GLASS.get());
        this.pipeTypes.add(GREEN_STAINED_GLASS.get());
        this.pipeTypes.add(RED_STAINED_GLASS.get());
        this.pipeTypes.add(BLACK_STAINED_GLASS.get());

        this.directionalPipeTypes.add(GLASS_PANE.get());
        this.directionalPipeTypes.add(WHITE_STAINED_GLASS_PANE.get());
        this.directionalPipeTypes.add(ORANGE_STAINED_GLASS_PANE.get());
        this.directionalPipeTypes.add(MAGENTA_STAINED_GLASS_PANE.get());
        this.directionalPipeTypes.add(LIGHT_BLUE_STAINED_GLASS_PANE.get());
        this.directionalPipeTypes.add(YELLOW_STAINED_GLASS_PANE.get());
        this.directionalPipeTypes.add(LIME_STAINED_GLASS_PANE.get());
        this.directionalPipeTypes.add(PINK_STAINED_GLASS_PANE.get());
        this.directionalPipeTypes.add(GRAY_STAINED_GLASS_PANE.get());
        this.directionalPipeTypes.add(LIGHT_GRAY_STAINED_GLASS_PANE.get());
        this.directionalPipeTypes.add(CYAN_STAINED_GLASS_PANE.get());
        this.directionalPipeTypes.add(BLUE_STAINED_GLASS_PANE.get());
        this.directionalPipeTypes.add(PURPLE_STAINED_GLASS_PANE.get());
        this.directionalPipeTypes.add(BLACK_STAINED_GLASS_PANE.get());
        this.directionalPipeTypes.add(BROWN_STAINED_GLASS_PANE.get());
        this.directionalPipeTypes.add(GREEN_STAINED_GLASS_PANE.get());
        this.directionalPipeTypes.add(RED_STAINED_GLASS_PANE.get());
        this.directionalPipeTypes.add(BLACK_STAINED_GLASS_PANE.get());

        this.pipeTypes.addAll(this.directionalPipeTypes);

        this.storagePipeTypes.clear();
        this.storagePipeTypes.add(QUARTZ_BLOCK.get());
        this.storagePipeTypes.add(QUARTZ_PILLAR.get());
        this.storagePipeTypes.add(CHISELED_QUARTZ_BLOCK.get());
        this.storagePipeTypes.add(SMOOTH_QUARTZ.get());
    }

    public boolean isPipe(BlockType type) {
        return this.pipeTypes.contains(type) || this.storagePipeTypes.contains(type);
    }

    public boolean isNormalPipe(BlockType type) {
        return this.pipeTypes.contains(type);
    }

    public boolean isStoragePipe(BlockType type) {
        return this.storagePipeTypes.contains(type);
    }

    public boolean isDirectionalPipe(BlockType type) {
        return this.directionalPipeTypes.contains(type);
    }

    public static boolean isEndPointType(BlockType type)
    {
        return BlockTypes.PISTON.get().equals(type) || STICKY_PISTON.get().equals(type) || BlockTypes.OBSERVER.get().equals(type);
    }

}
