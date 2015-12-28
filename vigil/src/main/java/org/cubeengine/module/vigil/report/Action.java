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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class Action
{
    public static final String DATE = "date";
    public static final String TYPE = "type";
    public static final String DATA = "data";
    private final DBObject dbObject;

    private Map<String, Object> cached;

    public Action(String type)
    {
        this(new BasicDBObject());
        dbObject.put(DATE, new Date());
        dbObject.put(TYPE, type);
        dbObject.put(DATA, new HashMap<String, Object>());
    }

    public Action(DBObject dbObject)
    {
        this.dbObject = dbObject;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getData()
    {
        return ((Map<String, Object>)dbObject.get(DATA));
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

    public DBObject getDBObject()
    {
        return dbObject;
    }

    public <T> T getData(String key, Function<Action, T> func)
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
