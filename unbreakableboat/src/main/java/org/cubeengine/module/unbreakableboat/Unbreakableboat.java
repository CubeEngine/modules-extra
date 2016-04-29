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
package org.cubeengine.module.unbreakableboat;

import java.util.Arrays;
import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.module.unbreakableboat.data.ImmutableUnbreakableData;
import org.cubeengine.module.unbreakableboat.data.UnbreakableData;
import org.cubeengine.module.unbreakableboat.data.UnbreakableDataBuilder;
import org.cubeengine.libcube.service.event.EventManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.meta.ItemEnchantment;
import org.spongepowered.api.entity.vehicle.Boat;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.ConstructEntityEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.item.Enchantments;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.recipe.ShapedRecipe;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import static java.util.Collections.singletonList;

/**
 * A module providing a recipe for an (almost) unbreakable boat
 */
@ModuleInfo(name = "UnbreakableBoat", description = "Adds a Recipe for an unbreakable Boat")
public class Unbreakableboat extends Module
{
    private ItemStack boat = ItemStack.of(ItemTypes.BOAT, 1);

    @Inject private EventManager em;

    @Enable
    public void onEnable()
    {
        em.registerListener(this, this);
        boat.offer(Keys.ITEM_ENCHANTMENTS, singletonList(new ItemEnchantment(Enchantments.UNBREAKING, 5)));
        boat.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GOLD, "Sturdy Boat"));
        boat.offer(Keys.ITEM_LORE, Arrays.asList(Text.of(TextColors.YELLOW, "Can take a lot!")));

        ItemStack log = ItemStack.of(ItemTypes.LOG, 1);

        ShapedRecipe recipe = Sponge.getRegistry().createBuilder(ShapedRecipe.Builder.class)
            .width(3).height(2)
            .row(0, log, null, log)
            .row(1, log, log, log)
        // TODO SpongePR#1098 .aisle("l l", "lll")
        // TODO SpongePR#1098 .where('l', log)
            .addResult(boat)
            .build();

        Sponge.getRegistry().getRecipeRegistry().register(recipe);

        Sponge.getDataManager().register(UnbreakableData.class, ImmutableUnbreakableData.class, new UnbreakableDataBuilder());
    }

    @Listener
    public void onVehicleBreak(DamageEntityEvent event)
    {
        if (event.getTargetEntity() instanceof Boat)
        {
            if (event.getTargetEntity().get(UnbreakableData.UNBREAKING).isPresent())
            {
                // TODO do no cancel if direct attacker is player
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onVehiclePlace(ConstructEntityEvent.Post event)
    {
        event.getTargetEntity().offer(new UnbreakableData(true));
    }
}
