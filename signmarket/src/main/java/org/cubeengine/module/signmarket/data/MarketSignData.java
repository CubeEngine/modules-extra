package org.cubeengine.module.signmarket.data;

import java.util.Optional;
import org.cubeengine.service.data.AbstractData;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.inventory.ItemStack;

public class MarketSignData extends AbstractData<MarketSignData, ImmutableMarketSignData> implements IMarketSignData
{
    private SignType type;
    private User owner;
    private Integer amount; // to sell or buy
    private Integer demand;
    private Double price;
    private ItemStack item; // quantity is stock
    private Integer size; // 9 Slot Inventory-Rows

    public MarketSignData()
    {
        super(1, Sign.class);
    }

    private MarketSignData with(SignType type, User owner, Integer amount, Integer demand, Double price, ItemStack item, Integer size)
    {
        this.type = type;
        this.owner = owner;
        this.amount = amount;
        this.demand = demand;
        this.price = price;
        this.item = item;
        this.size = size;
        return this;
    }

    public MarketSignData(IMarketSignData data)
    {
        super(1);
        with(data.getSignType(), data.getOwner(), data.getAmount(), data.getDemand(), data.getPrice(), data.getItem(), data.getSize());
    }

    @Override
    protected void registerKeys()
    {
        registerSingle(SIGN_TYPE, this::getSignType, this::setSignType);
        registerSingle(OWNER, this::getOwner, this::setOwner);
        registerSingle(AMOUNT, this::getAmount, this::setAmount);
        registerSingle(DEMAND, this::getDemand, this::setDemand);
        registerSingle(PRICE, this::getPrice, this::setPrice);
        registerSingle(ITEM, this::getItem, this::setItem);
        registerSingle(SIZE, this::getSize, this::setSize);
    }

    @Override
    public SignType getSignType()
    {
        return type;
    }

    public void setSignType(SignType type)
    {
        this.type = type;
    }

    @Override
    public User getOwner()
    {
        return owner;
    }

    public void setOwner(User owner)
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

    @Override
    public Integer getSize()
    {
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
                dataholder.get(SIGN_TYPE).orElse(null),
                dataholder.get(OWNER).orElse(null),
                dataholder.get(AMOUNT).orElse(null),
                dataholder.get(DEMAND).orElse(null),
                dataholder.get(PRICE).orElse(null),
                dataholder.get(ITEM).orElse(null),
                dataholder.get(SIZE).orElse(null)));
        if (merged != this)
        {
            this.with(merged.type, merged.owner, merged.amount, merged.demand, merged.price, merged.item, merged.size);
        }
        return Optional.of(this);
    }

    @Override
    public Optional<MarketSignData> from(DataContainer container)
    {
        Optional<SignType> signType = container.getSerializable(SIGN_TYPE.getQuery(), SignType.class);
        Optional<User> owner = container.getSerializable(OWNER.getQuery(), User.class);
        Optional<Integer> amount = container.getInt(AMOUNT.getQuery());
        Optional<Integer> demand = container.getInt(DEMAND.getQuery());
        Optional<Double> price = container.getDouble(PRICE.getQuery());
        Optional<ItemStack> item = container.getSerializable(ITEM.getQuery(), ItemStack.class);
        Optional<Integer> size = container.getInt(SIZE.getQuery());

        if (signType.isPresent() || owner.isPresent() || amount.isPresent() || demand.isPresent() || price.isPresent()
            || item.isPresent() || size.isPresent())
        {
            with(signType.orElse(null), owner.orElse(null), amount.orElse(null), demand.orElse(null), price.orElse(null), item.orElse(null), size.orElse(null));
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
    public int compareTo(MarketSignData o)
    {
        return IMarketSignData.compare(this, o);
    }


}
