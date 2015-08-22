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
package de.cubeisland.engine.module.log;

import javax.inject.Inject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import de.cubeisland.engine.converter.ConverterManager;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import org.cubeengine.service.filesystem.FileManager;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.module.core.sponge.EventManager;
import org.cubeengine.service.command.CommandManager;
import de.cubeisland.engine.dirigent.macro.example.DateFormatter;
import de.cubeisland.engine.dirigent.macro.example.DateFormatter.DateReader;
import de.cubeisland.engine.module.bigdata.Bigdata;
import de.cubeisland.engine.module.log.action.ActionManager;
import de.cubeisland.engine.module.log.action.block.player.worldedit.LogEditSessionFactory;
import de.cubeisland.engine.module.log.action.player.item.container.ContainerType;
import de.cubeisland.engine.module.log.action.player.item.container.ContainerTypeConverter;
import de.cubeisland.engine.module.log.commands.LogCommands;
import de.cubeisland.engine.module.log.commands.LookupCommands;
import de.cubeisland.engine.module.log.converter.ArtConverter;
import de.cubeisland.engine.module.log.converter.BlockFaceConverter;
import de.cubeisland.engine.module.log.converter.DamageCauseConverter;
import de.cubeisland.engine.module.log.converter.EntityTypeConverter;
import de.cubeisland.engine.module.log.converter.ItemStackConverter;
import de.cubeisland.engine.module.log.converter.NoteConverter;
import de.cubeisland.engine.module.log.storage.LogManager;
import de.cubeisland.engine.module.log.tool.ToolListener;
import de.cubeisland.engine.reflect.Reflector;
import de.cubeisland.engine.reflect.codec.mongo.MongoDBCodec;
import org.spongepowered.api.item.inventory.ItemStack;

@ModuleInfo(name = "Log", description = "Log everything you want")
public class Log extends Module
{
    private LogManager logManager;
    private LogConfiguration config;
    private ObjectMapper objectMapper = null;
    private ActionManager actionManager;
    private boolean worldEditFound = false;

    @Inject private Bigdata bigdata;
    @Inject private I18n i18n;
    @Inject private FileManager fm;
    @Inject private EventManager em;
    @Inject private Reflector reflector;
    @Inject private CommandManager cm;
    @Inject private de.cubeisland.engine.logscribe.Log logger;

    @Override
    public void onEnable()
    {
        i18n.getCompositor().registerFormatter(new DateFormatter());
        i18n.getCompositor().registerReader(DateFormatter.class, "format", new DateReader());
        this.config = fm.loadConfig(this, LogConfiguration.class);
        ConverterManager cMan = reflector.getDefaultConverterManager();
        cMan.registerConverter(new ContainerTypeConverter(), ContainerType.class);
        cMan.registerConverter(new EntityTypeConverter(), EntityType.class);
        cMan.registerConverter(new DamageCauseConverter(), DamageCause.class);
        cMan.registerConverter(new BlockFaceConverter(), BlockFace.class);
        cMan.registerConverter(new ArtConverter(), Art.class);
        cMan.registerConverter(new NoteConverter(), Note.class);
        reflector.getCodecManager().getCodec(MongoDBCodec.class).
            getConverterManager().registerConverter(new ItemStackConverter(), ItemStack.class);
        this.logManager = new LogManager(this, bigdata);
        this.actionManager = new ActionManager(this, cm, em);

        cm.addCommands(cm, this, new LookupCommands(this));
        cm.addCommand(new LogCommands(this));
        try
        {
            Class.forName("com.sk89q.worldedit.WorldEdit");
            LogEditSessionFactory.initialize(this);
            em.registerListener(this, this); // only register if worldEdit is available
        }
        catch (ClassNotFoundException ignored)
        {
            logger.warn("No WorldEdit found!");
        }
        em.registerListener(this, new ToolListener(this));
    }

    @EventHandler
    public void onWorldEditEnable(PluginEnableEvent event)
    {
        if (event.getPlugin() instanceof WorldEditPlugin)
        {
            LogEditSessionFactory.initialize(this);
            worldEditFound = true;
        }
    }

    @Override
    public void onDisable()
    {
        this.logManager.disable();
        if (worldEditFound)
        {
            LogEditSessionFactory.shutdown();
        }
        super.onDisable();
    }

    public LogManager getLogManager()
    {
        return this.logManager;
    }

    public LogConfiguration getConfiguration() {
        return this.config;
    }

    public ObjectMapper getObjectMapper()
    {
        if (this.objectMapper == null)
        {
            this.objectMapper = new ObjectMapper();
        }
        return objectMapper;
    }

    public ActionManager getActionManager()
    {
        return actionManager;
    }

    public boolean hasWorldEdit()
    {
        return this.worldEditFound;
    }
}
