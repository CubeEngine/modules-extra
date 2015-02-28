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
package de.cubeisland.engine.module.bigdata;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import de.cubeisland.engine.core.module.Module;
import de.cubeisland.engine.core.module.exception.ModuleLoadError;
import de.cubeisland.engine.module.bigdata.MongoDBConfiguration.Authentication;
import de.cubeisland.engine.reflect.codec.mongo.MongoDBCodec;

public class Bigdata extends Module
{
    private MongoClient mongoClient;
    private MongoDBConfiguration config;

    @Override
    public void onLoad()
    {
        this.config = this.loadConfig(MongoDBConfiguration.class);
        try
        {
            getDatabase();
            releaseClient();
        }
        catch (RuntimeException e)
        {
            throw new ModuleLoadError("Failed to connect to the your MongoDB instance!", e);
        }

        this.getCore().getConfigFactory().getCodecManager().registerCodec(new MongoDBCodec());
    }

    public void releaseClient()
    {
        this.mongoClient.close();
    }

    @Override
    public void onDisable()
    {
        this.releaseClient();
    }

    public DB getDatabase()
    {
        if (config.authentication == null)
        {
            config.authentication = new Authentication();
        }
        String db = config.authentication.database;
        return aquireClient(db).getDB(db);
    }

    private MongoClient aquireClient(String db)
    {
        if (mongoClient != null)
        {
            return mongoClient;
        }
        try
        {
            Authentication authConfig = config.authentication;
            ServerAddress address = new ServerAddress(InetAddress.getByName(this.config.host), this.config.port);
            List<MongoCredential> credentialList = Collections.emptyList();
            if (authConfig != null && authConfig.username != null && authConfig.password != null)
            {
                MongoCredential credential = MongoCredential.createMongoCRCredential(authConfig.username, db, authConfig.password.toCharArray());
                credentialList = Arrays.asList(credential);
            }
            MongoClientOptions options = MongoClientOptions.builder().connectTimeout(this.config.connectionTimeout).build();
            mongoClient = new MongoClient(address, credentialList, options);
            // verifies the connection by trying to access it
            mongoClient.getDatabaseNames();
            return mongoClient;
        }
        catch (UnknownHostException e)
        {
            throw new ModuleLoadError("Invalid host", e);
        }
    }
}
