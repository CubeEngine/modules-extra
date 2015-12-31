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
package org.cubeengine.module.vigil;

import org.cubeengine.module.core.util.StringUtils;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.ReportActions;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;

import org.spongepowered.api.text.format.TextColors;

import java.util.List;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;

public class Receiver
{
    private final CommandSource cmdSource;
    private final I18n i18n;
    private Object lookup;

    public Receiver(CommandSource cmdSource, I18n i18n, Object lookup)
    {
        this.cmdSource = cmdSource;
        this.i18n = i18n;
        this.lookup = lookup;
    }

    // TODO translate msgs on this method
    public void sendReport(List<Action> actions, String msg, Object... args)
    {
        Text trans = i18n.getTranslation(cmdSource, NEUTRAL, msg, args);
        // TODO add info (where when etc.)
        cmdSource.sendMessage(trans);
    }

    public void sendReport(List<Action> actions, int size, String msgSingular, String msgPlural, Object... args)
    {
        Text trans = i18n.getTranslationN(cmdSource, NEUTRAL, size, msgSingular, msgPlural, args);
        // TODO add info (where when etc.)
        cmdSource.sendMessage(trans);
    }

    public void sendReports(List<ReportActions> reportActions)
    {
        if (reportActions.isEmpty())
        {
            i18n.sendTranslated(cmdSource, NEGATIVE, "Nothing logged here");
            return;
        }
        cmdSource.sendMessage(Text.of(TextColors.GOLD, StringUtils.repeat("-", 53)));
        for (ReportActions reportAction : reportActions)
        {
            reportAction.showReport(this);
        }
    }
}
