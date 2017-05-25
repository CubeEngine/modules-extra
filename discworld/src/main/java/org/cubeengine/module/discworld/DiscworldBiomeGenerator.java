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
package org.cubeengine.module.discworld;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.world.biome.BiomeTypes;
import org.spongepowered.api.world.extent.MutableBiomeVolume;
import org.spongepowered.api.world.gen.BiomeGenerator;

public class DiscworldBiomeGenerator implements BiomeGenerator
{
    private static final double ISLAND_SIZE = 200f;
    private static final double BEACH_RADIUS = ISLAND_SIZE * ISLAND_SIZE;
    private static final double FOREST_SIZE = ISLAND_SIZE - 7;
    private static final double FOREST_RADIUS = FOREST_SIZE * FOREST_SIZE;
    private static final double HILLS_SIZE = FOREST_SIZE - 120;
    private static final double HILLS_RADIUS = HILLS_SIZE * HILLS_SIZE;

    @Override
    public void generateBiomes(MutableBiomeVolume buffer)
    {
        Vector3i min = buffer.getBiomeMin();
        Vector3i max = buffer.getBiomeMax();

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                if (x * x + y * y < HILLS_RADIUS) {
                    buffer.setBiome(x, y, 0, BiomeTypes.EXTREME_HILLS);
                } else if (x * x + y * y < FOREST_RADIUS) {
                    buffer.setBiome(x, y, 0, BiomeTypes.FOREST);
                } else if (x * x + y * y < BEACH_RADIUS) {
                    buffer.setBiome(x, y, 0, BiomeTypes.BEACH);
                } else {
                    buffer.setBiome(x, y, 0, BiomeTypes.OCEAN);
                    // TODO mushrooms islands
                }
            }
        }
    }
}
