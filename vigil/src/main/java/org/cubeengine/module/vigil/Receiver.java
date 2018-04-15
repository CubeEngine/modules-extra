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
package org.cubeengine.module.vigil;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.CRITICAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NONE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.text.action.TextActions.executeCallback;
import static org.spongepowered.api.text.action.TextActions.showText;
import static org.spongepowered.api.text.format.TextColors.GRAY;
import static org.spongepowered.api.text.format.TextColors.RED;
import static org.spongepowered.api.text.format.TextColors.WHITE;

import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.libcube.util.TimeUtil;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.Recall;
import org.cubeengine.module.vigil.report.Report;
import org.cubeengine.module.vigil.report.ReportActions;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.pagination.PaginationList.Builder;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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
    public void sendReport(Report report, List<Action> actions, String msg, Object... args)
    {
        Text reports = Text.of(report.getClass().getSimpleName());
        if (report instanceof Report.ReportGrouping)
        {
            reports = Text.of(((Report.ReportGrouping) report).getReportsList().stream().map(Class::getSimpleName).collect(Collectors.joining("/")));
        }
        Text trans = i18n.translate(cmdSource, NEUTRAL, msg, args)
                .toBuilder().onHover(TextActions.showText(reports)).build();
        sendReport(actions, trans);
    }

    public void sendReport(Report report, List<Action> actions, int size, String msgSingular, String msgPlural, Object... args)
    {
        Text reports = Text.of(report.getClass().getSimpleName());
        if (report instanceof Report.ReportGrouping)
        {
            reports = Text.of(((Report.ReportGrouping) report).getReportsList().stream().map(Class::getSimpleName).collect(Collectors.joining("/")));
        }
        Text trans = i18n.translateN(cmdSource, NEUTRAL, size, msgSingular, msgPlural, args)
                .toBuilder().onHover(TextActions.showText(reports)).build();
        sendReport(actions, trans);
    }

    private static final SimpleDateFormat dateShort = new SimpleDateFormat("yy-MM-dd");
    private static final SimpleDateFormat dateLong = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat timeLong = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat timeShort = new SimpleDateFormat("HH:mm");

    private static final Text RED_SEPARATOR = Text.of(RED, " - ");

    private void sendReport(List<Action> actions, Text trans)
    {
        Action firstAction = actions.get(0);
        Action lastAction = actions.get(actions.size() - 1);

        Text date = getDatePrefix(firstAction, lastAction);
        Text loc = getLocation(firstAction, lastAction);
        if (!date.isEmpty() && !loc.isEmpty())
        {
            lines.add(Text.of(date, WHITE, " ", i18n.translate(cmdSource, NONE, "at"), " ", loc, Text.NEW_LINE, "  ", trans));
        }
        else
        {
            Text prefix = Text.EMPTY;
            if (!date.isEmpty())
            {
                prefix = Text.of(date, RED_SEPARATOR);
            }
            else if (!loc.isEmpty())
            {
                prefix = Text.of(loc, RED_SEPARATOR);
            }
            lines.add(Text.of(prefix, trans));
        }
    }

    private Text getLocation(Action firstAction, Action lastAction)
    {
        if (!lookup.getSettings().isShowLocation())
        {
            return Text.EMPTY;
        }
        if (firstAction == lastAction)
        {
            Location<World> location = Recall.location(firstAction);
            Text.Builder builder = Text.of(GRAY, location.getBlockX(), WHITE, ":", GRAY, location.getBlockY(), WHITE, ":", GRAY, location.getBlockZ()).toBuilder();
            builder.onHover(showText(i18n.translate(cmdSource, NEUTRAL, "Click to teleport to the location in {world}", location.getExtent())))
                   .onClick(executeCallback(c -> showTeleport(location)));
            if (lookup.getSettings().isFullLocation())
            {
                builder.append(Text.of(" ", i18n.translate(cmdSource, NONE, "in"), " ", GRAY, location.getExtent().getName()));
            }
            return builder.build();
        }
        return Text.of("range"); // TODO
    }

    private void showTeleport(Location<World> loc)
    {
        if (cmdSource instanceof Player)
        {
            ((Player)cmdSource).setLocation(loc.add(0.5,0.5,0.5));
        }
        else
        {
            i18n.send(cmdSource, CRITICAL, "Cannot tp non player!");
        }
    }

    private Text getDatePrefix(Action firstAction, Action lastAction)
    {
        if (lookup.getSettings().isNoDate())
        {
            return Text.EMPTY;
        }
        if (firstAction == lastAction)
        {
            Date date = firstAction.getDate();
            String dLong = dateLong.format(date);
            boolean sameDay = dateLong.format(new Date()).equals(dLong);
            String tLong = timeLong.format(date);
            Text full = Text.of(GRAY, dLong, " ", tLong);
            if (lookup.getSettings().isFullDate())
            {
                return full;
            }
            String tShort = timeShort.format(date);
            if (sameDay) // Today?
            {
                return Text.of(GRAY, tShort).toBuilder().onHover(showText(full)).build();
            }
            else
            {
                return Text.of(GRAY, dateShort.format(date), " ", tShort).toBuilder().onHover(showText(full)).build();
            }
        }
        else
        {
            Date firstDate = firstAction.getDate();
            Date lastDate = lastAction.getDate();

            String fdLong = dateLong.format(firstDate);
            String ldLong = dateLong.format(lastDate);
            boolean sameDay = fdLong.equals(ldLong);
            boolean toDay = dateLong.format(new Date()).equals(fdLong);
            String ftLong = timeLong.format(firstDate);
            String ltLong = timeLong.format(lastDate);
            Text fFull = Text.of(GRAY, fdLong, " ", ftLong);
            Text lFull = Text.of(GRAY, ldLong, " ", ltLong);
            if (lookup.getSettings().isFullDate())
            {
                return Text.of(fFull, TextColors.WHITE, " - ", lFull);
            }
            String fdShort = dateShort.format(firstDate);
            String ftShort = timeShort.format(firstDate);
            String ltShort = timeShort.format(lastDate);
            if (sameDay)
            {
                if (toDay)
                {

                    return Text.of(Text.of(GRAY, ftShort).toBuilder().onHover(showText(fFull)).build()
                                ,WHITE, " - ",Text.of(GRAY, ltShort).toBuilder().onHover(showText(lFull)).build());
                }

                return Text.of(Text.of(GRAY, fdShort, " ", ftShort).toBuilder().onHover(showText(fFull)).build()
                              , WHITE, " - " ,Text.of(GRAY, ltShort).toBuilder().onHover(showText(lFull)).build());
            }
            else
            {
                String ldShort = dateShort.format(firstDate);
                return Text.of(Text.of(GRAY, fdShort, " ", ftShort).toBuilder().onHover(showText(fFull)).build()
                             ,  WHITE,    " - "  ,Text.of(GRAY, ldShort, " ", ltShort).toBuilder().onHover(showText(fFull)).build());
            }
        }
    }

    public void sendReports(List<ReportActions> reportActions)
    {
        if (reportActions.isEmpty())
        {
            if (cmdSource instanceof Player)
            {
                i18n.send(ChatTypes.ACTION_BAR, ((Player) cmdSource), NEGATIVE, "Nothing logged here");
                return;
            }
            i18n.send(cmdSource, NEGATIVE, "Nothing logged here");
            return;
        }
        cmdSource.sendMessage(Text.of(TextColors.GOLD, StringUtils.repeat("-", 53)));
        for (ReportActions reportAction : reportActions)
        {
            reportAction.showReport(this);
        }
        Builder builder = Sponge.getGame().getServiceManager().provideUnchecked(PaginationService.class).builder();
        Text titleLineAmount = i18n.translate(cmdSource, POSITIVE, "Showing {amount} Logs", lines.size());
        String titleLineSort = i18n.getTranslation(cmdSource, "(newest first)");
        Text titleLine = Text.of(titleLineAmount, " ", TextColors.YELLOW, titleLineSort);
        Text titleTimings = i18n.translate(cmdSource, NEUTRAL, "Query: {input#time} Report: {input#time}",
                TimeUtil.formatDuration(lookup.timing(Lookup.LookupTiming.LOOKUP)),
                TimeUtil.formatDuration(lookup.timing(Lookup.LookupTiming.REPORT)));
        titleLine = titleLine.toBuilder().onHover(TextActions.showText(titleTimings)).build();
        builder.title(titleLine).padding(Text.of("-"))
                // TODO reverse order
               .contents(lines).linesPerPage(2 + Math.min(lines.size(), 18)).sendTo(cmdSource);
        // TODO remove linesPerPage when Sponge puts the lines to the bottom
    }

    public Locale getLocale()
    {
        return cmdSource.getLocale();
    }

    public Lookup getLookup()
    {
        return lookup;
    }

    public CommandSource getSender()
    {
        return cmdSource;
    }

    public I18n getI18n()
    {
        return i18n;
    }
}
