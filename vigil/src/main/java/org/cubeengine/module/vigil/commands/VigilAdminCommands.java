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
package org.cubeengine.module.vigil.commands;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.libcube.util.ConfirmManager.requestConfirmation;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.vigil.Vigil;
import org.cubeengine.module.vigil.storage.QueryManager;
import org.spongepowered.api.command.CommandSource;

@Command(name = "admin", desc = "Vigil-Admin Commands")
public class VigilAdminCommands extends ContainerCommand
{
    private I18n i18n;
    private QueryManager qm;

    public VigilAdminCommands(CommandManager cm, I18n i18n, QueryManager qm)
    {
        super(cm, Vigil.class);
        this.i18n = i18n;
        this.qm = qm;
    }

    @Command(desc = "purges all logs")
    public void purge(CommandSource ctx)
    {
        requestConfirmation(i18n, i18n.getTranslation(ctx.getLocale(), NEUTRAL, "Do you really want do delete ALL logs?"), ctx, () -> runPurge(ctx));
    }

    private void runPurge(CommandSource ctx)
    {
        qm.purge();
        i18n.sendTranslated(ctx, POSITIVE, "Purged all logs from database!");
    }
}
