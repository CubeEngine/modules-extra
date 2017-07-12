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
import java.util.List;
import java.util.UUID;
import org.cubeengine.libcube.util.data.AbstractImmutableData;
import org.spongepowered.api.Sponge;

public class ImmutableLookupData extends AbstractImmutableData<ImmutableLookupData, LookupData> implements ILookupData
{
    private UUID creator;

    private boolean fullDate = false;
    private boolean noDate = false;
    private boolean showLocation = false;
    private boolean fullLocation = false;

    private boolean showDetailedInventory = false;

    private List<String> reports = new ArrayList<>();

    public ImmutableLookupData(ILookupData lookupData)
    {
        super(1);
        this.creator = lookupData.getCreator();
        this.fullDate = lookupData.isFullDate();
        this.noDate = lookupData.isNoDate();
        this.showLocation = lookupData.isShowLocation();
        this.fullLocation = lookupData.isFullLocation();
        this.showDetailedInventory = lookupData.showDetailedInventory();
    }

    // ------ GETTERS ------

    @Override
    public UUID getCreator()
    {
        return this.creator;
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

    // ------ DATA IMPLEMENTATION ------

    @Override
    protected void registerGetters()
    {
        registerSingle(ILookupData.CREATOR, this::getCreator);
        registerSingle(ILookupData.FULLDATE, this::isFullDate);
        registerSingle(ILookupData.SHOWLOC, this::isShowLocation);
        registerSingle(ILookupData.NODATE, this::isNoDate);
        registerSingle(ILookupData.FULLLOC, this::isFullLocation);
        registerSingle(ILookupData.DETAILINV, this::showDetailedInventory);
        registerGetter(ILookupData.REPORTS, this::filteredReports);
        registerValue(ILookupData.REPORTS, () -> Sponge.getRegistry().getValueFactory().createListValue(REPORTS, this.filteredReports()).asImmutable());
    }

    @Override
    public LookupData asMutable()
    {
        return new LookupData(this);
    }

}
