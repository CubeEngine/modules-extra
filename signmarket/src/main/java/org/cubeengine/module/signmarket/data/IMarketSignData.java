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
package org.cubeengine.module.signmarket.data;

import java.util.UUID;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.KeyFactory;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.item.inventory.ItemStack;

public interface IMarketSignData
{
    UUID ADMIN_SIGN = UUID.nameUUIDFromBytes("ADMIN".getBytes());

    Key<Value<UUID>> ID = KeyFactory.makeSingleKey(UUID.class, Value.class, DataQuery.of("id"));
    Key<Value<SignType>> SIGN_TYPE = KeyFactory.makeSingleKey(SignType.class, Value.class, DataQuery.of("signType"));
    Key<Value<UUID>> OWNER = KeyFactory.makeSingleKey(UUID.class, Value.class, DataQuery.of("owner"));
    Key<Value<Integer>> AMOUNT = KeyFactory.makeSingleKey(Integer.class, Value.class, DataQuery.of("amount"));
    Key<Value<Integer>> DEMAND = KeyFactory.makeSingleKey(Integer.class, Value.class, DataQuery.of("demand"));
    Key<Value<Double>> PRICE = KeyFactory.makeSingleKey(Double.class, Value.class, DataQuery.of("price"));
    Key<Value<ItemStack>> ITEM = KeyFactory.makeSingleKey(ItemStack.class, Value.class, DataQuery.of("item"));
    Key<Value<Integer>> STOCK = KeyFactory.makeSingleKey(Integer.class, Value.class, DataQuery.of("stock"));
    Key<Value<Integer>> SIZE = KeyFactory.makeSingleKey(Integer.class, Value.class, DataQuery.of("size"));

    UUID getID();
    SignType getSignType();
    UUID getOwner();
    Integer getAmount();
    Integer getDemand();
    Double getPrice();
    ItemStack getItem();
    Integer getStock();
    Integer getSize();

    static int compare(IMarketSignData o1, IMarketSignData o2)
    {
        return o1.getID().compareTo(o2.getID());
    }

    MarketSignData asMutable();
}
