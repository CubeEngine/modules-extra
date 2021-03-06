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
package org.cubeengine.module.vigil.report.entity;

import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bson.Document;
import org.cubeengine.module.vigil.Receiver;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.Observe;
import org.cubeengine.module.vigil.report.Recall;
import org.cubeengine.module.vigil.report.Report;
import org.cubeengine.module.vigil.report.ReportUtil;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.AttackEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackComparators;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.registry.RegistryTypes;

/* TODO
death
-animal
-boss
-kill
-monster
-npc
-other
-pet
-player?

-hanging-break
-vehicle-break

 */
public class DestructReport extends EntityReport<DestructEntityEvent>
{
    @Override
    public void showReport(List<Action> actions, Receiver receiver)
    {
        Action action = actions.get(0);
        //Optional<BlockSnapshot> orig = action.getCached(BLOCKS_ORIG, Recall::origSnapshot).get(0);

        Component cause = Recall.cause(action);
        EntitySnapshot entity = Recall.entity(action);
        Boolean isLiving = action.< Document>getData(EntityReport.ENTITY).getBoolean(EntityReport.LIVING);
        if (entity.type() == EntityTypes.ITEM.get())
        {
            Component name = ReportUtil.name(entity);
            ItemStack i = entity.get(Keys.ITEM_STACK_SNAPSHOT).map(ItemStackSnapshot::createStack).orElse(null);
            Component item = Component.text("?");
            if (i != null)
            {
                item = i.get(Keys.DISPLAY_NAME).get().hoverEvent(HoverEvent.showText(Component.text(i.type().key(RegistryTypes.ITEM_TYPE).asString())));
            }
            int count = 0;
            for (Action a : actions)
            {
                count += Recall.entity(a).get(Keys.ITEM_STACK_SNAPSHOT).map(ItemStackSnapshot::quantity).orElse(0);
            }
            if (count == 0)
            {
                count = actions.size();
            }

            receiver.sendReport(this, actions, count,
                                "{txt} destroyed {txt}",
                                "{txt} destroyed {txt} x{}",
                                cause, name.append(Component.text(": ")).append(Component.text(count)));
        }
        else if (isLiving)
        {
            receiver.sendReport(this, actions, actions.size(),
                                "{txt} killed {txt}",
                                "{txt} killed {txt} x{}",
                                cause, ReportUtil.name(entity), actions.size());
        }
        else if (entity.type().equals(EntityTypes.EXPERIENCE_ORB.get()))
        {
            Integer exp = 0;
            for (Action a : actions)
            {
                EntitySnapshot orb = Recall.entity(a);
                exp += orb.get(Keys.EXPERIENCE).orElse(0);
            }

            receiver.sendReport(this, actions, actions.size(),
                                "{txt} picked up an ExpOrb worth {2:amount} points",
                                "{txt} picked up {amount} ExpOrbs worth {amount} points",
                                cause, actions.size(), exp);
        }
        else
        {
            receiver.sendReport(this, actions, actions.size(),
                                "{txt} destroyed {txt}",
                                "{txt} destroyed {txt} x{}",
                                cause, ReportUtil.name(entity), actions.size());
        }
    }

    @Override
    public boolean group(Object lookup, Action action, Action otherAction, Report otherReport)
    {
        if (!this.equals(otherReport))
        {
            return false;
        }

        if (!action.getData(CAUSE).equals(otherAction.getData(CAUSE)))
        {
            // TODO check same cause better
            return false;
        }

        EntitySnapshot e1 = Recall.entity(action);
        EntitySnapshot e2 = Recall.entity(otherAction);
        if (e1.type() != e2.type())
        {
            return false;
        }

        if (e1.type() == EntityTypes.ITEM.get())
        {
            Optional<ItemStackSnapshot> i1 = Recall.entity(otherAction).get(Keys.ITEM_STACK_SNAPSHOT);
            Optional<ItemStackSnapshot> i2 = Recall.entity(action).get(Keys.ITEM_STACK_SNAPSHOT);
            if (!i1.isPresent() && i2.isPresent())
            {
                return false;
            }
            if (ItemStackComparators.DEFAULT.get().compare(
                i1.map(ItemStackSnapshot::createStack).orElse(null),
                i2.map(ItemStackSnapshot::createStack).orElse(null)) != 0)
            {
                return false;
            }
        }

        // TODO in short timeframe (minutes? configurable)

        return true;
    }

    @Override
    public void apply(Action action, boolean noOp)
    {

    }

    @Override
    public Action observe(DestructEntityEvent event)
    {
        Action action = newReport();
        action.addData(ENTITY, Observe.entity(event.entity()));
        action.addData(CAUSE, Observe.causes(event.cause()));
        action.addData(LOCATION, Observe.location(event.entity().serverLocation()));
        return action;
    }

    @Listener
    public void onAttack(AttackEntityEvent event)
    {
        //System.out.print(event.getCause()+ "\n");
        //System.out.print(event.getTargetEntity() + "\n");
    }

    @Listener
    public void onDestruct(DestructEntityEvent event)
    {
    /* TODO    if (event.getCause().get("CombinedItem", Object.class).isPresent())
        {
            // Ignore CombinedItem
            return;
        }

     TODO     if (event.getCause().get("PickedUp", Object.class).isPresent())
        {
            // Ignore Pickup
            return;
        }
        */

        if (!isActive(event.entity().serverLocation().world()))
        {
            return;
        }

        report(observe(event));
    }
}
