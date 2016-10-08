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
package org.cubeengine.module.vigil.report;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.cubeengine.module.vigil.report.block.BlockReport;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.MemoryDataContainer;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Text.Builder;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.translation.Translation;

import static org.spongepowered.api.text.action.TextActions.showItem;
import static org.spongepowered.api.text.action.TextActions.showText;

public class ReportUtil
{
    public static Text name(BlockSnapshot snapshot)
    {
        BlockType type = snapshot.getState().getType();
        Translation trans = type.getTranslation();
        if (snapshot.getState().getType().getItem().isPresent())
        {
            trans = ItemStack.builder().fromBlockSnapshot(snapshot).build().getTranslation();
        }

        Builder builder = Text.builder();

        builder.append(Text.of(TextColors.GOLD, trans).toBuilder().onHover(
            showText(Text.of(type.getName()))).build());

        Optional<List<DataView>> items = snapshot.toContainer().getViewList(BlockReport.BLOCK_ITEMS);
        if (items.isPresent() && !items.get().isEmpty())
        {
            builder.append(Text.of(" ∋ [ "));
            for (DataView dataView : items.get())
            {
                MemoryDataContainer itemData = new MemoryDataContainer();
                itemData.set(DataQuery.of("Count"), dataView.get(DataQuery.of("Count")).get());
                itemData.set(DataQuery.of("ItemType"), dataView.get(DataQuery.of("id")).get());

                Optional<DataView> tag = dataView.getView(DataQuery.of("tag"));
                if (tag.isPresent())
                {
                    itemData.set(DataQuery.of("UnsafeData"), tag.get().getValues(false));
                }

                itemData.set(DataQuery.of("UnsafeDamage"), dataView.get(DataQuery.of("Damage")).get());

                ItemStack item = ItemStack.builder().fromContainer(itemData).build();

                builder.append(Text.of(dataView.getInt(DataQuery.of("Slot")).get()).toBuilder()
                                   .onHover(showItem(item.createSnapshot())).build());
                builder.append(Text.of(" "));
            }
            builder.append(Text.of("]"));
        }

        Optional<List<Text>> sign = snapshot.get(Keys.SIGN_LINES);
        if (sign.isPresent())
        {
            builder.append(Text.of(" "), Text.of("[I]").toBuilder().onHover(showText(Text.joinWith(Text.NEW_LINE, sign.get()))).build());
        }

        return builder.build();

    }

    public static Text name(EntitySnapshot entity)
    {
        return Text.of(entity.getType().getTranslation()).toBuilder()
                   .onHover(showText(Text.of(entity.getType().getId()))).build();
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


}
