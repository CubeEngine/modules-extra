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

import java.util.Optional;
import java.util.UUID;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import org.cubeengine.libcube.util.data.AbstractData;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.item.inventory.ItemStack;

import static org.spongepowered.api.item.inventory.ItemStackComparators.ITEM_DATA;
import static org.spongepowered.api.item.inventory.ItemStackComparators.PROPERTIES;
import static org.spongepowered.api.item.inventory.ItemStackComparators.TYPE;

public class MarketSignData extends AbstractData<MarketSignData, ImmutableMarketSignData> implements IMarketSignData
{
    public static final Ordering<ItemStack> ITEM_NOSIZE_COMPARE = Ordering.compound(ImmutableList.of(TYPE, PROPERTIES, ITEM_DATA));

    private UUID id;
    private SignType type;
    private UUID owner; // 90053eee-d10b-7a55-5d33-570d7901474c
    private Integer amount; // to sell or buy
    private Integer demand;
    private Double price;
    private ItemStack item;
    private Integer stock; // amount in stock
    private Integer size; // 9 Slot Inventory-Rows

    public MarketSignData()
    {
        super(1, Sign.class);
    }

    private MarketSignData with(UUID id, SignType type, UUID owner, Integer amount, Integer demand, Double price, ItemStack item, Integer stock, Integer size)
    {
        this.id = id;
        this.type = type;
        this.owner = owner;
        this.amount = amount;
        this.demand = demand;
        this.price = price;
        this.item = item;
        this.size = size;
        this.stock = stock;
        return this;
    }

    public MarketSignData(IMarketSignData data)
    {
        super(1);
        with(data.getID(), data.getSignType(), data.getOwner(), data.getAmount(), data.getDemand(), data.getPrice(), data.getItem(), data.getStock(), data.getSize());
    }

    @Override
    protected void registerKeys()
    {
        registerSingle(ID, this::getID, this::setID);
        registerSingle(SIGN_TYPE, this::getSignType, this::setSignType);
        registerSingle(OWNER, this::getOwner, this::setOwner);
        registerSingle(AMOUNT, this::getAmount, this::setAmount);
        registerSingle(DEMAND, this::getDemand, this::setDemand);
        registerSingle(PRICE, this::getPrice, this::setPrice);
        registerSingle(ITEM, this::getItem, this::setItem);
        registerSingle(STOCK, this::getStock, this::setStock);
        registerSingle(SIZE, this::getSize, this::setSize);
    }

    @Override
    public UUID getID()
    {
        return id;
    }

    public void setID(UUID id)
    {
        this.id = id;
    }

    @Override
    public SignType getSignType()
    {
        return type;
    }

    public void setSignType(SignType type)
    {
        this.type = type;
        if (type == SignType.BUY)
        {
            this.demand = null;
        }
        if (!isAdminOwner() && getStock() == null)
        {
            setStock(0);
        }
    }

    @Override
    public UUID getOwner()
    {
        return owner;
    }

    public void setOwner(UUID owner)
    {
        this.owner = owner;
    }

    @Override
    public Integer getAmount()
    {
        return amount;
    }

    public void setAmount(Integer amount)
    {
        this.amount = amount;
    }

    @Override
    public Integer getDemand()
    {
        return demand;
    }

    public void setDemand(Integer demand)
    {
        this.demand = demand;
    }

    @Override
    public Double getPrice()
    {
        return price;
    }

    public void setPrice(Double price)
    {
        this.price = price;
    }

    @Override
    public ItemStack getItem()
    {
        return item;
    }

    public void setItem(ItemStack item)
    {
        this.item = item;
    }

    public void setStock(Integer stock)
    {
        this.stock = stock;
    }

    @Override
    public Integer getStock()
    {
        return stock;
    }

    @Override
    public Integer getSize()
    {
        if (size == null)
        {
            size = 6;
        }
        return size;
    }

    public void setSize(Integer size)
    {
        this.size = size;
    }

    @Override
    public Optional<MarketSignData> fill(DataHolder dataholder, MergeFunction mergeFunction)
    {
        if (!supports(dataholder))
        {
            return Optional.empty();
        }
        MarketSignData merged = mergeFunction.merge(this,
            new MarketSignData().with(
                dataholder.get(ID).orElse(null),
                dataholder.get(SIGN_TYPE).orElse(null),
                dataholder.get(OWNER).orElse(null),
                dataholder.get(AMOUNT).orElse(null),
                dataholder.get(DEMAND).orElse(null),
                dataholder.get(PRICE).orElse(null),
                dataholder.get(ITEM).orElse(null),
                dataholder.get(STOCK).orElse(null),
                dataholder.get(SIZE).orElse(null)));
        if (merged != this)
        {
            this.with(merged.id, merged.type, merged.owner, merged.amount, merged.demand, merged.price, merged.item, merged.stock, merged.size);
        }
        return Optional.of(this);
    }

    @Override
    public Optional<MarketSignData> from(DataContainer container)
    {
        Optional<UUID> id = container.getObject(ID.getQuery(), UUID.class);
        Optional<SignType> signType = container.getObject(SIGN_TYPE.getQuery(), SignType.class);
        Optional<UUID> owner = container.getObject(OWNER.getQuery(), UUID.class);
        Optional<Integer> amount = container.getInt(AMOUNT.getQuery());
        Optional<Integer> demand = container.getInt(DEMAND.getQuery());
        Optional<Double> price = container.getDouble(PRICE.getQuery());
        Optional<ItemStack> item = container.getSerializable(ITEM.getQuery(), ItemStack.class);
        Optional<Integer> stock = container.getInt(STOCK.getQuery());
        Optional<Integer> size = container.getInt(SIZE.getQuery());

        if (signType.isPresent() || owner.isPresent() || amount.isPresent() || demand.isPresent() || price.isPresent()
            || item.isPresent() || size.isPresent())
        {
            with(id.orElse(null),
                 signType.orElse(null),
                 owner.orElse(null),
                 amount.orElse(null),
                 demand.orElse(null),
                 price.orElse(null),
                 item.orElse(null),
                 stock.orElse(null),
                 size.orElse(null));
            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    public MarketSignData copy()
    {
        return new MarketSignData(this);
    }

    @Override
    public ImmutableMarketSignData asImmutable()
    {
        return new ImmutableMarketSignData(this);
    }

    @Override
    public MarketSignData asMutable()
    {
        return this;
    }

    // Convenience Methods

    public void setItem(ItemStack item, boolean setAmount)
    {
        this.setItem(item);
        if (setAmount)
        {
            this.setAmount(item.getQuantity());
        }
    }

    public void setAdminOwner()
    {
        this.setOwner(IMarketSignData.ADMIN_SIGN);
        this.setDemand(null);
    }

    public boolean isAdminOwner()
    {
        return getOwner().equals(IMarketSignData.ADMIN_SIGN);
    }

    public boolean isOwner(UUID uuid)
    {
        return getOwner().equals(uuid);
    }

    public boolean isSatisfied()
    {
        if (getStock() == null || getDemand() == null)
        {
            return false;
        }

        return getStock() >= getDemand();
    }

    public Integer getMax()
    {
        if (getDemand() != null)
        {
            return getDemand();
        }
        if (getSize() == -1)
        {
            return -1;
        }
        return getItem().getMaxStackQuantity() * getSize() * 9;
    }

    public boolean isItem(ItemStack itemStack)
    {
        return ITEM_NOSIZE_COMPARE.compare(getItem(), itemStack) == 0;
    }
}
