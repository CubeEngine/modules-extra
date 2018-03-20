package org.cubeengine.module.chopchop;


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
    @Comment("The list of blocks to detect as trees logs")
    public List<BlockType> logTypes = new ArrayList<>();
    @Comment("The list of blocks to detect as tree leaves")
    public List<BlockType> leafTypes = new ArrayList<>();

    @Override
    public void onLoaded(File loadedFrom)
    {
        if (logTypes.isEmpty())
        {
            logTypes.add(BlockTypes.LOG);
            logTypes.add(BlockTypes.LOG2);
        }
        if (leafTypes.isEmpty())
        {
            leafTypes.add(BlockTypes.LEAVES);
            leafTypes.add(BlockTypes.LEAVES2);
        }
        if (soilTypes.isEmpty())
        {
            soilTypes.add(BlockTypes.DIRT);
            soilTypes.add(BlockTypes.GRASS);
        }
    }
}
