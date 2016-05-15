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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.ReportActions;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.pagination.PaginationList.Builder;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.text.format.TextColors.GRAY;
import static org.spongepowered.api.text.format.TextColors.RED;

public class Receiver
{
    private final CommandSource cmdSource;
    private final I18n i18n;
    private Lookup lookup;

    private List<Text> lines = new ArrayList<>();

    public Receiver(CommandSource cmdSource, I18n i18n, Lookup lookup)
    {
        this.cmdSource = cmdSource;
        this.i18n = i18n;
        this.lookup = lookup;
    }

    // TODO translate msgs on this method
    public void sendReport(List<Action> actions, String msg, Object... args)
    {
        sendReport(actions, i18n.getTranslation(cmdSource, NEUTRAL, msg, args));
    }

    public void sendReport(List<Action> actions, int size, String msgSingular, String msgPlural, Object... args)
    {
        sendReport(actions, i18n.getTranslationN(cmdSource, NEUTRAL, size, msgSingular, msgPlural, args));
    }

    private static final SimpleDateFormat dateShort = new SimpleDateFormat("yy-MM-dd");
    private static final SimpleDateFormat dateLong = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat timeLong = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat timeShort = new SimpleDateFormat("HH:mm");

    private void sendReport(List<Action> actions, Text trans)
    {
        Action firstAction = actions.get(0);
        Action lastAction = actions.get(actions.size() - 1);

        Text date = getDatePrefix(firstAction, lastAction, false); // TODO fulldate?
        // TODO add info (where when etc.)
        lines.add(Text.of(date, RED, " - ", trans));
    }

    private Text getDatePrefix(Action firstAction, Action lastAction, boolean fullDate)
    {

        if (firstAction == lastAction)
        {
            Date date = firstAction.getDate();
            String dLong = dateLong.format(date);
            boolean sameDay = dateLong.format(new Date()).equals(dLong);
            String tLong = timeLong.format(date);
            Text full = Text.of(GRAY, dLong, " ", tLong);
            if (fullDate)
            {
                return full;
            }
            String tShort = timeShort.format(date);
            if (sameDay) // Today?
            {
                return Text.of(GRAY, tShort).toBuilder().onHover(TextActions.showText(full)).build();
            }
            else
            {
                return Text.of(GRAY, dateShort.format(date), " ", tShort).toBuilder().onHover(TextActions.showText(full)).build();
            }
        }
        else
        {
            Date firstDate = firstAction.getData(Action.DATE);
            Date lastDate = lastAction.getData(Action.DATE);
            return Text.of("range"); // TODO
        }
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
        Builder builder = Sponge.getGame().getServiceManager().provideUnchecked(PaginationService.class).builder();
        builder.title(i18n.getTranslation(cmdSource, POSITIVE, "Showing {amount} Logs", lines.size())).padding(Text.of("-"))
               .contents(lines).sendTo(cmdSource);
    }

    public Locale getLocale()
    {
        return cmdSource.getLocale();
    }

    public Lookup getLookup()
    {
        return lookup;
    }
}
