/*
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
package org.cubeengine.module.apiserver;

import java.util.List;
import java.util.Map;
import com.google.common.base.Preconditions;
import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.provider.Providers;

public class Parameters
{
    private final Map<String, List<String>> data;
    private final Providers provider;

    public Parameters(Map<String, List<String>> data, Providers provider)
    {
        this.data = data;
        this.provider = provider;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String name, int index, Class<T> type)
    {
        List<String> values = this.data.get(name);
        if (values == null)
        {
            return null;
        }
        String value = values.get(index);
        if (type != String.class)
        {
            return (T) provider.read(type, type, new CommandInvocation(null, value, provider)); // TODO fix ME
        }
        return (T)value;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String name, int index, T def)
    {
        Preconditions.checkNotNull(name, "The default value must not be null!");

        T value = (T)this.get(name, index, def.getClass());
        if (value == null)
        {
            return def;
        }
        return value;
    }

    public <T> T get(String name, Class<T> type)
    {
        return this.get(name, 0, type);
    }

    public <T> T get(String name, T def)
    {
        return this.get(name, 0, def);
    }

    public String getString(String name, int index)
    {
        return this.get(name, index, String.class);
    }

    public String getString(String name, int index, String def)
    {
        String value = this.getString(name, index);
        if (value == null)
        {
            return def;
        }
        return value;
    }

    public String getString(String name)
    {
        return this.getString(name, 0);
    }

    public String getString(String name, String def)
    {
        return this.getString(name, 0, def);
    }
}
