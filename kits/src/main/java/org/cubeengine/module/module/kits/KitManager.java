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
package org.cubeengine.module.module.kits;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.cubeengine.reflect.Reflector;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.libcube.service.matcher.StringMatcher;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import static org.cubeengine.libcube.service.filesystem.FileExtensionFilter.YAML;

public class KitManager
{
    private final Kits module;
    private final Map<String, Kit> kitMap = new HashMap<>();
    private final Map<Kit, KitConfiguration> kitConfigMap = new HashMap<>();
    private Reflector reflector;
    private StringMatcher sm;


    public KitManager(Kits module, Reflector reflector, StringMatcher sm)
    {
        this.module = module;
        this.reflector = reflector;
        this.sm = sm;
    }

    @Listener
    public void onJoin(ClientConnectionEvent.Join event)
    {
        if (!event.getTargetEntity().get(Keys.LAST_DATE_PLAYED).isPresent())
        {
            kitMap.values().stream().filter(Kit::isGiveKitOnFirstJoin).forEach(kit -> kit.give(event.getTargetEntity(), true));
        }
    }

    public Kit getKit(String name)
    {
        if (name == null)
        {
            return null;
        }
        Set<String> match = sm.getBestMatches(name.toLowerCase(Locale.ENGLISH), kitMap.keySet(), 2);
        if (match.isEmpty())
        {
            return null;
        }
        return kitMap.get(match.iterator().next());
    }

    public void saveKit(Kit kit)
    {
        KitConfiguration config = kitConfigMap.get(kit);
        if (config == null)
        {
            config = reflector.create(KitConfiguration.class);
            kitConfigMap.put(kit, config);
            kitMap.put(kit.getKitName(), kit);
        }
        kit.applyToConfig(config);
        config.save(module.getFolder().resolve(config.kitName + ".yml").toFile());
    }

    public void loadKit(Path file)
    {
        KitConfiguration config = reflector.load(KitConfiguration.class, file.toFile());
        config.kitName = StringUtils.stripFileExtension(file.getFileName().toString());
        Kit kit = config.getKit(module);
        kitConfigMap.put(kit, config);
        kitMap.put(config.kitName.toLowerCase(Locale.ENGLISH), kit);
    }

    public void loadKits()
    {
        Path folder = this.module.getFolder();
        try
        {
            Files.createDirectories(folder);
            try (DirectoryStream<Path> directory = Files.newDirectoryStream(folder, YAML))
            {
                for (Path file : directory)
                {
                    loadKit(file);
                }
            }
        }
        catch (IOException ex)
        {
            throw new IllegalStateException(ex);
        }
    }

    public Set<String> getKitsNames()
    {
        return this.kitMap.keySet();
    }

    public void deleteKit(Kit kit)
    {
        KitConfiguration conf = kitConfigMap.remove(kit);
        if (!conf.getFile().delete())
        {
            throw new IllegalStateException("Could not delete kit");
        }
        kitMap.values().remove(kit);
    }
}
