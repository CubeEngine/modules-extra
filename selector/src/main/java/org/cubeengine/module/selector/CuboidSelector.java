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
package org.cubeengine.module.selector;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.ServiceImpl;
import de.cubeisland.engine.modularity.asm.marker.Version;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.libcube.util.math.shape.Shape;
import org.cubeengine.libcube.service.Selector;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.block.BlockTypes.AIR;


@ServiceImpl(Selector.class)
@Version(1)
public class CuboidSelector implements Selector
{
    @Inject private org.cubeengine.module.selector.Selector module;
    @Inject private EventManager em;
    @Inject private I18n i18n;

    private Map<UUID, SelectorData> selectorData = new HashMap<>();

    @Enable
    public void onEnable()
    {
        em.registerListener(Selector.class, this);
    }

    @Override
    public Shape getSelection(Player user)
    {
        SelectorData data = this.selectorData.get(user.getUniqueId());
        return data == null ? null : data.getSelection();
    }

    @Override
    public Shape get2DProjection(Player user)
    {
        throw new UnsupportedOperationException("Not supported yet!"); // TODO Shape.projectOnto(Plane)
    }

    @Override
    public <T extends Shape> T getSelection(Player user, Class<T> shape)
    {
        throw new UnsupportedOperationException("Not supported yet!");
    }

    @Override
    public Location<World> getPoint(Player user, int index)
    {
        SelectorData data = this.selectorData.get(user.getUniqueId());
        return data == null ? null : data.getPoint(index);
    }

    @Listener
    public void onInteract(InteractBlockEvent event, @First Player player)
    {
        if (event.getTargetBlock() == BlockSnapshot.NONE)
        {
            return;
        }
        Location block = event.getTargetBlock().getLocation().get();
        if ((int)block.getPosition().length() == 0)
        {
            return;
        }
        Optional<ItemStack> itemInHand = player.getItemInHand(HandTypes.MAIN_HAND);
        if (!itemInHand.isPresent() || !"Selector-Tool".equals(itemInHand.get().get(Keys.DISPLAY_NAME).map(Text::toPlain).orElse("")))
        {
            return;
        }
        if (block.getBlockType() == AIR || !player.hasPermission(module.getSelectPerm().getId()))
        {
            return;
        }

        SelectorData data = selectorData.computeIfAbsent(player.getUniqueId(), k -> new SelectorData());
        if (event instanceof InteractBlockEvent.Primary)
        {
            data.setPoint(0, block);
            i18n.sendTranslated(player, POSITIVE, "First position set to ({integer}, {integer}, {integer}).", block.getBlockX(), block.getBlockY(), block.getBlockZ());
        }
        else if (event instanceof InteractBlockEvent.Secondary)
        {
            data.setPoint(1, block);
            i18n.sendTranslated(player, POSITIVE, "Second position set to ({integer}, {integer}, {integer}).", block.getBlockX(), block.getBlockY(), block.getBlockZ());
        }
        event.setCancelled(true);
    }

    @Override
    public void setPoint(Player user, int index, Location<World> loc)
    {
        SelectorData data = selectorData.computeIfAbsent(user.getUniqueId(), k -> new SelectorData());
        data.setPoint(index, loc);
    }
}
