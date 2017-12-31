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
package org.cubeengine.module.signmarket.data;

import static org.spongepowered.api.data.DataQuery.of;
import static org.spongepowered.api.data.key.KeyFactory.makeSingleKey;

import java.util.UUID;

import com.google.common.reflect.TypeToken;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.item.inventory.ItemStack;

public interface IMarketSignData
{
    UUID ADMIN_SIGN = UUID.nameUUIDFromBytes("ADMIN".getBytes());

    TypeToken<UUID> TT_UUID = new TypeToken<UUID>() {};
    TypeToken<SignType> TT_SignType = new TypeToken<SignType>() {};
    TypeToken<Value<UUID>> TTV_UUID = new TypeToken<Value<UUID>>() {};
    TypeToken<Value<SignType>> TTV_SignType = new TypeToken<Value<SignType>>() {};
    TypeToken<Integer> TT_Int = new TypeToken<Integer>() {};
    TypeToken<Value<Integer>> TTV_Int = new TypeToken<Value<Integer>>() {};
    TypeToken<Double> TT_Double = new TypeToken<Double>() {};
    TypeToken<Value<Double>> TTV_Double = new TypeToken<Value<Double>>() {};
    TypeToken<ItemStack> TT_ItemStack = new TypeToken<ItemStack>() {};
    TypeToken<Value<ItemStack>> TTV_ItemStack = new TypeToken<Value<ItemStack>>() {};

    Key<Value<UUID>> ID = Key.builder().type(TTV_UUID).id("cubeengine-signmarket:signdata-id").name("ID").query(of("id")).build();
    Key<Value<SignType>> SIGN_TYPE = Key.builder().type(TTV_SignType).id("cubeengine-signmarket:signdata-type").name("SignType").query(of("signType")).build();
    Key<Value<UUID>> OWNER = Key.builder().type(TTV_UUID).id("cubeengine-signmarket:signdata-owner").name("Owner").query(of("owner")).build();
    Key<Value<Integer>> AMOUNT = Key.builder().type(TTV_Int).id("cubeengine-signmarket:signdata-amount").name("Amount").query(of("amount")).build();
    Key<Value<Integer>> DEMAND = makeSingleKey(TT_Int, TTV_Int, of("demand"), "cubeengine-signmarket:signdata-demand", "Demand");
    Key<Value<Double>> PRICE = makeSingleKey(TT_Double, TTV_Double, of("price"), "cubeengine-signmarket:signdata-price", "Price");
    Key<Value<ItemStack>> ITEM = makeSingleKey(TT_ItemStack, TTV_ItemStack, of("item"), "cubeengine-signmarket:signdata-item", "Item");
    Key<Value<Integer>> STOCK = makeSingleKey(TT_Int, TTV_Int, of("stock"), "cubeengine-signmarket:signdata-stock", "Stock");
    Key<Value<Integer>> SIZE = makeSingleKey(TT_Int, TTV_Int, of("size"), "cubeengine-signmarket:signdata-size", "Size");

    UUID getID();
    SignType getSignType();
    UUID getOwner();
    Integer getAmount();
    Integer getDemand();
    Double getPrice();
    ItemStack getItem();
    Integer getStock();
    Integer getSize();

    MarketSignData asMutable();
}
