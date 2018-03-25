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
package org.cubeengine.module.itemduct;

import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.service.event.ModuleListener;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.module.itemduct.listener.ItemDuctFilterListener;
import org.cubeengine.module.itemduct.listener.ItemDuctListener;
import org.cubeengine.module.itemduct.listener.ItemDuctTransferListener;
import org.cubeengine.processor.Module;
import org.spongepowered.api.advancement.Advancement;
import org.spongepowered.api.advancement.AdvancementTree;
import org.spongepowered.api.advancement.AdvancementType;
import org.spongepowered.api.advancement.AdvancementTypes;
import org.spongepowered.api.advancement.DisplayInfo;
import org.spongepowered.api.advancement.criteria.AdvancementCriterion;
import org.spongepowered.api.advancement.criteria.ScoreAdvancementCriterion;
import org.spongepowered.api.advancement.criteria.trigger.Trigger;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.GameRegistryEvent;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.item.inventory.CraftItemEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Module
public class Itemduct extends CubeEngineModule
{
    @ModuleConfig private ItemductConfig config;
    @Inject private ItemDuctManager manager;
    @ModuleListener private ItemDuctListener listenerActivator;
    @ModuleListener private ItemDuctTransferListener listenerTransfer;
    @ModuleListener private ItemDuctFilterListener listenerFilter;
    @Inject private PluginContainer plugin;
    @Listener
    public void onPreInit(GamePreInitializationEvent event)
    {
        this.manager.setup(this.plugin, this.config);
        this.listenerActivator.setup(this);
        this.listenerFilter.setup(this);
        this.listenerTransfer.setup(this, this.manager);
    }

    @Listener
    public void onReload(GameReloadEvent event)
    {
        this.config.reload();
        this.manager.reload(this.config);
    }

    public AdvancementTree advancementTree;
    public Advancement rootAdvancement;
    public Advancement activate;
    public Advancement filters;
    public Advancement prompted;
    public ScoreAdvancementCriterion promptCriterion;

    @Listener
    public void onRegisterAdvancementTrees(GameRegistryEvent.Register<AdvancementTree> event) {
        this.advancementTree = AdvancementTree.builder()
                .rootAdvancement(this.rootAdvancement)
                .id("itemduct")
                .build();
        event.register(this.advancementTree);
    }

    @Listener
    public void onCraft(CraftItemEvent.Craft event, @Root Player player)
    {
        if (event.getRecipe().isPresent())
        {
            if (this.manager.matchesRecipe(event.getRecipe().get()))
            {
                player.getProgress(this.rootAdvancement).get(AdvancementCriterion.DUMMY).get().grant();
            }
        }
    }

    // TODO CraftItemEvent for root

    @Listener
    public void onRegisterAdvancements(GameRegistryEvent.Register<Advancement> event)
    {
        this.rootAdvancement = Advancement.builder()
                .criterion(AdvancementCriterion.DUMMY)
                .displayInfo(DisplayInfo.builder()
                        .icon(ItemTypes.HOPPER)
                        .title(Text.of("Item Logistics"))
                        .description(Text.of("Craft an ItemDuct Activator"))
                        .build())
                .id("itemduct-start")
                .build();
        event.register(this.rootAdvancement);

        this.activate = Advancement.builder()
                .parent(this.rootAdvancement)
                .displayInfo(DisplayInfo.builder()
                        .icon(ItemTypes.HOPPER)
                        .title(Text.of("First Activation"))
                        .description(Text.of("Activate a Piston"))
                        .build())
                .criterion(AdvancementCriterion.DUMMY)
                .id("itemduct-activate")
                .build();
        event.register(this.activate);

        this.filters = Advancement.builder()
                .parent(this.activate)
                .displayInfo(DisplayInfo.builder()
                        .icon(ItemTypes.PAPER)
                        .title(Text.of("Filters"))
                        .description(Text.of("Open a Filter"))
                        .build())
                .criterion(AdvancementCriterion.DUMMY)
                .id("itemduct-filter")
                .build();
        event.register(this.filters);

        this.promptCriterion = ScoreAdvancementCriterion.builder()
                .goal(100)
                .name("itemduct-prompt")
                .build();
        this.prompted = Advancement.builder()
                .parent(this.activate)
                .displayInfo(DisplayInfo.builder()
                        .icon(ItemTypes.NETHER_STAR)
                        .title(Text.of("Mastered"))
                        .description(Text.of("Use ItemDuct sorting over 100 times"))
                        .type(AdvancementTypes.CHALLENGE)
                        .build())
                .criterion(promptCriterion)
                .id("itemduct-master")
                .build();
        event.register(this.prompted);
    }
}
