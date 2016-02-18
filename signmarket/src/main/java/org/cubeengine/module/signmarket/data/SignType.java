package org.cubeengine.module.signmarket.data;

import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.data.MemoryDataContainer;
import org.spongepowered.api.data.Queries;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.KeyFactory;
import org.spongepowered.api.data.value.mutable.Value;

public enum SignType implements DataSerializable
{
    BUY, SELL;

    public static final Key<Value<String>> SIGN_TYPE = KeyFactory.makeSingleKey(String.class, Value.class, DataQuery.of("type"));
    @Override
    public int getContentVersion()
    {
        return 1;
    }

    @Override
    public DataContainer toContainer()
    {
        return new MemoryDataContainer().set(Queries.CONTENT_VERSION, getContentVersion()).set(SIGN_TYPE, name());
    }
}
