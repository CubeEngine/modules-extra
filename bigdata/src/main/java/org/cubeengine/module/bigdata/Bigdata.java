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

import static java.util.Collections.singletonList;
import static org.cubeengine.libcube.util.LoggerUtil.setLoggerLevel;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.module.bigdata.MongoDBConfiguration.Authentication;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameConstructionEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import javax.inject.Singleton;

@Singleton
@Module(id = "bigdata", name = "BigData", version = "1.0.0",
        description = "Provides serialization to a MongoDB",
        dependencies = @Dependency("cubeengine-core"),
        url = "http://cubeengine.org",
        authors = {"Anselm 'Faithcaio' Brehme", "Phillip Schichtel"})
public class Bigdata extends CubeEngineModule
{
    @ModuleConfig private MongoDBConfiguration config;
    private MongoClient mongoClient;

    @Listener
    public void onPreInit(GameConstructionEvent event)
    {
        try
        {
            setLoggerLevel("org.mongodb.driver.connection", "WARN");
            setLoggerLevel("org.mongodb.driver.management", "WARN");
            setLoggerLevel("org.mongodb.driver.cluster", "WARN");
            setLoggerLevel("org.mongodb.driver.protocol.insert", "WARN");
            setLoggerLevel("org.mongodb.driver.protocol.query", "WARN");
            setLoggerLevel("org.mongodb.driver.protocol.update", "WARN");
            setLoggerLevel("org.mongodb.driver.protocol.command", "WARN");
            setLoggerLevel("org.mongodb.driver.management", "ERROR");
            getDatabase();
            releaseClient();
        }
        catch (RuntimeException e)
        {
            throw new IllegalStateException("Failed to connect to the your MongoDB instance!", e);
        }
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
    public void onShutdown(GameStoppingEvent event)
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
        return aquireClient(db).getDatabase(db);
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
                MongoCredential credential = MongoCredential.createCredential(authConfig.username, db, authConfig.password.toCharArray());
                credentialList = singletonList(credential);
            }
            MongoClientOptions options = MongoClientOptions.builder().connectTimeout(this.config.connectionTimeout).build();
            mongoClient = new MongoClient(address, credentialList, options);
            // Check if available by pinging the database...
            mongoClient.getDatabase(db).runCommand(new Document("ping", 1));
            return mongoClient;
        }
        catch (UnknownHostException e)
        {
            throw new IllegalStateException("Invalid host", e);
        }
    }
}
