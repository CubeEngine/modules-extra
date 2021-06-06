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
package org.cubeengine.module.traders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.logging.log4j.Logger;
import org.cubeengine.libcube.util.EventUtil;
import org.cubeengine.processor.Module;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.entity.living.trader.WanderingTrader;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.event.lifecycle.RegisterDataPackValueEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.merchant.TradeOffer;
import org.spongepowered.api.item.recipe.RecipeRegistration;
import org.spongepowered.math.vector.Vector3d;

@Singleton
@Module
public class Traders
{
    @Inject Logger logger;

    @Listener
    public void onRegisterData(RegisterDataEvent event)
    {
        TradersData.register(event);
    }

    @Listener
    public void onRegisterRecipe(RegisterDataPackValueEvent<RecipeRegistration> event)
    {
        TradersItems.register(event);
    }

    @Listener
    public void onRightClickBlock(InteractEntityEvent.Secondary.On event, @First ServerPlayer player)
    {
        if (!EventUtil.isMainHand(event.context()) || !(event.entity() instanceof WanderingTrader))
        {
            return;
        }
        final Entity villager = event.entity();
        final ItemStack itemInHand = player.itemInHand(HandTypes.MAIN_HAND);

        if (villager.get(TradersData.VILLAGER).isPresent())
        {
            return;
        }

        final Optional<String> type = itemInHand.get(TradersData.VILLAGER);
        if (type.isPresent())
        {
            if (!player.gameMode().get().equals(GameModes.CREATIVE.get()))
            {
                itemInHand.setQuantity(itemInHand.quantity() - 1);
                player.setItemInHand(HandTypes.MAIN_HAND, itemInHand);
            }
            villager.offer(TradersData.VILLAGER, type.get());
            villager.world().playSound(Sound.sound(SoundTypes.ENTITY_VILLAGER_WORK_CLERIC, Source.NEUTRAL, 3, 2f), villager.position());
            final ParticleEffect effect = ParticleEffect.builder().type(ParticleTypes.SMOKE).offset(new Vector3d(0.2, 1, 0.2)).quantity(50).build();
            villager.world().spawnParticles(effect, villager.position());
            initOffers(villager, type.get());
            event.setCancelled(true);
        }
    }

