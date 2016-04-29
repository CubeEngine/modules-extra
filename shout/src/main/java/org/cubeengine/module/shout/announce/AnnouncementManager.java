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
package org.cubeengine.module.shout.announce;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import de.cubeisland.engine.reflect.Reflector;
import org.cubeengine.module.shout.Shout;
import org.cubeengine.libcube.service.i18n.I18n;
import de.cubeisland.engine.logscribe.Log;
import org.cubeengine.module.shout.announce.task.DynamicCycleTask;
import org.cubeengine.libcube.service.matcher.StringMatcher;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.libcube.service.task.TaskManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;

import static org.cubeengine.libcube.service.filesystem.FileExtensionFilter.YAML;

/**
 * Class to manage all the announcements and their receivers
 */
public class AnnouncementManager
{
    private final Log log;
    private final Shout module;
    private PermissionManager pm;
    private final Path modulePath;
    private final Map<String, Announcement> dynamicAnnouncements;
    private final Map<String, FixedCycleAnnouncement> fixedCycleAnnouncements;
    private final I18n i18n;
    private final TaskManager tm;
    private StringMatcher stringMatcher;
    private Reflector reflector;

    private final Map<UUID, DynamicCycleTask> dynamicTasks = new HashMap<>();

    public AnnouncementManager(Shout module, Path modulePath, I18n i18n, PermissionManager pm, TaskManager tm,
                               StringMatcher stringMatcher, Reflector reflector)
    {
        this.module = module;
        this.pm = pm;
        this.tm = tm;
        this.stringMatcher = stringMatcher;
        this.reflector = reflector;
        this.log = module.getLog();
        this.i18n = i18n;
        this.dynamicAnnouncements = new HashMap<>();
        this.fixedCycleAnnouncements = new LinkedHashMap<>();
        this.modulePath = modulePath;

        this.handleFirstRun();
    }

    private void handleFirstRun()
    {
        Path file = modulePath.resolve(".shout");
        if (Files.exists(file))
        {
            return;
        }
        try
        {
            Files.createFile(file);
            try
            {
                this.createAnnouncement("Example", "This is an example announcement", "10 minutes", "*", false);
            }
            catch (Exception ex)
            {
                log.warn(ex, "An exception occured when creating the example announcement!");
            }
        }
        catch (IOException ex)
        {
            log.debug(ex, "There was an error creating a file: {}", file);
        }
    }

    /**
     * Get all the announcements registered
     *
     * @return All announcements currently registered
     */
    public Collection<Announcement> getAllAnnouncements()
    {
        Collection<Announcement> announcements = new HashSet<>();
        announcements.addAll(this.dynamicAnnouncements.values());
        announcements.addAll(this.fixedCycleAnnouncements.values());
        return announcements;
    }

    /**
     * Get announcement by name
     *
     * @param   name    Name of the announcement
     * @return  The announcement with this name, or null if not exist
     */
    public Announcement getAnnouncement(String name)
    {
        Map<String, Announcement> announcements = new HashMap<>();
        announcements.putAll(this.dynamicAnnouncements);
        announcements.putAll(this.fixedCycleAnnouncements);
        name = name.toLowerCase(Locale.ENGLISH);
        Announcement announcement = announcements.get(name);
        if (announcement == null)
        {
            Set<String> matches = stringMatcher.getBestMatches(name, announcements.keySet(), 3);

            if (matches.size() > 0)
            {
                announcement = announcements.get(matches.iterator().next());
            }
        }
        return announcement;
    }

    /**
     * Load the dynamicAnnouncements of a user
     * this will create an Receiver and call initializeReceiver
     *
     * @param user the user to load
     */
    public void initializeUser(Player user)
    {
        DynamicCycleTask messageTask = new DynamicCycleTask(tm, user, module, this);
        dynamicAnnouncements.values().forEach(messageTask::addAnnouncement);
        dynamicTasks.put(user.getUniqueId(), messageTask);
        messageTask.run();
    }

    /**
     * Reload all loaded announcements and users
     */
    public void reload()
    {
        dynamicTasks.values().forEach(DynamicCycleTask::stop);
        dynamicTasks.clear();
        dynamicAnnouncements.clear();

        fixedCycleAnnouncements.values().forEach(FixedCycleAnnouncement::stop);
        fixedCycleAnnouncements.clear();

        tm.cancelTasks(this.module);

        loadAnnouncements();
        initUsers();
    }

    public void initUsers()
    {
        Sponge.getServer().getOnlinePlayers().forEach(this::initializeUser);
    }

    public void addAnnouncement(final Announcement announcement)
    {
        if (announcement instanceof FixedCycleAnnouncement)
        {
             this.fixedCycleAnnouncements.put(announcement.getName().toLowerCase(Locale.ENGLISH), ((FixedCycleAnnouncement)announcement).start());
        }
        else
        {
            this.dynamicAnnouncements.put(announcement.getName().toLowerCase(Locale.ENGLISH), announcement);
            for (DynamicCycleTask task : dynamicTasks.values())
            {
                task.addAnnouncement(announcement);
            }
        }
    }

    /**
     * Load announcements
     */
    public void loadAnnouncements()
    {
        try (DirectoryStream<Path> directory = Files.newDirectoryStream(modulePath, YAML))
        {
            for (Path path : directory)
            {
                addAnnouncement(loadAnnouncement(path));
            }
        }
        catch (IOException ex)
        {
            this.log.warn(ex, "An error occured while loading announcements.");
        }
    }

    /**
     * Load a specific announcement
     *
     * @param file the file to load from
     */
    public Announcement loadAnnouncement(Path file)
    {
        AnnouncementConfig config = reflector.load(AnnouncementConfig.class, file.toFile());
        String name = file.getFileName().toString().toLowerCase(Locale.US);
        if (config.fixedCycle)
        {
            return new FixedCycleAnnouncement(module, name, config, pm, tm);
        }
        return new Announcement(module, name, config, pm);
    }



    /**
     * Create an announcement folder structure with the params specified.
     * This will not load the announcement into the plugin
     */
    public Announcement createAnnouncement(String name, String message, String delay, String permName, boolean fc) throws IOException, IllegalArgumentException
    {
        Path file = this.modulePath.resolve(name + YAML.getExtention());

        AnnouncementConfig config = reflector.create(AnnouncementConfig.class);
        config.setFile(file.toFile());
        config.delay = delay;
        config.permName = permName;
        config.fixedCycle = fc;
        config.announcement = message;
        config.save();

        if (config.fixedCycle)
        {
            return new FixedCycleAnnouncement(module, name, config, pm, tm);
        }
        return new Announcement(module, name, config, pm);
    }

    public void stop(Player player)
    {
        DynamicCycleTask task = dynamicTasks.remove(player.getUniqueId());
        task.stop();
    }
}
