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
package org.cubeengine.module.vigil.report.entity.player;

import org.cubeengine.module.vigil.Receiver;
import org.cubeengine.module.vigil.report.*;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.MessageSinkEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.text.Texts;

import java.util.List;

public class ChatReport extends BaseReport<MessageSinkEvent.Chat>
{
    private static final String CHAT = "chat";

    @Override
    public void showReport(List<Action> actions, Receiver receiver)
    {
        Action action = actions.get(0);
        receiver.sendReport(actions, actions.size(), "{txt} wrote {input}", "{txt} spammed {input} x{}",
                Recall.cause(action), action.getData(CHAT), actions.size());
    }

    @Override
    public boolean group(Object lookup, Action action, Action otherAction, Report otherReport)
    {
        if (!this.equals(otherReport))
        {
            return false;
        }
        // TODO compare cause
        return action.getData(CHAT).equals(otherAction.getData(CHAT));
    }

    @Override
    public void apply(Action action, boolean rollback)
    {}

    @Override
    public Action observe(MessageSinkEvent.Chat event)
    {
        Action action = newReport();
        action.addData(CAUSE, Observe.causes(event.getCause()));
        action.addData(CHAT, Texts.toPlain(event.getRawMessage()));
        return null;
    }

    @Listener(order = Order.POST)
    public void onChat(MessageSinkEvent.Chat event, @First Player player)
    {
        observe(event);
    }
}
