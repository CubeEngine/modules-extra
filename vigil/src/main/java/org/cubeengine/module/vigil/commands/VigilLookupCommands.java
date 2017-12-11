/*
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
package org.cubeengine.module.vigil.commands;

import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.vigil.Lookup;
import org.cubeengine.module.vigil.Vigil;
import org.cubeengine.module.vigil.data.LookupData;
import org.cubeengine.module.vigil.storage.QueryManager;
import org.spongepowered.api.entity.living.player.Player;

@Command(name = "vigil", alias = "log", desc = "Vigil-Module Commands")
public class VigilLookupCommands extends ContainerCommand
{
    private I18n i18n;
    private QueryManager qm;

    public VigilLookupCommands(CommandManager cm, I18n i18n, QueryManager qm)
    {
        super(cm, Vigil.class);
        this.i18n = i18n;
        this.qm = qm;
    }

    @Alias(value = "lookup")
    @Command(desc = "Performs a lookup.")
    public void lookup(Player context, @Named("radius") int radius)
    {
        LookupData ld = new LookupData();
        Lookup lookup = new Lookup(ld).with(context.getLocation()).withRadius(radius);
        this.qm.queryAndShow(lookup, context);
    }

}
