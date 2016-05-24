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
package org.cubeengine.module.vigil.report.entity;

import java.util.List;
import java.util.Optional;
import org.cubeengine.module.vigil.Receiver;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.Observe;
import org.cubeengine.module.vigil.report.Recall;
import org.cubeengine.module.vigil.report.Report;
import org.cubeengine.module.vigil.report.ReportUtil;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.ExperienceOrb;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.AttackEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackComparators;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.text.Text;

import static org.spongepowered.api.text.action.TextActions.showText;

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

        Text cause = Recall.cause(action);
        EntitySnapshot entity = Recall.entity(action);
        if (entity.getType() == EntityTypes.ITEM)
        {
            Text name = ReportUtil.name(entity);
            ItemStack i = entity.get(Keys.REPRESENTED_ITEM).map(ItemStackSnapshot::createStack).orElse(null);
            Text item = Text.of("?");
            if (i != null)
            {
                item = Text.of(i.getTranslation().get(receiver.getLocale())).toBuilder().onHover(showText(Text.of(i.getItem().getId()))).build();
            }
            int count = 0;
            for (Action a : actions)
            {
                count += Recall.entity(a).get(Keys.REPRESENTED_ITEM).map(ItemStackSnapshot::getCount).orElse(0);
            }
            if (count == 0)
            {
                count = actions.size();
            }

            receiver.sendReport(actions, count,
                                "{txt} destroyed {txt}",
                                "{txt} destroyed {txt} x{}",
                                cause, Text.of(name, ": ", item), count);
        }
        else if (Living.class.isAssignableFrom(entity.getType().getEntityClass()))
        {
            receiver.sendReport(actions, actions.size(),
                                "{txt} killed {txt}",
                                "{txt} killed {txt} x{}",
                                cause, ReportUtil.name(entity), actions.size());
        }
        else if (ExperienceOrb.class.isAssignableFrom(entity.getType().getEntityClass()))
        {
            Integer exp = 0;
            for (Action a : actions)
            {
                EntitySnapshot orb = Recall.entity(a);
                exp += orb.get(Keys.CONTAINED_EXPERIENCE).orElse(0);
            }

            receiver.sendReport(actions, actions.size(),
                                "{txt} picked up an ExpOrb worth {2:amount} points",
                                "{txt} picked up {amount} ExpOrbs worth {amount} points",
                                cause, actions.size(), exp);
        }
        else
        {
            receiver.sendReport(actions, actions.size(),
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
        if (e1.getType() != e2.getType())
        {
            return false;
        }

        if (e1.getType() == EntityTypes.ITEM)
        {
            Optional<ItemStackSnapshot> i1 = Recall.entity(otherAction).get(Keys.REPRESENTED_ITEM);
            Optional<ItemStackSnapshot> i2 = Recall.entity(action).get(Keys.REPRESENTED_ITEM);
            if (!i1.isPresent() && i2.isPresent())
            {
                return false;
            }
            if (ItemStackComparators.DEFAULT.compare(
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
        action.addData(ENTITY, Observe.entity(event.getTargetEntity().createSnapshot()));
        action.addData(CAUSE, Observe.causes(event.getCause()));
        action.addData(LOCATION, Observe.location(event.getTargetEntity().getLocation()));
        return action;
    }

    @Listener public void onAttack(AttackEntityEvent event)
    {
        System.out.print(event.getCause()+ "\n");
        System.out.print(event.getTargetEntity() + "\n");
    }

    @Listener
    public void onDesctruct(DestructEntityEvent event)
    {
        if (event.getCause().get("CombinedItem", Object.class).isPresent())
        {
            // Ignore CombinedItem
            return;
        }

        if (event.getCause().get("PickedUp", Object.class).isPresent())
        {
            // Ignore CombinedItem
            return;
        }

        report(observe(event));
    }
}
