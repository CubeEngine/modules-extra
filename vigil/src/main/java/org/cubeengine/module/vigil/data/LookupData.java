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

import org.cubeengine.libcube.util.data.AbstractData;
import org.cubeengine.module.vigil.report.Report;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.item.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class LookupData extends AbstractData<LookupData, ImmutableLookupData> implements ILookupData
{
    private UUID creator;

    private boolean fullDate = false;
    private boolean noDate = false;
    private boolean showLocation = false;
    private boolean fullLocation = false;

    private boolean showDetailedInventory = false;

    private List<String> reports = new ArrayList<>();

    public LookupData()
    {
        super(1, ItemStack.class);
    }

    protected LookupData(ILookupData lookupData)
    {
        this();
        this.creator = lookupData.getCreator();
        this.fullDate = lookupData.isFullDate();
        this.noDate = lookupData.isNoDate();
        this.showLocation = lookupData.isShowLocation();
        this.fullLocation = lookupData.isFullLocation();
        this.showDetailedInventory = lookupData.showDetailedInventory();
        this.reports = new ArrayList<>(lookupData.getReports());
    }

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

    @Override
    public UUID getCreator()
    {
        return creator;
    }

    @Override
    public boolean isFullDate()
    {
        return fullDate;
    }

    @Override
    public boolean isShowLocation()
    {
        return showLocation;
    }

    @Override
    public boolean isNoDate()
    {
        return noDate;
    }

    @Override
    public boolean isFullLocation()
    {
        return fullLocation;
    }

    @Override
    public boolean showDetailedInventory()
    {
        return showDetailedInventory;
    }

    @Override
    public List<String> filteredReports()
    {
        return this.reports;
    }

    @Override
    public List<String> getReports()
    {
        return reports;
    }

    // ----- DATA IMPLEMENTATION -----

    @Override
    protected void registerKeys()
    {
        registerSingle(ILookupData.CREATOR, this::getCreator, this::withCreator);
        registerSingle(ILookupData.FULLDATE, this::isFullDate, this::withFullDate);
        registerSingle(ILookupData.SHOWLOC, this::isShowLocation, this::withShowLocation);
        registerSingle(ILookupData.NODATE, this::isNoDate, this::withNoDate);
        registerSingle(ILookupData.FULLLOC, this::isFullLocation, this::withFullLocation);
        registerSingle(ILookupData.DETAILINV, this::showDetailedInventory, this::withDetailedInventory);
        registerGetter(ILookupData.REPORTS, this::filteredReports);
        registerValue(ILookupData.REPORTS, () -> Sponge.getRegistry().getValueFactory().createListValue(REPORTS, this.filteredReports()));
        registerSetter(ILookupData.REPORTS, this::setReports);
    }

    @Override
    public LookupData asMutable()
    {
        return this;
    }

    @Override
    public Optional<LookupData> fill(DataHolder dataHolder, MergeFunction overlap)
    {
        if (!supports(dataHolder))
        {
            return Optional.empty();
        }
        LookupData newData = new LookupData().withCreator(dataHolder.get(CREATOR).get())
                    .withFullDate(dataHolder.get(FULLDATE).orElse(false))
                    .withShowLocation(dataHolder.get(SHOWLOC).orElse(false))
                    .withNoDate(dataHolder.get(NODATE).orElse(false))
                    .withFullLocation(dataHolder.get(FULLLOC).orElse(false))
                    .withDetailedInventory(dataHolder.get(DETAILINV).orElse(false))
                    .setReports(dataHolder.get(REPORTS).orElse(new ArrayList<>()));

        LookupData merged = overlap.merge(this, newData);
        if (merged != this)
        {
            this.withCreator(merged.creator)
                .withFullDate(merged.fullDate)
                .withShowLocation(merged.showLocation)
                .withNoDate(merged.noDate)
                .withFullLocation(merged.fullLocation)
                .withDetailedInventory(merged.showDetailedInventory)
                .setReports(merged.reports);
        }
        return Optional.of(this);
    }

    @Override
    public Optional<LookupData> from(DataContainer container)
    {
        Optional<UUID> creator = container.getObject(CREATOR.getQuery(), UUID.class);
        boolean fullDate = container.getBoolean(FULLDATE.getQuery()).orElse(false);
        boolean noDate = container.getBoolean(SHOWLOC.getQuery()).orElse(false);
        boolean showLocation = container.getBoolean(NODATE.getQuery()).orElse(false);
        boolean fullLocation = container.getBoolean(FULLLOC.getQuery()).orElse(false);
        boolean showDetailedInventory = container.getBoolean(DETAILINV.getQuery()).orElse(false);
        List<String> reports = container.getStringList(REPORTS.getQuery()).orElse(new ArrayList<>());
        if (creator.isPresent())
        {
            this.withCreator(creator.get())
                .withFullDate(fullDate)
                .withNoDate(noDate)
                .withShowLocation(showLocation)
                .withFullLocation(fullLocation)
                .withDetailedInventory(showDetailedInventory)
                .setReports(reports);
            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    public ImmutableLookupData asImmutable()
    {
        return new ImmutableLookupData(this);
    }

    @Override
    public LookupData copy()
    {
        return new LookupData(this);
    }
}
