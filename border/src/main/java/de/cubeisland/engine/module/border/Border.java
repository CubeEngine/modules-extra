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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import de.cubeisland.engine.module.core.module.Module;
import de.cubeisland.engine.module.core.module.exception.ModuleLoadError;
import de.cubeisland.engine.module.service.world.WorldManager;
import de.cubeisland.engine.module.portals.Portals;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jooq.types.UInteger;

public class Border extends Module
{
    private BorderConfig globalConfig;
    private Map<UInteger, BorderConfig> worldConfigs;
    private WorldManager wm;
    private Path folder;
    private BorderPerms perms;

    @Override
    public void onEnable()
    {
        wm = this.getCore().getWorldManager();
        this.globalConfig = this.getCore().getConfigFactory().load(BorderConfig.class, this.getFolder().resolve("globalconfig.yml").toFile());
        folder = this.getFolder().resolve("worlds");
        try
        {
            Files.createDirectories(folder);
        }
        catch (IOException e)
        {
            throw new ModuleLoadError("Could not create the worlds folder", e);
        }
        this.worldConfigs = new HashMap<>();
        for (World world : Bukkit.getWorlds())
        {
            this.loadConfig(world);
        }
        perms = new BorderPerms(this);
        this.getCore().getEventManager().registerListener(this, new BorderListener(this));
        this.getCore().getCommandManager().addCommand(new BorderCommands(this));

    }

    private BorderConfig loadConfig(World world)
    {
        BorderConfig worldConfig = this.globalConfig.loadChild(folder.resolve(world.getName() + ".yml").toFile());
        this.worldConfigs.put(this.wm.getWorldId(world), worldConfig);

        if (!worldConfig.checkCenter(world))
        {
            this.getLog().warn("The world spawn of {} is not inside the border!", world.getName());
        }
        if (this.getCore().getModuleManager().getModule("portals") != null)
        {
            Portals portals = (Portals)this.getCore().getModuleManager().getModule("portals");
            portals.getPortalManager().setRandomDestinationSetting(world, worldConfig.radius, world.getChunkAt(worldConfig.center.chunkX, worldConfig.center.chunkZ));
        }
        return worldConfig;
    }

    public BorderConfig getConfig(World world)
    {
        BorderConfig worldConfig = this.worldConfigs.get(this.wm.getWorldId(world));
        if (worldConfig == null)
        {
            return this.loadConfig(world);
        }
        return worldConfig;
    }

    public BorderPerms perms()
    {
        return perms;
    }
}
