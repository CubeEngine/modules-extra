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

import java.util.concurrent.TimeUnit;
import com.google.inject.Singleton;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientSettings.Builder;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.connection.ClusterConnectionMode;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.module.bigdata.MongoDBConfiguration.Authentication;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Server;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;

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
            getDatabase();
            releaseClient();
        }
        catch (RuntimeException e)
        {
            throw new IllegalStateException("Failed to connect to the MongoDB instance!", e);
        }
    }

    public void releaseClient()
    {
        try
        {
            if (this.mongoClient != null)
            {
                this.mongoClient.close();
            }
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
        final ConnectionString connectionString = new ConnectionString("mongodb://" + this.config.host + ":" + this.config.port + "/?connectTimeoutMS=" + this.config.connectionTimeout + "&socketTimeoutMS=" + this.config.connectionTimeout);
        final Builder settingsBuilder = MongoClientSettings.builder().applyConnectionString(connectionString)
                                                           .applyToClusterSettings(b -> {
                                                               b.mode(ClusterConnectionMode.SINGLE);
                                                               b.serverSelectionTimeout(1 , TimeUnit.SECONDS);
                                                           })
                                                           .uuidRepresentation(UuidRepresentation.STANDARD);

        if (authConfig != null && authConfig.username != null && authConfig.password != null)
        {
            MongoCredential credential = MongoCredential.createCredential(authConfig.username, db, authConfig.password.toCharArray());
            settingsBuilder.credential(credential);
            mongoClient = MongoClients.create(settingsBuilder.build());
        }
        else
        {
            mongoClient = MongoClients.create(settingsBuilder.build());
        }
        // Check if available by pinging the database...
        mongoClient.getDatabase(db).runCommand(new Document("ping", 1));
        return mongoClient;
    }
}
