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
package org.cubeisland.module.signmarket.storage;

import javax.persistence.Transient;
import de.cubeisland.engine.module.core.CubeEngine;
import org.cubeengine.service.database.AsyncRecord;
import de.cubeisland.engine.module.core.storage.database.AsyncRecord;
import org.cubeengine.service.user.User;
import org.spongepowered.api.world.Location;
import org.jooq.types.UInteger;

import static org.cubeisland.module.signmarket.storage.TableSignBlock.TABLE_SIGN_BLOCK;

public class BlockModel extends AsyncRecord<BlockModel>
{
    @Transient
    private Location location;

    public BlockModel()
    {
        super(TABLE_SIGN_BLOCK);
        this.setValue(TABLE_SIGN_BLOCK.KEY, UInteger.valueOf(0));
    }

    public BlockModel newBlockModel(Location location)
    {
        this.setValue(TABLE_SIGN_BLOCK.WORLD, CubeEngine.getCore().getWorldManager().getWorldId(location.getWorld()));
        this.setValue(TABLE_SIGN_BLOCK.X, location.getBlockX());
        this.setValue(TABLE_SIGN_BLOCK.Y, location.getBlockY());
        this.setValue(TABLE_SIGN_BLOCK.Z, location.getBlockZ());
        return this;
    }

    /**
     * Copies the values from an other BlockModel into this one
     *
     * @param blockInfo the model to copy the values from
     */
    public void copyValuesFrom(BlockModel blockInfo)
    {
        this.setValue(TABLE_SIGN_BLOCK.SIGNTYPE, blockInfo.getValue(TABLE_SIGN_BLOCK.SIGNTYPE));
        this.setValue(TABLE_SIGN_BLOCK.OWNER, blockInfo.getValue(TABLE_SIGN_BLOCK.OWNER));
        this.setValue(TABLE_SIGN_BLOCK.ITEMKEY, blockInfo.getValue(TABLE_SIGN_BLOCK.ITEMKEY));
        this.setValue(TABLE_SIGN_BLOCK.AMOUNT, blockInfo.getValue(TABLE_SIGN_BLOCK.AMOUNT));
        this.setValue(TABLE_SIGN_BLOCK.DEMAND, blockInfo.getValue(TABLE_SIGN_BLOCK.DEMAND));
        this.setValue(TABLE_SIGN_BLOCK.PRICE, blockInfo.getValue(TABLE_SIGN_BLOCK.PRICE));
    }

    /**
     * Returns the location of this sign
     * <p>Do NEVER change this location!
     *
     * @return the location of the sign represented by this model
     */
    public final Location getLocation()
    {
        if (this.location == null)
        {
            this.location = new Location(CubeEngine.getCore().getWorldManager().getWorld(this.getValue(
                TABLE_SIGN_BLOCK.WORLD)), this.getValue(TABLE_SIGN_BLOCK.X), this.getValue(TABLE_SIGN_BLOCK.Y),
                                         this.getValue(TABLE_SIGN_BLOCK.Z));
        }
        return this.location;
    }

    /**
     * Returns true if given user is the owner
     *
     * @param user the user
     * @return whether the user is owner
     */
    public boolean isOwner(User user)
    {
        UInteger owner = this.getValue(TABLE_SIGN_BLOCK.OWNER);
        if (owner == null)
        {
            return user == null;
        }
        return user != null && user.getEntity().getId().equals(owner);
    }
}
