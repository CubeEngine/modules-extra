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
/**
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 * <p>
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cubeengine.module.vigil.report;

import org.cubeengine.module.vigil.Receiver;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.event.Event;

import java.util.List;

public interface Report<T extends Event>
{
    DataQuery WORLD = DataQuery.of("WorldUuid");
    DataQuery X = DataQuery.of("Position", "X");
    DataQuery Y = DataQuery.of("Position", "Y");
    DataQuery Z = DataQuery.of("Position", "Z");
    String CAUSE = "cause";
    String CAUSE_TYPE = "type";
    String CAUSE_PLAYER_UUID = "UUID";
    String CAUSE_PLAYER_NAME = "name";
    String LOCATION = "location";

    /**
     * Shows the action to given CommandSource
     *  @param actions   the action to show
     * @param receiver the CommandSource
     */
    void showReport(List<Action> actions, Receiver receiver);

    /**
     * Returns whether the actions can be grouped
     *
     *
     * @param lookup
     * @param action      the first action
     * @param otherAction
     * @param otherReport
     * @return whether the actions can be grouped
     */
    boolean group(Object lookup, Action action, Action otherAction, Report otherReport);

    /**
     * Applies given action to the world
     *
     * @param action   the action to apply
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

    enum CauseType
    {
        CAUSE_PLAYER,
        CAUSE_BLOCK_FIRE,
    }
}
