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
package org.cubeengine.module.signmarket;

import org.cubeengine.service.permission.PermissionContainer;
import org.spongepowered.api.service.permission.PermissionDescription;

@SuppressWarnings("all")
public class MarketSignPerm extends PermissionContainer<Signmarket>
{
    public MarketSignPerm(Signmarket module, SignMarketCommands smCmds)
    {
        super(module);
    }

    public final PermissionDescription INTERACT = register("interact", "Allows buy and sell interactions with MarketSigns", null);
    public final PermissionDescription INTERACT_SELL = register("sell", "Allows selling to MarketSigns", INTERACT);
    public final PermissionDescription INTERACT_BUY = register("buy", "Allows buying from MarketSigns", INTERACT);
    public final PermissionDescription INTERACT_INVENTORY = register("inventory", "Allows seeing a MarketSigns Inventory", INTERACT);

    public final PermissionDescription EDIT = register("edit", "Allows creating and modifying all MarketSigns", null);
    public final PermissionDescription EDIT_ADMIN = register("admin", "Allows creating and modifying Admin MarketSigns", EDIT);
    public final PermissionDescription EDIT_PLAYER = register("player", "Allows creating and modifying all Player MarketSigns", EDIT);
    public final PermissionDescription EDIT_PLAYER_SELF = register("self", "Allows creating and modifying your own Player MarketSigns", EDIT_PLAYER);
    public final PermissionDescription EDIT_PLAYER_OTHER = register("other", "Allows creating and modifying Player MarketSigns of other players", EDIT_PLAYER);

    public final PermissionDescription EDIT_USE = register("use", "Allows creating and modifying MarketSigns", EDIT, EDIT_PLAYER, EDIT_PLAYER_SELF, EDIT_PLAYER_OTHER);
}
