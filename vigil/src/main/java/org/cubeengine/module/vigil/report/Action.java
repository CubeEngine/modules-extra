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
package org.cubeengine.module.vigil.report;

import org.bson.Document;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Action
{
    public static final String DATE = "date";
    public static final String TYPE = "type";
    public static final String DATA = "data";
    private final Document document;

    private Map<String, Object> cached;

    public Action(String type)
    {
        this(new Document());
        document.put(DATE, new Date());
        document.put(TYPE, type);
        document.put(DATA, new HashMap<String, Object>());
    }

    public Action(Document dbObject)
    {
        this.document = dbObject;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getData()
    {
        return document.get(DATA, Map.class);
    }

    @SuppressWarnings("unchecked")
    public <T> T getData(String key)
    {
        return ((T)getData().get(key));
    }

    public void addData(String name, Object value)
    {
        getData().put(name, value);
    }

    public Document getDocument()
    {
        return document;
    }

    public <T> T getCached(String key, Function<Action, T> func)
    {
        if (cached == null)
        {
            cached = new HashMap<>();
        }
        T result = (T)cached.get(key);
        if (result == null)
        {
            result = func.apply(this);
            cached.put(key, result);
        }
        return result;
    }
}
