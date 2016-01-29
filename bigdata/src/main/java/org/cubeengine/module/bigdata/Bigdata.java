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
package org.cubeengine.module.bigdata;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Disable;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bson.Document;
import org.cubeengine.module.bigdata.MongoDBConfiguration.Authentication;
import org.cubeengine.service.filesystem.FileManager;
import org.cubeengine.service.filesystem.ModuleConfig;

import javax.inject.Inject;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessControlException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.apache.logging.log4j.Level.ERROR;
import static org.apache.logging.log4j.Level.WARN;

@ModuleInfo(name = "BigData", description = "Provides serialization to a MongoDB")
public class Bigdata extends Module
{
    @ModuleConfig private MongoDBConfiguration config;
    private MongoClient mongoClient;

    @Enable
    public void onEnable()
    {
        try
        {
            ((Logger) LogManager.getLogger("org.mongodb.driver.connection")).setLevel(WARN);
            ((Logger) LogManager.getLogger("org.mongodb.driver.management")).setLevel(WARN);
            ((Logger) LogManager.getLogger("org.mongodb.driver.cluster")).setLevel(WARN);
            ((Logger) LogManager.getLogger("org.mongodb.driver.protocol.insert")).setLevel(WARN);
            ((Logger) LogManager.getLogger("org.mongodb.driver.protocol.query")).setLevel(WARN);
            ((Logger) LogManager.getLogger("org.mongodb.driver.protocol.update")).setLevel(WARN);
            ((Logger) LogManager.getLogger("org.mongodb.driver.protocol.command")).setLevel(WARN);
            ((Logger) LogManager.getLogger("org.mongodb.driver.management")).setLevel(ERROR);
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

    @Disable
    public void onDisable()
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
                MongoCredential credential = MongoCredential.createMongoCRCredential(authConfig.username, db, authConfig.password.toCharArray());
                credentialList = Arrays.asList(credential);
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
