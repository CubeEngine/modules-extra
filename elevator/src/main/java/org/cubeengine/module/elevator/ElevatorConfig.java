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
package org.cubeengine.module.elevator;

import org.cubeengine.reflect.annotations.Comment;
import org.cubeengine.reflect.codec.yaml.ReflectedYaml;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;

@SuppressWarnings("all")
public class ElevatorConfig extends ReflectedYaml
{
    @Comment("Dekor String to the left and right of the second line")
    public String liftDecor = "۩";
    @Comment("Dekor String to show elevators going up")
    public String upDecor = "▲";
    @Comment("Dekor String to show elevators going down")
    public String downDecor = "▼";
    @Comment("The ItemType to use to create elevator signs")
    public ItemType creationItem = ItemTypes.ENDER_PEARL;
}
