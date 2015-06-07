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
package de.cubeisland.engine.module.selector;

import javax.inject.Inject;
import com.google.common.base.Optional;
import de.cubeisland.engine.modularity.asm.marker.Enable;
import de.cubeisland.engine.modularity.asm.marker.ServiceImpl;
import de.cubeisland.engine.modularity.asm.marker.Version;
import de.cubeisland.engine.module.core.sponge.EventManager;
import de.cubeisland.engine.module.core.util.formatter.MessageType;
import de.cubeisland.engine.module.service.Selector;
import de.cubeisland.engine.module.service.command.CommandManager;
import de.cubeisland.engine.module.service.permission.Permission;
import de.cubeisland.engine.module.service.permission.PermissionManager;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.core.util.ChatFormat;
import de.cubeisland.engine.module.core.util.math.shape.Shape;

import de.cubeisland.engine.module.service.user.UserManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.manipulator.DisplayNameData;
import org.spongepowered.api.entity.EntityInteractionType;
import org.spongepowered.api.entity.EntityInteractionTypes;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.entity.player.PlayerInteractBlockEvent;
import org.spongepowered.api.event.entity.player.PlayerInteractEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;

import static de.cubeisland.engine.module.core.util.formatter.MessageType.POSITIVE;


@ServiceImpl(Selector.class)
@Version(1)
public class CuboidSelector implements Selector
{
    @Inject private de.cubeisland.engine.module.selector.Selector module;
    @Inject private EventManager em;
    @Inject private CommandManager cm;
    @Inject private PermissionManager pm;
    @Inject private UserManager um;
    @Inject private Game game;
    private Permission selectPerm;

    @Enable
    public void onEnable()
    {
        em.registerListener(module, this);
        cm.addCommands(module, new SelectorCommand(game));
        selectPerm = module.getProvided(Permission.class).child("use-wand");
        pm.registerPermission(module, selectPerm);
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
        Location block = event.getBlock();
        if ((int)block.getPosition().length() == 0)
        {
            return;
        }
        if (block.getType() == BlockTypes.AIR || !event.getUser().hasPermission(selectPerm.getFullName()))
        {
            return;
        }
        Optional<ItemStack> itemInHand = event.getUser().getItemInHand();
        if (!itemInHand.isPresent() || !Texts.of(TextColors.BLUE, "Selector-Tool").equals(itemInHand.get().getOrCreate(
            DisplayNameData.class).get().getDisplayName()))
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
