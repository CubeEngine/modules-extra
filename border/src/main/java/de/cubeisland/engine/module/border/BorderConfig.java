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
package de.cubeisland.engine.module.border;

import de.cubeisland.engine.reflect.Section;
import de.cubeisland.engine.reflect.annotations.Comment;
import de.cubeisland.engine.reflect.annotations.Name;
import de.cubeisland.engine.reflect.codec.yaml.ReflectedYaml;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.World;

@SuppressWarnings("all")
public class BorderConfig extends ReflectedYaml
{
    @Name("chunk-radius")
    public int radius = 30;

    @Name("square-area")
    @Comment("Whether the radius should define a square instead of a circle around the spawn point")
    public boolean square = false;

    @Name("allow-bypass")
    @Comment("Whether players can bypass the restriction with a permission")
    public boolean allowBypass = false;

    @Name("enable-torus")
    @Comment("Experimental! The world acts as a torus. If you reach the border on the north side you'll get teleported to the south of the map")
    public boolean torusWorld = false;

    public Center center;

    public void setCenter(Chunk center, boolean isSpawn)
    {
        this.center.chunkX = center.getX();
        this.center.chunkZ = center.getZ();
        this.center.useSpawn = isSpawn;

        try
        {
            this.removeInheritedField(BorderConfig.this.getClass().getField("center"));
            this.removeInheritedField(this.center.getClass().getField("chunkX"));
            this.removeInheritedField(this.center.getClass().getField("chunkZ"));
        }
        catch (NoSuchFieldException ignored)
        {}
        this.save();
    }

    public boolean checkCenter(World world)
    {
        if (center.useSpawn)
        {
             world.getSpawnLocation().getPosition()
            setCenter(world.getSpawnLocation().getChunk(), true);
            return true;
        }
        return !BorderListener.isChunkInRange(world.getSpawnLocation().getChunk(), BorderConfig.this);
    }

    public static class Center implements Section
    {
        @Comment("When set to true the x and z values will be set to the worlds spawn chunk")
        public boolean useSpawn = true;
        public Integer chunkX = null;
        public Integer chunkZ = null;
    }
}
