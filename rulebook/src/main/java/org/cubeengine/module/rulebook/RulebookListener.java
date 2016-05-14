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
package org.cubeengine.module.rulebook;

import java.util.Locale;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.rulebook.bookManagement.RulebookManager;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.inventory.ItemStack;

class RulebookListener
{

    private final Rulebook module;
    private final RulebookManager rulebookManager;
    private I18n i18n;

    public RulebookListener(Rulebook module, I18n i18n)
    {
        this.module = module;
        this.rulebookManager = module.getRuleBookManager();
        this.i18n = i18n;
    }

    @Listener
    public void onPlayerLanguageReceived(ClientConnectionEvent.Join event)
    {
        Player player = event.getTargetEntity();
        if (!player.hasPlayedBefore() && !rulebookManager.getLocales().isEmpty())
        {
            Locale locale = player.getLocale();
            if (!this.rulebookManager.contains(locale))
            {
                locale = i18n.getDefaultLanguage().getLocale();
                if (!this.rulebookManager.contains(locale))
                {
                    locale = this.rulebookManager.getLocales().iterator().next();
                }
            }

            ItemStack hand = player.getItemInHand().orElse(null);
            player.setItemInHand(this.rulebookManager.getBook(locale));
            player.getInventory().offer(hand);
            if (hand.getQuantity() != 0)
            {
                Entity entity = player.getWorld().createEntity(EntityTypes.ITEM, player.getLocation().getPosition()).get();
                entity.offer(Keys.REPRESENTED_ITEM, hand.createSnapshot());
                player.getWorld().spawnEntity(entity, Cause.of(NamedCause.source(player)));
            }
        }
    }
}
