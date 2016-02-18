package org.cubeengine.module.signmarket.data;

import java.util.Comparator;
import java.util.function.Function;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.KeyFactory;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Identifiable;

import static org.spongepowered.api.item.inventory.ItemStackComparators.ALL;

public interface IMarketSignData
{
    Key<Value<SignType>> SIGN_TYPE = KeyFactory.makeSingleKey(SignType.class, Value.class, DataQuery.of("signType"));
    Key<Value<User>> OWNER = KeyFactory.makeSingleKey(User.class, Value.class, DataQuery.of("owner"));
    Key<Value<Integer>> AMOUNT = KeyFactory.makeSingleKey(Integer.class, Value.class, DataQuery.of("amount"));
    Key<Value<Integer>> DEMAND = KeyFactory.makeSingleKey(Integer.class, Value.class, DataQuery.of("demand"));
    Key<Value<Double>> PRICE = KeyFactory.makeSingleKey(Double.class, Value.class, DataQuery.of("price"));
    Key<Value<ItemStack>> ITEM = KeyFactory.makeSingleKey(ItemStack.class, Value.class, DataQuery.of("item"));
    Key<Value<Integer>> SIZE = KeyFactory.makeSingleKey(Integer.class, Value.class, DataQuery.of("size"));

    SignType getSignType();
    User getOwner();
    Integer getAmount();
    Integer getDemand();
    Double getPrice();
    ItemStack getItem();
    Integer getSize();

    static int compare(IMarketSignData o1, IMarketSignData o2)
    {
        int val = cp(o1.getSignType(), o2.getSignType());
        if (val != 0)
        {
            return val;
        }
        val = cp(o1.getOwner(), o2.getOwner(), Identifiable::getUniqueId);
        if (val != 0)
        {
            return val;
        }
        val = cp(o1.getAmount(), o2.getAmount());
        if (val != 0)
        {
            return val;
        }
        val = cp(o1.getDemand(), o2.getDemand());
        if (val != 0)
        {
            return val;
        }
        val = cp(o1.getPrice(), o2.getPrice());
        if (val != 0)
        {
            return val;
        }

        val = cp(o1.getItem(), o2.getItem(), ALL);
        if (val != 0)
        {
            return val;
        }
        val = cp(o1.getSize(), o2.getSize());
        return val;
    }

    static <T extends Comparable<T>> int cp(T a, T b)
    {
        return a == null ? (b == null ? 0 : -1) : (b == null ? 1 : a.compareTo(b));
    }

    static <A, B extends Comparable<B>> int cp(A a, A b, Function<A, B> func)
    {
        return a == null ? (b == null ? 0 : -1) : (b == null ? 1 : func.apply(a).compareTo(func.apply(b)));
    }

    static <T> int cp(T a, T b, Comparator<T> comparator)
    {
        return a == null ? (b == null ? 0 : -1) : (b == null ? 1 : comparator.compare(a, b));
    }
}
