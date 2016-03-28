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
package org.cubeengine.module.backpack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.cubeengine.service.user.UserAttachment;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.World;


public class BackpackAttachment
{
    public void loadGlobalBackpacks()
    {
        this.loadBackpacks(module.globalDir, globalBackpacks);
    }
    // backpack/global/<playername>/backpackname

    // backpack/<worldname>/<playername>/backpackname
    // OR backpack/grouped/<mainworldname>/<playername>/<backpackname>
    // OR backpack/single/<worldname>/...
    // without worlds module groups work as default universe would build world world_nether & world_the_end are grouped
    public void loadBackpacks(World world)
    {
        this.loadGlobalBackpacks();
        if (world == null)
        {
            return;
        }
        Backpack module = (Backpack)this.getModule();
        Path dir = module.singleDir.resolve(world.getName());
        if (Files.isDirectory(dir))
        {
            Map<String, BackpackInventory> map = this.backpacks.get(world);
            if (map == null)
            {
                map = new HashMap<>();
                this.backpacks.put(world, map);
            }
            this.loadBackpacks(dir, map);
        }
        World mainWorld = Sponge.getServer().getWorld(Sponge.getServer().getDefaultWorld().get().getUniqueId()).get();
        dir = module.groupedDir.resolve(mainWorld.getName());
        if (Files.isDirectory(dir))
        {
            Map<String, BackpackInventory> map = this.groupedBackpacks.get(mainWorld);
            if (map == null)
            {
                map = new HashMap<>();
                this.groupedBackpacks.put(mainWorld, map);
            }
            this.loadBackpacks(dir, map);
        }
    }

    private Path getGlobalBackpack(String name)
    {
        Backpack module = (Backpack)this.getModule();
        Path path = module.globalDir.resolve(this.getHolder().getUniqueId().toString());
        try
        {
            Files.createDirectories(path);
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e); // TODO better exeption
        }
        return path.resolve(name + DAT.getExtention());
    }

    private Path getSingleBackpack(String name, String worldName)
    {
        Backpack module = (Backpack)this.getModule();
        Path path = module.singleDir.resolve(worldName).resolve(this.getHolder().getUniqueId().toString());
        try
        {
            Files.createDirectories(path);
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e); // TODO better exeption
        }
        return path.resolve(name + DAT.getExtention());
    }

    private Path getGroupedBackpack(String name, String worldName)
    {
        Backpack module = (Backpack)this.getModule();
        Path path = module.groupedDir.resolve(worldName).resolve(this.getHolder().getUniqueId().toString());
        try
        {
            Files.createDirectories(path);
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e); // TODO better exeption
        }
        return path.resolve(name + DAT.getExtention());
    }



    public BackpackInventory getBackpack(String name, World world)
    {
        BackpackInventory backpack = this.globalBackpacks.get(name);
        if (backpack != null)
        {
            return backpack;
        }
        if (world == null) return null;
        Map<String, BackpackInventory> map = this.backpacks.get(world);
        if (map != null)
        {
             backpack = map.get(name);
        }
        if (backpack != null)
        {
            return backpack;
        }

        World mainWorld = Sponge.getServer().getWorld(Sponge.getServer().getDefaultWorld().get().getUniqueId()).get();
        map = this.groupedBackpacks.get(mainWorld);
        if (map != null)
        {
            backpack = map.get(name);
        }
        return backpack;
    }
}
