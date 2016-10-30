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
