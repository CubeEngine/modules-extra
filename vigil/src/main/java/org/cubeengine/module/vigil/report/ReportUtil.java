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
package org.cubeengine.module.vigil.report;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.module.vigil.Receiver;
import org.cubeengine.module.vigil.report.block.BlockReport;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.registry.RegistryTypes;

public class ReportUtil
{
    public static Component name(BlockSnapshot snapshot, Receiver receiver)
    {
        BlockType type = snapshot.state().type();
        final Builder builder = Component.text();
        builder.append(type.asComponent().color(NamedTextColor.GOLD).hoverEvent(HoverEvent.showText(Component.text(type.key(RegistryTypes.BLOCK_TYPE).asString()))));
        Optional<List<DataView>> items = snapshot.toContainer().getViewList(BlockReport.BLOCK_ITEMS);
        if (items.isPresent() && !items.get().isEmpty())
        {
            // TODO lookup config : detailed inventory? click on ∋ to activate/deactivate or using cmd
            final boolean detailedInventory = receiver.getLookup().getSettings().showDetailedInventory();
            TextComponent elementsHover = Component.text("Inventory Content", NamedTextColor.GRAY);
            if (!detailedInventory)
            {
                elementsHover = elementsHover.append(Component.space()).append(Component.text("(hidden)", NamedTextColor.DARK_GRAY));
                // TODO
                // elementsHover = elementsHover.append(Component.newline()).append(Component.text("Click to show"));
            }

            final TextComponent elements = Component.text("∋").hoverEvent(HoverEvent.showText(elementsHover)); // TODO translate
            builder.append(Component.space()).append(elements).append(Component.space()).append(Component.text("["));
            if (detailedInventory)
            {
                builder.append(Component.space());
                for (DataView dataView : items.get())
                {
                    DataContainer itemData = DataContainer.createNew();
                    itemData.set(DataQuery.of("Count"), dataView.get(DataQuery.of("Count")).get());
                    itemData.set(DataQuery.of("ItemType"), dataView.get(DataQuery.of("id")).get());

                    Optional<DataView> tag = dataView.getView(DataQuery.of("tag"));
                    if (tag.isPresent())
                    {
                        itemData.set(DataQuery.of("UnsafeData"), tag.get().values(false));
                    }

//                    itemData.set(DataQuery.of("UnsafeDamage"), dataView.get(DataQuery.of("Damage")).get());

                    ItemStack item = ItemStack.builder().fromContainer(itemData).build();
                    builder.append(Component.text(dataView.getInt(DataQuery.of("Slot")).get()).toBuilder()
                                            .hoverEvent(item.createSnapshot().asHoverEvent()).build());
                    builder.append(Component.space());
                }
            }
            else
            {
                builder.append(Component.text("..."));
            }
            builder.append(Component.text("]"));
        }

        Optional<List<Component>> sign = snapshot.get(Keys.SIGN_LINES);
        if (sign.isPresent())
        {
            builder.append(Component.space()).append(Component.text("[I]").hoverEvent(HoverEvent.showText(Component.join(Component.newline(), sign.get()))));
        }

        return builder.build();

    }

    public static Component name(EntitySnapshot entity)
    {
        return entity.type().asComponent().hoverEvent(HoverEvent.showText(Component.text(entity.type().key(RegistryTypes.ENTITY_TYPE).asString())));
    }

    public static <LT, T> boolean containsSingle(List<LT> list, Function<LT, T> func)
    {
        T onlyType = null;
        for (LT elem : list)
        {
            T type = func.apply(elem);
            if (onlyType == null || onlyType.equals(type))
            {
                onlyType = type;
            }
            else
            {
                return false;
            }
        }
        return true;
    }


    public static Component name(ItemStackSnapshot itemStackSnapshot)
    {
        return itemStackSnapshot.get(Keys.DISPLAY_NAME).get().append(Component.text(" x", NamedTextColor.YELLOW)).append(Component.text(itemStackSnapshot.quantity(), NamedTextColor.GRAY))
               .hoverEvent(HoverEvent.showText(Component.text(itemStackSnapshot.type().key(RegistryTypes.ITEM_TYPE).asString())));
    }
}
