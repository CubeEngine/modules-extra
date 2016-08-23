package org.cubeengine.module.discworld;

import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.world.WorldCreationSettings;
import org.spongepowered.api.world.gen.WorldGenerator;
import org.spongepowered.api.world.gen.WorldGeneratorModifier;

public class DiscworldGeneratorModifier implements WorldGeneratorModifier
{
    @Override
    public void modifyWorldGenerator(WorldCreationSettings world, DataContainer settings, WorldGenerator worldGenerator)
    {
        worldGenerator.setBiomeGenerator(new DiscworldBiomeGenerator());
    }

    @Override
    public String getId()
    {
        return "cubeengine:discworld:identifier";
    }

    @Override
    public String getName()
    {
        return "Discworld Modifier";
    }
}
