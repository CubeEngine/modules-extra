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
package org.cubeisland.module.faq;

import java.util.Map;
import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.converter.ConverterManager;
import de.cubeisland.engine.converter.converter.SingleClassConverter;
import de.cubeisland.engine.converter.node.MapNode;
import de.cubeisland.engine.converter.node.Node;
import de.cubeisland.engine.converter.node.StringNode;

public class QuestionConverter extends SingleClassConverter<Question>
{
    @Override
    public Node toNode(Question object, ConverterManager manager) throws ConversionException
    {
        MapNode node = MapNode.emptyMap();
        node.set("question", new StringNode(object.getQuestion()));
        node.set("answer", new StringNode(object.getAnswer()));
        node.set("keywords", manager.convertToNode(object.getKeywords()));
        return node;
    }

    @Override
    public Question fromNode(Node node, ConverterManager manager) throws ConversionException
    {
        if (node instanceof MapNode)
        {
            Map<String, Node> content = ((MapNode)node).getValue();
            if (!content.containsKey("question") || !content.containsKey("answer") ||!content.containsKey("keywords"))
            {
                throw ConversionException.of(this, node, "Missing keys, required: question, answer, keywords!");
            }
            return new Question(
                content.get("question").asText(),
                content.get("answer").asText(),
                manager.<String[]>convertFromNode(content.get("keywords"), String[].class));
        }
        throw ConversionException.of(this, node, "Incompatible node!");
    }
}
