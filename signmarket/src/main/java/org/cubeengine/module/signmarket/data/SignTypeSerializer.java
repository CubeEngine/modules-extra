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
import com.google.common.reflect.TypeToken;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.MemoryDataContainer;
import org.spongepowered.api.data.Queries;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.KeyFactory;
import org.spongepowered.api.data.persistence.DataBuilder;
import org.spongepowered.api.data.persistence.DataSerializer;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.value.mutable.Value;

public class SignTypeSerializer implements DataSerializer<SignType>
{
    public static final Key<Value<String>> SIGN_TYPE = KeyFactory.makeSingleKey(String.class, Value.class, DataQuery.of("type"));

    private final TypeToken<SignType> token = TypeToken.of(SignType.class);

    @Override
    public TypeToken<SignType> getToken()
    {
        return token;
    }

    @Override
    public Optional<SignType> deserialize(DataView view) throws InvalidDataException
    {
        Optional<String> name = view.getString(SIGN_TYPE.getQuery());
        if (name.isPresent())
        {
            return Optional.of(SignType.valueOf(name.get()));
        }
        return Optional.empty();
    }

    @Override
    public DataContainer serialize(SignType obj) throws InvalidDataException
    {
        return new MemoryDataContainer().set(SIGN_TYPE, obj.name());
    }
}
