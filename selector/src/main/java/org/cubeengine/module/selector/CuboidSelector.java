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

import javax.inject.Inject;
import com.google.common.base.Optional;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.modularity.asm.marker.ServiceImpl;
import de.cubeisland.engine.modularity.asm.marker.Version;
import org.cubeengine.module.core.sponge.EventManager;
import org.cubeengine.service.Selector;
import org.cubeengine.service.user.User;
import org.cubeengine.module.core.util.math.shape.Shape;

import org.cubeengine.service.user.UserManager;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.EntityInteractionType;
import org.spongepowered.api.entity.EntityInteractionTypes;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.entity.player.PlayerInteractBlockEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;

import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.block.BlockTypes.AIR;


@ServiceImpl(Selector.class)
@Version(1)
public class CuboidSelector implements Selector
{
    @Inject private org.cubeengine.module.selector.Selector module;
    @Inject private EventManager em;
    @Inject private UserManager um;

    @Enable
    public void onEnable()
    {
        em.registerListener(module, this);
    }

    @Override
    public Shape getSelection(User user)
    {
        SelectorAttachment attachment = user.attachOrGet(SelectorAttachment.class, this.module);
        return attachment.getSelection();
    }

    @Override
    public Shape get2DProjection(User user)
    {
        throw new UnsupportedOperationException("Not supported yet!"); // TODO Shape.projectOnto(Plane)
    }

    @Override
    public <T extends Shape> T getSelection(User user, Class<T> shape)
    {
        throw new UnsupportedOperationException("Not supported yet!");
    }

    @Override
    public Location getFirstPoint(User user)
    {
        return this.getPoint(user, 0);
    }

    @Override
    public Location getSecondPoint(User user)
    {
        return this.getPoint(user, 1);
    }

    @Override
    public Location getPoint(User user, int index)
    {
        SelectorAttachment attachment = user.attachOrGet(SelectorAttachment.class, this.module);
        return attachment.getPoint(index);
    }

    @Subscribe
    public void onInteract(PlayerInteractBlockEvent event)
    {
        EntityInteractionType type = event.getInteractionType();
        Location block = event.getLocation();
        if ((int)block.getPosition().length() == 0)
        {
            return;
        }
        if (block.getBlockType() == AIR || !event.getUser().hasPermission(module.getSelectPerm().getId()))
        {
            return;
        }
        Optional<ItemStack> itemInHand = event.getUser().getItemInHand();
        if (!itemInHand.isPresent() || !Texts.of(TextColors.BLUE, "Selector-Tool").equals(itemInHand.get().get(Keys.DISPLAY_NAME).orNull()))
        {
            return;
        }
        User user = um.getExactUser(event.getUser().getUniqueId());
        SelectorAttachment logAttachment = user.attachOrGet(SelectorAttachment.class, this.module);
        if (EntityInteractionTypes.ATTACK == type)
        {
            logAttachment.setPoint(0, block);
            user.sendTranslated(POSITIVE, "First position set to ({integer}, {integer}, {integer}).", block.getBlockX(), block.getBlockY(), block.getBlockZ());
        }
        else if (EntityInteractionTypes.USE == type)
        {
            logAttachment.setPoint(1, block);
            user.sendTranslated(POSITIVE, "Second position set to ({integer}, {integer}, {integer}).", block.getBlockX(), block.getBlockY(), block.getBlockZ());
        }
        event.setCancelled(true);
    }
}
