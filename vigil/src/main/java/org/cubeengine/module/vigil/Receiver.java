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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.libcube.util.TimeUtil;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.Recall;
import org.cubeengine.module.vigil.report.Report;
import org.cubeengine.module.vigil.report.ReportActions;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.pagination.PaginationList.Builder;
import org.spongepowered.api.util.locale.LocaleSource;
import org.spongepowered.api.world.server.ServerLocation;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

public class Receiver
{
    private final Audience cmdSource;
    private final I18n i18n;
    private Lookup lookup;

    private List<Component> lines = new ArrayList<>();

    public Receiver(Audience cmdSource, I18n i18n, Lookup lookup)
    {
        this.cmdSource = cmdSource;
        this.i18n = i18n;
        this.lookup = lookup;
    }

    // TODO translate msgs on this method
    public void sendReport(Report report, List<Action> actions, String msg, Object... args)
    {
        Component reports = Component.text(report.getClass().getSimpleName());
        if (report instanceof Report.ReportGrouping)
        {
            reports = Component.text(((Report.ReportGrouping) report).getReportsList().stream().map(Class::getSimpleName).collect(Collectors.joining("/")));
        }
        Component trans = i18n.translate(cmdSource, NEUTRAL, msg, args).hoverEvent(HoverEvent.showText(reports));
        sendReport(actions, trans);
    }

    public void sendReport(Report report, List<Action> actions, int size, String msgSingular, String msgPlural, Object... args)
    {
        Component reports = Component.text(report.getClass().getSimpleName());
        if (report instanceof Report.ReportGrouping)
        {
            reports = Component.text(((Report.ReportGrouping) report).getReportsList().stream().map(Class::getSimpleName).collect(Collectors.joining("/")));
        }
        Component trans = i18n.translateN(cmdSource, NEUTRAL, size, msgSingular, msgPlural, args).hoverEvent(HoverEvent.showText(reports));
        sendReport(actions, trans);
    }

    private static final SimpleDateFormat dateShort = new SimpleDateFormat("yy-MM-dd");
    private static final SimpleDateFormat dateLong = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat timeLong = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat timeShort = new SimpleDateFormat("HH:mm");

    private static final Component RED_SEPARATOR = Component.text(" - ", NamedTextColor.RED);

    private void sendReport(List<Action> actions, Component trans)
    {
        Action firstAction = actions.get(0);
        Action lastAction = actions.get(actions.size() - 1);

        Component date = lookup.getSettings().isNoDate() ? null : getDatePrefix(firstAction, lastAction);
        Component loc = lookup.getSettings().isShowLocation() ? null : getLocation(actions, firstAction, lastAction);
        if (date != null && loc != null)
        {
            lines.add(
                date.append(Component.space()).append(i18n.translate(cmdSource, "at"))
                    .append(Component.space()).append(loc).append(Component.newline()).append(Component.text("  ")).append(trans));
        }
        else
        {
            Component prefix = Component.empty();
            if (date != null)
            {
                prefix = date.append(RED_SEPARATOR);
            }
            else if (loc != null)
            {
                prefix = loc.append(RED_SEPARATOR);
            }
            lines.add(prefix.append(trans));
        }
    }

    private Component getLocation(List<Action> actions, Action firstAction, Action lastAction)
    {
        final boolean singleAction = firstAction == lastAction;
        boolean singleLocation = true;
        if (!singleAction)
        {
            final ServerLocation firstLoc = Recall.location(firstAction);
            for (Action action : actions)
            {
                final ServerLocation loc = Recall.location(action);
                if (!firstLoc.equals(loc))
                {
                    singleLocation = false;
                    break;
                }
            }
        }
        if (singleAction || singleLocation)
        {
            ServerLocation location = Recall.location(firstAction);
            final Component worldName = location.getWorld().getProperties().displayName().orElse(Component.text(location.getWorldKey().asString()));
            final TextComponent text = Component.join(Component.text(":", NamedTextColor.WHITE), Component.text(location.getBlockX()),
                                                               Component.text(location.getBlockY()), Component.text(location.getBlockZ())).hoverEvent(
                HoverEvent.showText(i18n.translate(cmdSource, NEUTRAL, "Click to teleport to the location in {txt#world}", worldName))).clickEvent(
                SpongeComponents.executeCallback(c -> showTeleport(location)));
            if (lookup.getSettings().isFullLocation())
            {
                return Component.space().append(i18n.translate(cmdSource, "in")).append(Component.space()).append(worldName.color(NamedTextColor.GRAY));
            }
            return text;
        }
        return Component.text("range"); // TODO
    }

    private void showTeleport(ServerLocation loc)
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

