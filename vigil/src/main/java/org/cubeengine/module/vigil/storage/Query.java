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
package org.cubeengine.module.vigil.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.mongodb.QueryOperators;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.Report;
import org.cubeengine.module.vigil.report.entity.EntityReport;
import org.spongepowered.api.data.manipulator.mutable.RepresentedItemData;
import org.spongepowered.api.world.World;

import static com.mongodb.QueryOperators.AND;
import static com.mongodb.QueryOperators.IN;
import static com.mongodb.QueryOperators.OR;
import static org.cubeengine.module.vigil.report.Action.DATA;
import static org.cubeengine.module.vigil.report.Action.TYPE;
import static org.cubeengine.module.vigil.report.Report.*;
import static org.cubeengine.module.vigil.report.block.BlockReport.BLOCK_CHANGES;
import static org.cubeengine.module.vigil.report.entity.EntityReport.ENTITY;
import static org.cubeengine.module.vigil.report.entity.EntityReport.ENTITY_DATA;

import javax.print.Doc;

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

    public Query world(UUID world)
    {
        Document block = new Document();
        Document other = new Document();

        block.put(String.join(".", DATA.name, BLOCK_CHANGES.name, LOCATION, WORLD.asString("_")), world.toString());
        other.put(String.join(".", DATA.name, LOCATION, WORLD.asString("_")), world.toString());

        and.add(new Document(OR, Arrays.asList(block, other)));
        return this;
    }

    public Query position(Vector3i pos)
    {
        Document position = new Document();

        position.put(String.join(".", DATA.name, LOCATION, X.asString("_")), pos.getX());
        position.put(String.join(".", DATA.name, LOCATION, Y.asString("_")), pos.getY());
        position.put(String.join(".", DATA.name, LOCATION, Z.asString("_")), pos.getZ());

        and.add(position);
        return this;
    }

    public Query radius(Vector3i pos, int radius)
    {
        Document posRadius = new Document();

        Document cond = new Document();
        cond.put(QueryOperators.GT, pos.getX() - radius);
        cond.put(QueryOperators.LT, pos.getX() + radius);

        posRadius.put(String.join(".", DATA.name, LOCATION, X.asString("_")), cond);

        cond = new Document();
        cond.put(QueryOperators.GT, pos.getZ() - radius);
        cond.put(QueryOperators.LT, pos.getZ() + radius);

        posRadius.put(String.join(".", DATA.name, LOCATION, Z.asString("_")), cond);

        and.add(posRadius);
        return this;
    }

    public Query reportFilters(List<String> reports)
    {
        Document types = new Document();
        types.put(TYPE.name, new Document(IN, reports));
        and.add(types);
        return this;
    }
}
