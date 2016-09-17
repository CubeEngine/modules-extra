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

import static org.spongepowered.api.data.DataQuery.of;

import java.util.Optional;
import com.google.common.reflect.TypeToken;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.MemoryDataContainer;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.KeyFactory;
import org.spongepowered.api.data.persistence.DataTranslator;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.value.mutable.Value;

public class SignTypeSerializer implements DataTranslator<SignType>
{
    public static final Key<Value<String>> SIGN_TYPE = KeyFactory.makeSingleKey(
            new TypeToken<String>() {}, new TypeToken<Value<String>>() {},
            of("type"), "cubeengine:signmarket:signtype", "Serializer");
    private final TypeToken<SignType> token = TypeToken.of(SignType.class);

    @Override
    public String getId()
    {
        return "cubeengine:signmarket:signtype";
    }

    @Override
    public String getName()
    {
        return "SignType Translator";
    }

    @Override
    public TypeToken<SignType> getToken()
    {
        return token;
    }

    @Override
    public SignType translate(DataView view) throws InvalidDataException
    {
        Optional<String> name = view.getString(SIGN_TYPE.getQuery());
        if (name.isPresent())
        {
            return SignType.valueOf(name.get());
        }
        throw new InvalidDataException("Missing SignType Data");
    }

    @Override
    public DataContainer translate(SignType obj) throws InvalidDataException
    {
        return new MemoryDataContainer().set(SIGN_TYPE, obj.name());
    }
}
