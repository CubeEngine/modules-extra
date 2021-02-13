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
package org.cubeengine.module.vigil.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.cubeengine.module.vigil.report.Report;

public class LookupData
{
    private UUID creator;

    private boolean fullDate = false;
    private boolean noDate = false;
    private boolean showLocation = false;
    private boolean fullLocation = false;

    private boolean showDetailedInventory = false;

    private List<String> reports = new ArrayList<>();

    // ------ SETTERS ------

    public LookupData withCreator(UUID uuid)
    {
        this.creator = uuid;
        return this;
    }

    private LookupData withFullDate(boolean set)
    {
        this.fullDate = set;
        return this;
    }

    private LookupData withNoDate(boolean set)
    {
        this.noDate = set;
        return this;
    }

    private LookupData withShowLocation(boolean set)
    {
        this.showLocation = set;
        return this;
    }

    private LookupData withFullLocation(boolean set)
    {
        this.fullLocation = set;
        return this;
    }

    private LookupData withDetailedInventory(boolean set)
    {
        this.showDetailedInventory = set;
        return this;
    }

    public LookupData setReports(List<String> reports)
    {
        this.reports = reports;
        return this;
    }

    public LookupData withReports(Class<? extends Report>... reports)
    {
        this.reports = Arrays.stream(reports).map(Class::getName).collect(Collectors.toList());
        return this;
    }

    // ------ GETTERS ------

    public UUID getCreator()
    {
        return creator;
    }

    public boolean isFullDate()
    {
        return fullDate;
    }

    public boolean isShowLocation()
    {
        return showLocation;
    }

    public boolean isNoDate()
    {
        return noDate;
    }

    public boolean isFullLocation()
    {
        return fullLocation;
    }

    public boolean showDetailedInventory()
    {
        return showDetailedInventory;
    }

    public List<String> filteredReports()
    {
        return this.reports;
    }

    public List<String> getReports()
    {
        return reports;
    }


    public LookupData copy()
    {
        final LookupData copy = new LookupData();
        copy.creator = this.creator;
        copy.fullDate = this.fullDate;
        copy.noDate = this.noDate;
        copy.showLocation = this.showLocation;
        copy.fullLocation = this.fullLocation;
        copy.showDetailedInventory = this.showDetailedInventory;
        copy.reports = this.reports;
        return copy;
    }
}
