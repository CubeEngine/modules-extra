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
package org.cubeengine.module.vigil.storage;

import com.flowpowered.math.vector.Vector3d;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mongodb.QueryOperators.AND;
import static com.mongodb.QueryOperators.OR;
import static org.cubeengine.module.vigil.report.Action.DATA;
import static org.cubeengine.module.vigil.report.Report.*;
import static org.cubeengine.module.vigil.report.block.BlockReport.BLOCK_CHANGES;

public class Query
{
    private final Document query = new Document();
    private List<Object> and = new ArrayList<>();

    public Query()
    {
        query.put(AND, and);
    }

    public FindIterable<Document> find(MongoCollection<Document> collection)
    {
        return collection.find(query);
    }

    public Query world(World world)
    {
        Document block = new Document();
        Document other = new Document();

        block.put(String.join(".", DATA, BLOCK_CHANGES, LOCATION, WORLD.asString("_")), world.getUniqueId().toString());
        other.put(String.join(".", DATA, LOCATION, WORLD.asString("_")), world.getUniqueId().toString());

        and.add(new Document(OR, Arrays.asList(block, other)));
        return this;
    }

    public Query position(Vector3d pos)
    {
        Document block = new Document();
        Document other = new Document();

        block.put(String.join(".", DATA, BLOCK_CHANGES, LOCATION, X.asString("_")), pos.getFloorX());
        block.put(String.join(".", DATA, BLOCK_CHANGES, LOCATION, Y.asString("_")), pos.getFloorY());
        block.put(String.join(".", DATA, BLOCK_CHANGES, LOCATION, Z.asString("_")), pos.getFloorZ());

        other.put(String.join(".", DATA, LOCATION, X.asString("_")), pos.getFloorX());
        other.put(String.join(".", DATA, LOCATION, Y.asString("_")), pos.getFloorY());
        other.put(String.join(".", DATA, LOCATION, Z.asString("_")), pos.getFloorZ());

        and.add(new Document(OR, Arrays.asList(block, other)));
        return this;
    }
}