    private Component getDatePrefix(Action firstAction, Action lastAction)
    {
        if (firstAction == lastAction)
        {
            Date date = firstAction.getDate();
            String dLong = dateLong.format(date);
            boolean sameDay = dateLong.format(new Date()).equals(dLong);
            String tLong = timeLong.format(date);
            Component full = Component.text(dLong, NamedTextColor.GRAY).append(Component.space()).append(Component.text(tLong));
            if (lookup.getSettings().isFullDate())
            {
                return full;
            }
            String tShort = timeShort.format(date);
            if (sameDay) // Today?
            {
                return Component.text(tShort, NamedTextColor.GRAY).hoverEvent(HoverEvent.showText(full));
            }
            else
            {
                return Component.text(dateShort.format(date), NamedTextColor.GRAY).append(Component.space()).append(Component.text(tShort)).hoverEvent(HoverEvent.showText(full));
            }
        }
        else
        {
            Date firstDate = firstAction.getDate();
            Date lastDate = lastAction.getDate();

            String fdLong = dateLong.format(firstDate);
            String ldLong = dateLong.format(lastDate);
            boolean isSameDay = fdLong.equals(ldLong);
            boolean isToday = dateLong.format(new Date()).equals(fdLong);
            final TextComponent ftLong = Component.text(timeLong.format(firstDate));
            final TextComponent ltLong = Component.text(timeLong.format(lastDate));
            final Component fFull = Component.text(fdLong, NamedTextColor.GRAY).append(Component.space()).append(ftLong);
            final Component lFull = Component.text(ldLong, NamedTextColor.GRAY).append(Component.space()).append(ltLong);
            final TextComponent dash = Component.text(" - ", NamedTextColor.WHITE);
            if (lookup.getSettings().isFullDate())
            {
                return fFull.append(dash).append(lFull);
            }
            final Component fdShort = Component.text(dateShort.format(firstDate), NamedTextColor.GRAY);
            final Component ftShort = Component.text(timeShort.format(firstDate), NamedTextColor.GRAY).hoverEvent(HoverEvent.showText(fFull));
            final Component ltShort = Component.text(timeShort.format(lastDate), NamedTextColor.GRAY).hoverEvent(HoverEvent.showText(lFull));
            if (isSameDay)
            {
                if (isToday)
                {
                    return ftShort.append(dash).append(ltShort);
                }
                return fdShort.append(Component.space()).append(ftShort).append(dash).append(ltShort);
            }
            else
            {
                final Component ldShort = Component.text(dateShort.format(firstDate), NamedTextColor.GRAY);
                return fdShort.append(Component.space()).append(ftShort).append(dash).append(ldShort).append(Component.space()).append(ltShort);
            }
        }
    }

    public void sendReports(List<ReportActions> reportActions)
    {
        if (reportActions.isEmpty())
        {
            if (cmdSource instanceof Player)
            {
                i18n.send(ChatType.ACTION_BAR, ((Player) cmdSource), NEGATIVE, "Nothing logged here");
                return;
            }
            i18n.send(cmdSource, NEGATIVE, "Nothing logged here");
            return;
        }

        cmdSource.sendMessage(Component.text(StringUtils.repeat("-", 53), NamedTextColor.GOLD));
        for (ReportActions reportAction : reportActions)
        {
            reportAction.showReport(this);
        }
        final Builder builder = Sponge.getGame().getServiceProvider().paginationService().builder();
        Component titleLineAmount = i18n.translate(cmdSource, POSITIVE, "Showing {amount} Logs", lines.size());
        String titleLineSort = i18n.getTranslation(cmdSource, "(newest first)");
        Component titleTimings = i18n.translate(cmdSource, NEUTRAL, "Query: {input#time} Report: {input#time}",
                                                TimeUtil.formatDuration(lookup.timing(Lookup.LookupTiming.LOOKUP)),
                                                TimeUtil.formatDuration(lookup.timing(Lookup.LookupTiming.REPORT)));
        final Component titleLine = titleLineAmount.append(Component.space()).append(Component.text(titleLineSort, NamedTextColor.YELLOW)).hoverEvent(HoverEvent.showText(titleTimings));
        builder.title(titleLine).padding(Component.text("-"))
               // TODO reverse order setting
               .contents(lines).linesPerPage(6 + Math.min(lines.size() * 2, 14)).sendTo(cmdSource);
        // TODO remove linesPerPage when Sponge puts the lines to the bottom
    }

    public Locale getLocale()
    {
        if (cmdSource instanceof LocaleSource)
        {
            return ((LocaleSource)cmdSource).getLocale();
        }
        return Locale.getDefault();
    }

    public Lookup getLookup()
    {
        return lookup;
    }

    public Audience getSender()
    {
        return cmdSource;
    }

    public I18n getI18n()
    {
        return i18n;
    }
}
