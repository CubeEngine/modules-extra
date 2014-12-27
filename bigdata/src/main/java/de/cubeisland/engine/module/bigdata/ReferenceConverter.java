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

import com.mongodb.DBRefBase;
import de.cubeisland.engine.converter.converter.SimpleConverter;
import de.cubeisland.engine.module.bigdata.node.DBRefBaseNode;
import de.cubeisland.engine.reflect.Reflector;
import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.converter.node.Node;

public class ReferenceConverter extends SimpleConverter<Reference>
{
    private final Reflector reflector;

    public ReferenceConverter(Reflector reflector)
    {
        this.reflector = reflector;
    }

    @Override
    public Node toNode(Reference object) throws ConversionException
    {
        return new DBRefBaseNode(object.getDBRef());
    }

    @Override
    public Reference fromNode(Node node) throws ConversionException
    {
        if (node instanceof DBRefBaseNode)
        {
            return new Reference(reflector, (DBRefBase)node.getValue());
        }
        throw ConversionException.of(this, node, "Node is not a mapnode!");
    }
}
