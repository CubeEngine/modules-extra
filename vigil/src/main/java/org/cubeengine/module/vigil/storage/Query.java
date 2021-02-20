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
import java.util.List;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.math.vector.Vector3i;

import static org.cubeengine.module.vigil.report.Action.DATA;
import static org.cubeengine.module.vigil.report.Action.TYPE;
import static org.cubeengine.module.vigil.report.Report.*;
import static org.cubeengine.module.vigil.report.block.BlockReport.BLOCK_CHANGES;

public class Query
{
    private List<Bson> andFilters = new ArrayList<>();

    public FindIterable<Document> find(MongoCollection<Document> collection)
    {
        return collection.find(Filters.and(andFilters));
    }

    public Query world(ResourceKey world)
    {
        final Bson block = Filters.eq(String.join(".", DATA.name, BLOCK_CHANGES.name, LOCATION, WORLD.asString("_")), world.asString());
        final Bson other = Filters.eq(String.join(".", DATA.name, LOCATION, WORLD.asString("_")), world.asString());
        andFilters.add(Filters.or(block, other));
        return this;
    }

    public Query position(Vector3i pos)
    {
        final Bson blockX = Filters.eq(String.join(".", DATA.name, LOCATION, X.asString("_")), pos.getX());
        final Bson blockY = Filters.eq(String.join(".", DATA.name, LOCATION, Y.asString("_")), pos.getY());
        final Bson blockZ = Filters.eq(String.join(".", DATA.name, LOCATION, Z.asString("_")), pos.getZ());

        andFilters.add(Filters.and(blockX, blockY, blockZ));
        return this;
    }

    public Query radius(Vector3i pos, int radius)
    {
        final String xLoc = String.join(".", DATA.name, LOCATION, X.asString("_"));
        final Bson gtX = Filters.gt(xLoc, pos.getX() - radius);
        final Bson ltX = Filters.lt(xLoc, pos.getX() + radius);

        final String zLoc = String.join(".", DATA.name, LOCATION, Z.asString("_"));
        final Bson gtZ = Filters.gt(zLoc, pos.getZ() - radius);
        final Bson ltZ = Filters.lt(zLoc, pos.getZ() + radius);

        andFilters.add(Filters.and(gtX, ltX, gtZ, ltZ));
        return this;
    }

    public Query reportFilters(List<String> reports)
    {
        andFilters.add(Filters.in(TYPE.name, reports));
        return this;
    }

    public Query prepared(Document prepared)
    {
        this.andFilters.add(prepared);
        return this;
    }
}
