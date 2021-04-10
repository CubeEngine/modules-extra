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
package org.cubeengine.module.headvillager;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import com.google.gson.Gson;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.cubeengine.libcube.util.EventUtil;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Server;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.data.type.ProfessionTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.entity.living.trader.Villager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.event.lifecycle.RegisterDataPackValueEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.merchant.TradeOffer;
import org.spongepowered.api.item.recipe.RecipeRegistration;

/**
 * A module to buy heads
 */
@Singleton
@Module
public class HeadVillager
{
    private static final String mcHeadUrl = "https://minecraft-heads.com/scripts/api.php?tags=true&cat=";

    @Listener
    public void onStartUp(StartedEngineEvent<Server> event)
    {
        final Gson gson = new Gson();
        for (Category category : Category.values())
        {
            try
            {
                final InputStreamReader isr = new InputStreamReader(new URL(mcHeadUrl + category).openStream());
                for (Head head : gson.fromJson(isr, Head[].class))
                {
                    head.init(category);
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Listener
    public void onRegisterData(RegisterDataEvent event)
    {
        HeadVillagerData.register(event);
    }

    @Listener
    public void onRegisterRecipe(RegisterDataPackValueEvent<RecipeRegistration> event)
    {
        HeadVillagerItems.register(event);
    }

    @Listener
    public void onRightClickBlock(InteractEntityEvent.Secondary.On event, @First ServerPlayer player)
    {
        if (!EventUtil.isMainHand(event.context()) || !(event.entity() instanceof Villager))
        {
            return;
        }
        final Entity villager = event.entity();
        final ItemStack itemInHand = player.itemInHand(HandTypes.MAIN_HAND);

        if (!villager.get(HeadVillagerData.VILLAGER).isPresent())
        {
            if (itemInHand.get(HeadVillagerData.VILLAGER).isPresent())
            {
                if (!player.gameMode().get().equals(GameModes.CREATIVE.get()))
                {
                    itemInHand.setQuantity(itemInHand.quantity() - 1);
                    player.setItemInHand(HandTypes.MAIN_HAND, itemInHand);
                }
                villager.offer(HeadVillagerData.VILLAGER, "bob");
                // TODO fancy effects

                event.setCancelled(true);
            }
            return;
        }


        String name = itemInHand.get(Keys.CUSTOM_NAME).map(SpongeComponents.plainSerializer()::serialize).orElse(null);
        if (name == null)
        {
            final Component component = itemInHand.get(Keys.DISPLAY_NAME).orElse(null);
            if (component instanceof TranslatableComponent)
            {
                final Component actualComponent = ((TranslatableComponent)component).args().get(0).children().get(0);
                name = SpongeComponents.plainSerializer().serialize(actualComponent);
            }
            else
            {
                name = "???";
            }
        }
        final List<ItemStack> headStacks = new ArrayList<>();
        for (Category category : Category.values())
        {
            for (String headName : category.heads.keySet())
            {
                if (headName.toLowerCase().contains(name.toLowerCase()))
                {
                    category.heads.get(headName).forEach(head -> headStacks.add(head.stack));
                }
                else
                {
                    for (Head head : category.heads.get(headName))
                    {
                        if (head.tags != null && head.tags.toLowerCase().contains(name))
                        {
                            headStacks.add(head.stack);
                        }
                    }
                }
            }
        }
        Collections.shuffle(headStacks);
        final List<TradeOffer> tradeOffers = headStacks.subList(0, Math.min(headStacks.size(), 20)).stream().map(stack -> TradeOffer.builder().canGrantExperience(false).maxUses(2)
            .firstBuyingItem(ItemStack.of(ItemTypes.EMERALD_BLOCK))
            .sellingItem(stack).build()).collect(Collectors.toList());
        villager.offer(Keys.PROFESSION_TYPE, ProfessionTypes.NITWIT.get());
        villager.offer(Keys.PROFESSION_LEVEL, 6);
        villager.offer(Keys.TRADE_OFFERS, tradeOffers);
        villager.offer(Keys.CUSTOM_NAME, Component.text("Bob - Master Headsman"));
    }

}
