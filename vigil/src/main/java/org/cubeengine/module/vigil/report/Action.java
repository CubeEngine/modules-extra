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
package org.cubeengine.module.vigil.report;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.bson.Document;


public class Action
{
    public static class DataKey<T> {
        public final String name;

        public DataKey(String name) {
            this.name = name;
        }

        @SuppressWarnings("unchecked")
        public T get(Map<String, Object> store) {
            return (T)store.get(name);
        }

        public void put(Map<String, Object> store, T value) {
            store.put(name, value);
        }
    }

    public static final DataKey<Date> DATE = new DataKey<>("date");
    public static final DataKey<String> TYPE = new DataKey<>("type");
    public static final DataKey<Map<String, Object>> DATA = new DataKey<>("data");

    private final Document document;
    private Map<String, Object> cached;

    public Action(String type)
    {
        this(new Document());
        DATE.put(document, new Date(Instant.now().toEpochMilli()));
        TYPE.put(document, type);
        DATA.put(document, new HashMap<>());
    }

    public Action(Document dbObject)
    {
        this.document = dbObject;
    }

    public String getType() {
        return TYPE.get(document);
    }

    public Date getDate() {
        return DATE.get(document);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getData()
    {
        return document.get(DATA.name, Map.class);
    }

    @SuppressWarnings("unchecked")
    public <T> T getData(String key)
    {
        return ((T)getData().get(key));
    }

    public <T> T getData(DataKey<T> key)
    {
        return key.get(getData());
    }

    public void addData(String name, Object value)
    {
        getData().put(name, value);
    }

    public <T> void addData(DataKey<T> key, T value)
    {
        key.put(getData(), value);
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

    public <T> T getCached(DataKey<T> key, Function<Action, T> func)
    {
        if (cached == null)
        {
            cached = new HashMap<>();
        }
        T result = key.get(cached);
        if (result == null)
        {
            result = func.apply(this);
            key.put(cached, result);
        }
        return result;
    }
}
