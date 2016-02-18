package org.cubeengine.module.signmarket.data;

import java.util.Optional;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.DataManipulatorBuilder;
import org.spongepowered.api.util.persistence.DataBuilder;
import org.spongepowered.api.util.persistence.InvalidDataException;

public class MarketSignDataBuilder implements DataManipulatorBuilder<MarketSignData, ImmutableMarketSignData>
{
    @Override
    public MarketSignData create()
    {
        return null;
    }

    @Override
    public Optional<MarketSignData> createFrom(DataHolder dataHolder)
    {
        return null;
    }

    @Override
    public Optional<MarketSignData> build(DataView dataView) throws InvalidDataException
    {
        return null;
    }
}
