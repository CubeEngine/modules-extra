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
package org.cubeengine.module.module.kits;

import org.cubeengine.service.database.AsyncRecord;
import org.spongepowered.api.entity.living.player.Player;

public class KitsGiven extends AsyncRecord<KitsGiven>
{
    public KitsGiven()
    {
        super(TableKitsGiven.TABLE_KITS);
    }

    public KitsGiven newKitsGiven(Player player, Kit kit)
    {
        this.setValue(TableKitsGiven.TABLE_KITS.USERID, player.getUniqueId());
        this.setValue(TableKitsGiven.TABLE_KITS.KITNAME, kit.getKitName());
        this.setValue(TableKitsGiven.TABLE_KITS.AMOUNT, 1);
        return this;
    }
}
