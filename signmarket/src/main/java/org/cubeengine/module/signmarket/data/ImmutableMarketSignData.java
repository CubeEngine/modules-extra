package org.cubeengine.module.signmarket.data;

import org.cubeengine.service.data.AbstractImmutableData;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.inventory.ItemStack;

public class ImmutableMarketSignData extends AbstractImmutableData<ImmutableMarketSignData, MarketSignData> implements IMarketSignData
{
    private final SignType type;
    private final User owner;
    private final Integer amount; // to sell or buy
    private final Integer demand;
    private final Double price;
    private final ItemStack item; // quantity is stock
    private final Integer size; // 9 Slot Inventory-Rows

    public ImmutableMarketSignData(IMarketSignData data)
    {
        super(1);
        type = data.getSignType();
        owner = data.getOwner();
        amount = data.getAmount();
        demand = data.getDemand();
        price = data.getPrice();
        item = data.getItem();
        size = data.getSize();
    }

    @Override
    protected void registerGetters()
    {
        registerSingle(SIGN_TYPE, this::getSignType);
        registerSingle(OWNER, this::getOwner);
        registerSingle(AMOUNT, this::getAmount);
        registerSingle(DEMAND, this::getDemand);
        registerSingle(PRICE, this::getPrice);
        registerSingle(ITEM, this::getItem);
        registerSingle(SIZE, this::getSize);
    }

    @Override
    public SignType getSignType()
    {
        return type;
    }

    @Override
    public User getOwner()
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
    public MarketSignData asMutable()
    {
        return new MarketSignData(this);
    }

    @Override
    public int compareTo(ImmutableMarketSignData o)
    {
        return IMarketSignData.compare(this, o);
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
