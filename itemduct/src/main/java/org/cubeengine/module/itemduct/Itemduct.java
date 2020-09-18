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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.TextComponent;
import org.cubeengine.libcube.service.event.ModuleListener;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.module.itemduct.data.DuctData;
import org.cubeengine.module.itemduct.data.DuctRecipes;
import org.cubeengine.module.itemduct.listener.ItemDuctFilterListener;
import org.cubeengine.module.itemduct.listener.ItemDuctListener;
import org.cubeengine.module.itemduct.listener.ItemDuctTransferListener;
import org.cubeengine.processor.Module;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.advancement.Advancement;
import org.spongepowered.api.advancement.AdvancementTree;
import org.spongepowered.api.advancement.AdvancementTypes;
import org.spongepowered.api.advancement.DisplayInfo;
import org.spongepowered.api.advancement.criteria.AdvancementCriterion;
import org.spongepowered.api.advancement.criteria.ScoreAdvancementCriterion;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.CraftItemEvent;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.RegisterCatalogEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.recipe.Recipe;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe;
import org.spongepowered.plugin.PluginContainer;


@Singleton
@Module
public class Itemduct
{
    @ModuleConfig private ItemductConfig config;
    @Inject private ItemDuctManager manager;
    @ModuleListener private ItemDuctListener listenerActivator;
    @ModuleListener private ItemDuctTransferListener listenerTransfer;
    @ModuleListener private ItemDuctFilterListener listenerFilter;
    @Inject private PluginContainer plugin;

    @Listener
    public void onConstruct(ConstructPluginEvent event)
    {
        this.manager.setup(this.config);
        this.listenerActivator.setup(this);
        this.listenerFilter.setup(this);
        this.listenerTransfer.setup(this, this.manager);
    }

//    @Listener
//    public void onReload(Reload event)
//    {
//        this.config.reload();
//        this.manager.reload(this.config);
//    }

    @Listener
    public void onRegisterRecipe(RegisterCatalogEvent<Recipe> event)
    {
        DuctRecipes.register(event, config);
    }

    @Listener
    public void onRegisterData(RegisterCatalogEvent<DataRegistration>  event)
    {
        DuctData.register(event);
    }

    public AdvancementTree advancementTree;
    public Advancement rootAdvancement;
    public Advancement activate;
    public Advancement filters;
    public Advancement prompted;
    public ScoreAdvancementCriterion promptCriterion;

    @Listener
    public void onRegisterAdvancementTrees(RegisterCatalogEvent<AdvancementTree> event) {
        this.advancementTree = AdvancementTree.builder()
                .rootAdvancement(this.rootAdvancement)
                .key(ResourceKey.of("itemduct", "itemduct"))
                .build();
        event.register(this.advancementTree);
    }

    @Listener
    public void onCraft(CraftItemEvent.Craft event, @Root ServerPlayer player)
    {
        if (event.getRecipe().isPresent())
        {
            if (DuctRecipes.matchesRecipe(event.getRecipe().get()))
            {
                player.getProgress(this.rootAdvancement).get(AdvancementCriterion.dummy()).get().grant();
            }
        }
    }

    public ItemDuctManager getManager()
    {
        return manager;
    }

    @Listener
    public void onRegisterAdvancements(RegisterCatalogEvent<Advancement> event)
    {
        this.rootAdvancement = Advancement.builder()
                .criterion(AdvancementCriterion.dummy())
                .displayInfo(DisplayInfo.builder()
                        .icon(ItemTypes.HOPPER)
                        .title(TextComponent.of("Item Logistics"))
                        .description(TextComponent.of("Craft an ItemDuct Activator"))
                        .build())
                .key(ResourceKey.of(plugin, "itemduct-start"))
                .build();
        event.register(this.rootAdvancement);

        this.activate = Advancement.builder()
                .parent(this.rootAdvancement)
                .displayInfo(DisplayInfo.builder()
                        .icon(ItemTypes.HOPPER)
                        .title(TextComponent.of("First Activation"))
                        .description(TextComponent.of("Activate a Piston"))
                        .build())
                .criterion(AdvancementCriterion.dummy())
                .key(ResourceKey.of(plugin, "itemduct-activate"))
                .build();
        event.register(this.activate);

        this.filters = Advancement.builder()
                .parent(this.activate)
                .displayInfo(DisplayInfo.builder()
                        .icon(ItemTypes.PAPER)
                        .title(TextComponent.of("Filters"))
                        .description(TextComponent.of("Open a Filter"))
                        .build())
                .criterion(AdvancementCriterion.dummy())
                .key(ResourceKey.of(plugin, "itemduct-filter"))
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
                        .title(TextComponent.of("Mastered"))
                        .description(TextComponent.of("Use ItemDuct sorting over 100 times"))
                        .type(AdvancementTypes.CHALLENGE)
                        .build())
                .criterion(promptCriterion)
                .key(ResourceKey.of(plugin, "itemduct-master"))
                .build();
        event.register(this.prompted);
    }

    public ItemductConfig getConfig() {
        return config;
    }
}
