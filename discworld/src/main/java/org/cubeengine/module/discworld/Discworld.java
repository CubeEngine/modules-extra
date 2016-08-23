package org.cubeengine.module.discworld;

import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.gen.BiomeGenerator;
import org.spongepowered.api.world.gen.WorldGeneratorModifier;

@ModuleInfo(name = "Discworld", description = "Provides a custom world generator for a discworld")
public class Discworld extends Module
{
    public Discworld()
    {
        Sponge.getRegistry().register(WorldGeneratorModifier.class, new DiscworldGeneratorModifier());
    }
}
