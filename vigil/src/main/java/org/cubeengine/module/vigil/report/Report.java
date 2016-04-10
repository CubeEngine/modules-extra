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

import java.util.List;
import java.util.Objects;
import org.cubeengine.module.vigil.Receiver;
import org.spongepowered.api.data.DataQuery;

import static java.util.Collections.emptyList;

public interface Report
{
    DataQuery WORLD = DataQuery.of("WorldUuid");
    DataQuery X = DataQuery.of("Position", "X");
    DataQuery Y = DataQuery.of("Position", "Y");
    DataQuery Z = DataQuery.of("Position", "Z");
    String CAUSE = "cause";
    String CAUSE_TYPE = "type";
    String CAUSE_PLAYER_UUID = "UUID";
    String CAUSE_NAME = "name";
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
     * Applies the action to the world
     *
     * @param action   the action to apply
     * @param noOp true if permanent or false transient
     */
    void apply(Action action, boolean noOp);

    /**
     * Applies the reverse action to the world
     *
     * @param action the action to unapply
     * @param noOp true if permanent or false if transient
     */
    void unapply(Action action, boolean noOp);

    enum CauseType
    {
        CAUSE_PLAYER,
        CAUSE_BLOCK_AIR, // Indirect
        CAUSE_BLOCK_FIRE,
        CAUSE_TNT,
    }

    interface SimpleGrouping extends Report
    {
        @Override
        default boolean group(Object lookup, Action action, Action otherAction, Report otherReport)
        {
            if (!this.equals(otherReport))
            {
                return false;
            }
            // TODO compare cause
            return !groupBy().stream().anyMatch(key -> !Objects.equals(action.getData(key), otherAction.getData(key)));
        }

        default List<String> groupBy()
        {
            return emptyList();
        }
    }

    interface Readonly extends Report
    {
        default void apply(Action action, boolean noOp) {}
        default void unapply(Action action, boolean noOp) {}
    }

    interface NonGrouping extends Report
    {
        @Override
        default boolean group(Object lookup, Action action, Action otherAction, Report otherReport)
        {
            return false;
        }
    }
}
