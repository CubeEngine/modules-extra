package org.cubeengine.module.signmarket.data;

import java.util.Optional;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.util.persistence.DataBuilder;
import org.spongepowered.api.util.persistence.InvalidDataException;

public class SignTypeBuilder implements DataBuilder<SignType>
{
    @Override
    public Optional<SignType> build(DataView container) throws InvalidDataException
    {
        Optional<String> name = container.getString(SignType.SIGN_TYPE.getQuery());
        if (name.isPresent())
        {
            return Optional.of(SignType.valueOf(name.get()));
        }
        return Optional.empty();
    }
}