    private void initOffers(Entity villager, String type)
    {
        final List<TradeOffer> offers;
        switch (type) {
            case "fisher":
                final TradeOffer tridentTrade = TradeOffer.builder()
                                                          .firstBuyingItem(ItemStack.of(ItemTypes.EMERALD, 32))
                                                          .secondBuyingItem(ItemStack.of(ItemTypes.DIAMOND, 20))
                                                          .sellingItem(ItemStack.of(ItemTypes.TRIDENT))
                                                          .maxUses(1).build();
                final TradeOffer lanternTrade = TradeOffer.builder()
                                                          .firstBuyingItem(ItemStack.of(ItemTypes.EMERALD, 20))
                                                          .secondBuyingItem(ItemStack.of(ItemTypes.REDSTONE_LAMP, 64))
                                                          .sellingItem(ItemStack.of(ItemTypes.SEA_LANTERN, 64))
                                                          .maxUses(10).build();
                final TradeOffer prismarineCrystalTrade = TradeOffer.builder()
                                                                    .firstBuyingItem(ItemStack.of(ItemTypes.EMERALD, 7))
                                                                    .sellingItem(ItemStack.of(ItemTypes.PRISMARINE_CRYSTALS, 5))
                                                                    .maxUses(10).build();
                final TradeOffer prismarineShardTrade = TradeOffer.builder()
                                                                  .firstBuyingItem(ItemStack.of(ItemTypes.EMERALD, 5))
                                                                  .sellingItem(ItemStack.of(ItemTypes.PRISMARINE_SHARD, 4))
                                                                  .maxUses(10).build();
                final TradeOffer inkSakTrade = TradeOffer.builder()
                                                         .firstBuyingItem(ItemStack.of(ItemTypes.EMERALD, 3))
                                                         .sellingItem(ItemStack.of(ItemTypes.INK_SAC, 8))
                                                         .maxUses(10).build();
                final TradeOffer darkPrismarineTrade = TradeOffer.builder()
                                                                 .firstBuyingItem(ItemStack.of(ItemTypes.EMERALD, 20))
                                                                 .secondBuyingItem(ItemStack.of(ItemTypes.MOSSY_STONE_BRICKS, 64))
                                                                 .sellingItem(ItemStack.of(ItemTypes.DARK_PRISMARINE, 64))
                                                                 .maxUses(10).build();
                final TradeOffer prismarineBricksTrade = TradeOffer.builder()
                                                                   .firstBuyingItem(ItemStack.of(ItemTypes.EMERALD, 20))
                                                                   .secondBuyingItem(ItemStack.of(ItemTypes.STONE_BRICKS, 64))
                                                                   .sellingItem(ItemStack.of(ItemTypes.PRISMARINE_BRICKS, 64))
                                                                   .maxUses(10).build();
                final TradeOffer prismarineTrade = TradeOffer.builder()
                                                             .firstBuyingItem(ItemStack.of(ItemTypes.EMERALD, 20))
                                                             .secondBuyingItem(ItemStack.of(ItemTypes.COBBLESTONE, 64))
                                                             .sellingItem(ItemStack.of(ItemTypes.PRISMARINE, 64))
                                                             .maxUses(10).build();
                final TradeOffer spongeTrade = TradeOffer.builder()
                                                         .firstBuyingItem(ItemStack.of(ItemTypes.EMERALD, 15))
                                                         .secondBuyingItem(ItemStack.of(ItemTypes.DEAD_TUBE_CORAL_BLOCK, 1))
                                                         .sellingItem(ItemStack.of(ItemTypes.WET_SPONGE, 1))
                                                         .maxUses(10).build();
                final TradeOffer bribe = TradeOffer.builder()
                                                   .firstBuyingItem(ItemStack.of(ItemTypes.FISHING_ROD, 1))
                                                   .secondBuyingItem(ItemStack.of(ItemTypes.EMERALD_BLOCK, 9))
                                                   .sellingItem(TradersItems.conduitHead())
                                                   .maxUses(1).build();
                offers = Arrays.asList(prismarineShardTrade, prismarineCrystalTrade, inkSakTrade,
                              darkPrismarineTrade, prismarineBricksTrade, prismarineTrade, lanternTrade, spongeTrade, tridentTrade, bribe);
                break;
            case "ice":

                final TradeOffer berry = TradeOffer.builder()
                                  .firstBuyingItem(ItemStack.of(ItemTypes.EMERALD, 1))
                                  .sellingItem(ItemStack.of(ItemTypes.SWEET_BERRIES, 3))
                                  .maxUses(10).build();
                final TradeOffer spruceSaplings = TradeOffer.builder()
                                                       .firstBuyingItem(ItemStack.of(ItemTypes.EMERALD, 2))
                                                       .sellingItem(ItemStack.of(ItemTypes.SPRUCE_SAPLING, 16))
                                                       .maxUses(10).build();
                final TradeOffer snowBlock = TradeOffer.builder()
                                                     .firstBuyingItem(ItemStack.of(ItemTypes.EMERALD, 2))
                                                     .secondBuyingItem(ItemStack.of(ItemTypes.CARVED_PUMPKIN, 1))
                                                     .sellingItem(ItemStack.of(ItemTypes.SNOW_BLOCK, 2))
                                                     .maxUses(10).build();
                final TradeOffer pumpkin = TradeOffer.builder()
                                                 .firstBuyingItem(ItemStack.of(ItemTypes.EMERALD, 1))
                                                 .secondBuyingItem(ItemStack.of(ItemTypes.SNOWBALL, 8))
                                                 .sellingItem(ItemStack.of(ItemTypes.CARVED_PUMPKIN, 1))
                                                 .maxUses(10).build();
                final TradeOffer ice = TradeOffer.builder()
                                                       .firstBuyingItem(ItemStack.of(ItemTypes.EMERALD, 10))
                                                       .secondBuyingItem(ItemStack.of(ItemTypes.SNOW_BLOCK, 32))
                                                       .sellingItem(ItemStack.of(ItemTypes.ICE, 32))
                                                       .maxUses(10).build();
                final TradeOffer packedIce = TradeOffer.builder()
                                                       .firstBuyingItem(ItemStack.of(ItemTypes.EMERALD, 25))
                                                       .secondBuyingItem(ItemStack.of(ItemTypes.ICE, 32))
                                                       .sellingItem(ItemStack.of(ItemTypes.PACKED_ICE, 32))
                                                       .maxUses(10).build();
                final TradeOffer blueIce = TradeOffer.builder()
                                                   .firstBuyingItem(ItemStack.of(ItemTypes.EMERALD, 60))
                                                   .secondBuyingItem(ItemStack.of(ItemTypes.PACKED_ICE, 32))
                                                   .sellingItem(ItemStack.of(ItemTypes.BLUE_ICE, 32))
                                                   .maxUses(10).build();
                final TradeOffer spawnEgg = TradeOffer.builder()
                                                     .firstBuyingItem(ItemStack.of(ItemTypes.EMERALD_BLOCK, 10))
                                                     .secondBuyingItem(ItemStack.of(ItemTypes.BLUE_ICE, 64))
                                                     .sellingItem(ItemStack.of(ItemTypes.POLAR_BEAR_SPAWN_EGG, 1))
                                                     .maxUses(2).build();
                final TradeOffer bribe2 = TradeOffer.builder()
                           .firstBuyingItem(ItemStack.of(ItemTypes.FISHING_ROD, 1))
                           .secondBuyingItem(ItemStack.of(ItemTypes.EMERALD_BLOCK, 9))
                           .sellingItem(TradersItems.iceHead())
                           .maxUses(1).build();
                offers = Arrays.asList(spruceSaplings, snowBlock, pumpkin, ice, packedIce, blueIce, spawnEgg, bribe2);
                break;
            default:
                offers = new ArrayList<>();
        }

        villager.offer(Keys.TRADE_OFFERS, offers);
    }
}
