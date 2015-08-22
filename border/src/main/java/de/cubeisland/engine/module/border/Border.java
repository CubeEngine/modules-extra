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
import javax.inject.Inject;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.module.core.module.Module;
import de.cubeisland.engine.module.core.module.exception.ModuleLoadError;
import org.cubeengine.module.core.sponge.EventManager;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.world.WorldManager;
import org.cubeengine.module.portals.Portals;
import de.cubeisland.engine.reflect.Reflector;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jooq.types.UInteger;
import org.spongepowered.api.world.World;

@ModuleInfo(name = "Border", description = "Limiting the world size")
public class Border extends Module
{
    private BorderConfig globalConfig;
    private Map<UInteger, BorderConfig> worldConfigs;
    @Inject private WorldManager wm;
    @Inject private Reflector reflector;
    @Inject private EventManager em;
    @Inject private CommandManager cm;
    @Inject private Log logger;
    private Path folder;
    private BorderPerms perms;

    @Enable
    public void onEnable()
    {
        this.globalConfig = reflector.load(BorderConfig.class, this.getFolder().resolve("globalconfig.yml").toFile());
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
        wm.getWorlds().forEach(this::loadConfig);
        perms = new BorderPerms(this);
        em.registerListener(this, new BorderListener(this));
        cm.addCommand(new BorderCommands(this));

    }

    private BorderConfig loadConfig(World world)
    {
        BorderConfig worldConfig = this.globalConfig.loadChild(folder.resolve(world.getName() + ".yml").toFile());
        this.worldConfigs.put(this.wm.getWorldId(world), worldConfig);

        if (!worldConfig.checkCenter(world))
        {
            logger.warn("The world spawn of {} is not inside the border!", world.getName());
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
