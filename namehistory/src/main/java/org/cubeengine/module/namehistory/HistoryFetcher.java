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
package org.cubeengine.module.namehistory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class HistoryFetcher
{
    private final static String URL_ROOT = "https://api.mojang.com";
    private final static String URL_PROFILE_NAMES = "/user/profiles/%s/names";
    private final static Gson GSON;
    public static final ExecutorService EXECUTOR;

    static
    {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(NameChange.class, new NameChangeDeserializer());
        GSON = builder.create();
        EXECUTOR = Executors.newFixedThreadPool(1);
    }

    public static CompletableFuture<Optional<HistoryData>> get(final UUID key)
    {
        return CompletableFuture.supplyAsync(() -> getHistory(key));
    }

    private static Optional<HistoryData> getHistory(UUID uuid)
    {
        try
        {
            InputStream inputStream = new URL(
                URL_ROOT + String.format(URL_PROFILE_NAMES, convert(uuid))).openConnection().getInputStream();
            NameChange[] names = GSON.fromJson(new InputStreamReader(inputStream), NameChange[].class);
            if (names != null)
            {
                return Optional.of(new HistoryData(uuid, names));
            }
            return Optional.empty();
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
    }

    public static String convert(UUID uuid)
    {
        return uuid.toString().replace("-", "");
    }
}
