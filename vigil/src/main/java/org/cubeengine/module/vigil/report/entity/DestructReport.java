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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import com.google.common.collect.Maps;
import org.cubeengine.module.vigil.Receiver;
import org.cubeengine.module.vigil.report.Action;
import org.cubeengine.module.vigil.report.Observe;
import org.cubeengine.module.vigil.report.Recall;
import org.cubeengine.module.vigil.report.Report;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.value.mutable.MutableBoundedValue;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.ExperienceOrb;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.AttackEntityEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackComparators;
import org.spongepowered.api.text.LiteralText.Builder;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import static java.util.stream.Collectors.toList;
import static org.cubeengine.module.vigil.report.ReportUtil.name;
import static org.spongepowered.api.text.format.TextColors.GOLD;
import static org.spongepowered.api.text.format.TextColors.YELLOW;

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
        Entity restored = Recall.restoredEntity(action);
        if (entity.getType() == EntityTypes.ITEM)
        {
            String name = entity.getType().getTranslation().get(receiver.getLocale());
            ItemStack i = restored.get(Keys.REPRESENTED_ITEM).get().createStack();
            Builder item = Text.of(i.getTranslation().get(receiver.getLocale())).toBuilder().onHover(
                TextActions.showText(Text.of(i.getItem().getId())));
            int count = 0;
            for (Action a : actions)
            {
                count += Recall.restoredEntity(a).get(Keys.REPRESENTED_ITEM).get().getCount();
            }

            receiver.sendReport(actions, count,
                                "{txt} destroyed {txt}",
                                "{txt} destroyed {txt} x{}",
                                cause, Text.of(name, ": ", item), count);
        }
        else if (restored instanceof Living)
        {
            receiver.sendReport(actions, actions.size(),
                                "{txt} killed {name}",
                                "{txt} killed {name} x{}",
                                cause, entity.getType().getTranslation().get(receiver.getLocale()), actions.size());
        }
        else if (restored instanceof ExperienceOrb)
        {
            Integer exp = 0;
            for (Action a : actions)
            {
                Entity orb = Recall.restoredEntity(a);
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
                                "{txt} destroyed {name}",
                                "{txt} destroyed {name} x{}",
                                cause, entity.getType().getTranslation().get(receiver.getLocale()), actions.size());
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
            if (ItemStackComparators.DEFAULT.compare(
                Recall.restoredEntity(otherAction).get(Keys.REPRESENTED_ITEM).get().createStack(),
                Recall.restoredEntity(action).get(Keys.REPRESENTED_ITEM).get().createStack()) != 0)
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
        if (event.getTargetEntity() instanceof Item
        && ((Item)event.getTargetEntity()).item().get().getCount() == 0)
        {
            // ItemPickup is handled in ChangeInventoryEvent
            return;
        }
        report(observe(event));
    }
}
