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
package org.cubeengine.module.vigil.report;

import org.spongepowered.api.event.Event;
import org.spongepowered.api.command.CommandSource;

public interface Report<T extends Event>
{
    // TODO multiple similar Actions

    /**
     * Shows the action to given CommandSource
     * @param action the action to show
     * @param cmdSource the CommandSource
     */
    void showReport(Action action, CommandSource cmdSource);

    /**
     * Returns whether the actions can be grouped
     * @param action the first action
     * @param other the second action
     * @return whether the actions can be grouped
     */
    boolean group(Action action, Action other);

    /**
     * Applies given action to the world
     *
     * @param action the action to apply
     * @param rollback true if rollback or false if redo
     */
    void apply(Action action, boolean rollback);

    /**
     * Observes an event an creates an action for it
     *
     * @param event the event to observe
     * @return the events action
     */
    Action observe(T event);
}
