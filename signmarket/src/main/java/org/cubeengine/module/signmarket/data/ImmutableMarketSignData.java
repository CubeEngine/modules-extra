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
import org.cubeengine.libcube.util.data.AbstractImmutableData;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.item.inventory.ItemStack;

public class ImmutableMarketSignData extends AbstractImmutableData<ImmutableMarketSignData, MarketSignData> implements IMarketSignData
{
    private final UUID id;
    private final SignType type;
    private final UUID owner;
    private final Integer amount; // to sell or buy
    private final Integer demand;
    private final Double price;
    private final ItemStack item; // quantity
    private final Integer stock; // amount in stock
    private final Integer size; // 9 Slot Inventory-Rows

    public ImmutableMarketSignData(IMarketSignData data)
    {
        super(2);
        id = data.getID();
        type = data.getSignType();
        owner = data.getOwner();
        amount = data.getAmount();
        demand = data.getDemand();
        price = data.getPrice();
        item = data.getItem();
        stock = data.getStock();
        size = data.getSize();
    }

    @Override
    protected void registerGetters()
    {
        registerSingle(ID, this::getID);
        registerSingle(SIGN_TYPE, this::getSignType);
        registerSingle(OWNER, this::getOwner);
        registerSingle(AMOUNT, this::getAmount);
        registerSingle(DEMAND, this::getDemand);
        registerSingle(PRICE, this::getPrice);
        registerSingle(ITEM, this::getItem);
        registerSingle(SIZE, this::getSize);
    }

    @Override
    public UUID getID()
    {
        return id;
    }

    @Override
    public SignType getSignType()
    {
        return type;
    }

    @Override
    public UUID getOwner()
    {
        return owner;
    }

    @Override
    public Integer getAmount()
    {
        return amount;
    }

    @Override
    public Integer getDemand()
    {
        return demand;
    }

    @Override
    public Double getPrice()
    {
        return price;
    }

    @Override
    public ItemStack getItem()
    {
        return item;
    }

    @Override
    public Integer getSize()
    {
        return size;
    }

    @Override
    public Integer getStock()
    {
        return stock;
    }

    @Override
    public MarketSignData asMutable()
    {
        return new MarketSignData(this);
    }

    @Override
    public int getContentVersion()
    {
        return 1;
    }

    @Override
    public DataContainer toContainer()
    {
        return super.toContainer();
    }
}
