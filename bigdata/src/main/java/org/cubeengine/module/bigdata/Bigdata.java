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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import javax.inject.Inject;

import com.mongodb.*;
import com.mongodb.client.MongoDatabase;
import de.cubeisland.engine.modularity.core.marker.Disable;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bson.Document;
import org.cubeengine.module.bigdata.MongoDBConfiguration.Authentication;
import org.cubeengine.service.filesystem.FileManager;
import org.slf4j.LoggerFactory;

import static org.apache.logging.log4j.Level.*;

@ModuleInfo(name = "BigData", description = "Provides serialization to a MongoDB")
public class Bigdata extends Module
{
    private MongoClient mongoClient;
    private MongoDBConfiguration config;

    @Inject private FileManager fm;

    @Enable
    public void onLoad()
    {
        this.config = fm.loadConfig(this, MongoDBConfiguration.class);
        try
        {
            ((Logger) LogManager.getLogger("org.mongodb.driver.connection")).setLevel(INFO);
            ((Logger) LogManager.getLogger("org.mongodb.driver.management")).setLevel(INFO);
            ((Logger) LogManager.getLogger("org.mongodb.driver.cluster")).setLevel(INFO);
            ((Logger) LogManager.getLogger("org.mongodb.driver.protocol.insert")).setLevel(INFO);
            ((Logger) LogManager.getLogger("org.mongodb.driver.protocol.query")).setLevel(INFO);
            ((Logger) LogManager.getLogger("org.mongodb.driver.protocol.update")).setLevel(INFO);
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
