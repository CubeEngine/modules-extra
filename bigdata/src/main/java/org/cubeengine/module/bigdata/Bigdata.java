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
package org.cubeengine.module.bigdata;

import com.google.inject.Singleton;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.module.bigdata.MongoDBConfiguration.Authentication;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Server;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;

import static org.cubeengine.libcube.util.LoggerUtil.setLoggerLevel;

@Singleton
@Module
public class Bigdata
{
    @ModuleConfig private MongoDBConfiguration config;
    private MongoClient mongoClient;

    @Listener
    public void onStarted(StartedEngineEvent<Server> event)
    {
        try
        {
            lessSpamPls();
            getDatabase();
            releaseClient();
        }
        catch (RuntimeException e)
        {
            throw new IllegalStateException("Failed to connect to the your MongoDB instance!", e);
        }
        lessSpamPls();

    }

    public void lessSpamPls()
    {
        setLoggerLevel("org.mongodb.driver.connection", "WARN");
        setLoggerLevel("org.mongodb.driver.management", "WARN");
        setLoggerLevel("org.mongodb.driver.cluster", "WARN");
        setLoggerLevel("org.mongodb.driver.protocol.insert", "WARN");
        setLoggerLevel("org.mongodb.driver.protocol.query", "WARN");
        setLoggerLevel("org.mongodb.driver.protocol.update", "WARN");
        setLoggerLevel("org.mongodb.driver.protocol.command", "WARN");
        setLoggerLevel("org.mongodb.driver.management", "ERROR");
    }

    public void releaseClient()
    {
        try
        {
            this.mongoClient.close();
        }
        finally
        {
            this.mongoClient = null;
        }
    }

    @Listener
    public void onShutdown(StoppingEngineEvent<Server> event)
    {
        this.releaseClient();
    }

    public MongoDatabase getDatabase()
    {
        if (config.authentication == null)
        {
            config.authentication = new Authentication();
        }
        String db = config.authentication.database;
        return acquireClient(db).getDatabase(db);
    }

    private MongoClient acquireClient(String db)
    {
        if (mongoClient != null)
        {
            return mongoClient;
        }
        Authentication authConfig = config.authentication;
//            MongoClientOptions options = MongoClientOptions.builder().connectTimeout(this.config.connectionTimeout).build();
        final ConnectionString connectionString = new ConnectionString("mongodb://" + this.config.host + ":" + this.config.port);
        if (authConfig != null && authConfig.username != null && authConfig.password != null)
        {
            MongoCredential credential = MongoCredential.createCredential(authConfig.username, db, authConfig.password.toCharArray());
            final MongoClientSettings settings = MongoClientSettings.builder().credential(credential).applyConnectionString(connectionString).build();
            mongoClient = MongoClients.create(settings);
        }
        else
        {
            mongoClient = MongoClients.create(connectionString);
        }
        // Check if available by pinging the database...
        mongoClient.getDatabase(db).runCommand(new Document("ping", 1));
        return mongoClient;
    }
}
