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

import java.nio.file.Path;
import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.reflect.Reflector;
import de.cubeisland.engine.reflect.codec.nbt.NBTCodec;
import org.cubeengine.module.backpack.converter.NBTItemStackConverter;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.inventoryguard.InventoryGuardFactory;
import org.spongepowered.api.item.inventory.ItemStack;

@ModuleInfo(name = "Backpack", description = "Expand your inventory")
/*
TODO blocked by custom inventories implementation
TODO replace global/grouped/single with context:
    backpack names are unique to one player and can be active in any number of context
TODO NBT-Reflect Context Reader
TODO add cmds to add/remove context
 */
public class Backpack extends Module
{
    private BackpackManager manager;

    @Inject private Path modulePath;
    @Inject private Reflector reflector;
    @Inject private I18n i18n;
    @Inject private CommandManager cm;
    @Inject private EventManager em;
    @Inject private InventoryGuardFactory igf;

    public BackpackPermissions perms()
    {
        return perms;
    }

    private BackpackPermissions perms;

    @Enable
    public void onEnable()
    {
        perms = new BackpackPermissions(this);
        reflector.getCodecManager().getCodec(NBTCodec.class).getConverterManager().
            registerConverter(new NBTItemStackConverter(), ItemStack.class);

        manager = new BackpackManager(this, reflector, i18n);
        cm.addCommand(new BackpackCommands(this, manager, i18n));
        em.registerListener(this, manager);
    }

    public Path getModulePath()
    {
        return modulePath;
    }

    public InventoryGuardFactory getInventoryGuardFactory()
    {
        return igf;
    }
}
