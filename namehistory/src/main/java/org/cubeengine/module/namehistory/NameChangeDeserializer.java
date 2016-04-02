package org.cubeengine.module.namehistory;

import java.lang.reflect.Type;
import java.util.Date;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class NameChangeDeserializer implements JsonDeserializer<NameChange>
{
    @Override
    public NameChange deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
        JsonObject obj = json.getAsJsonObject();
        String name = obj.get("name").getAsString();
        Date changedAt = null;
        if (obj.has("changedToAt"))
        {
            changedAt = new Date(obj.get("changedToAt").getAsLong());
        }
        return new NameChange(name, changedAt);
    }
}