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
package org.cubeengine.module.elevator;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.text.chat.ChatTypes.ACTION_BAR;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.elevator.data.ElevatorData;
import org.cubeengine.module.elevator.data.IElevatorData;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.Optional;

public class ElevatorListener
{
    private I18n i18n;
    private Elevator module;

    public ElevatorListener(I18n i18n, Elevator module)
    {
        this.i18n = i18n;
        this.module = module;
    }

    @Listener
    public void onInteractBlock(InteractBlockEvent event, @Root Player player)
    {
        if (!(event instanceof InteractBlockEvent.Primary.MainHand) && !(event instanceof InteractBlockEvent.Secondary.MainHand))
        {
            return;
        }
        BlockType type = event.getTargetBlock().getState().getType();
        if (type != BlockTypes.STANDING_SIGN && type != BlockTypes.WALL_SIGN)
        {
            return;
        }
        Location<World> loc = event.getTargetBlock().getLocation().get();
        ElevatorData data = loc.get(ElevatorData.class).orElse(null);
        Boolean sneak = player.get(Keys.IS_SNEAKING).orElse(false);
        if (sneak)
        {
            ItemStack itemInHand = player.getItemInHand(HandTypes.MAIN_HAND).orElse(ItemStack.empty());
            if (data == null)
            {
                if (!(event instanceof InteractBlockEvent.Primary))
                {
                    return; // Only Punch to activate
                }

                if (!itemInHand.isEmpty())
                {
                    if (player.hasPermission(module.getPerm().CREATE.getId()) && itemInHand.getType().equals(module.getConfig().creationItem))
                    {
                        data = new ElevatorData();
                        data.setOwner(player.getUniqueId());
                        loc.offer(data);
                        itemInHand.setQuantity(itemInHand.getQuantity() - 1);
                        player.setItemInHand(HandTypes.MAIN_HAND, itemInHand);

                        List<Text> list = loc.get(Keys.SIGN_LINES).get();
                        // Set First Line with name of renamed Item
                        list.set(0, itemInHand.get(Keys.DISPLAY_NAME).orElse(list.get(0)));
                        loc.offer(Keys.SIGN_LINES, list);

                        i18n.send(ACTION_BAR, player, POSITIVE, "Elevator created!");
                        updateSign(loc, data);
                        event.setCancelled(true);
                    }
                }
            }
            else if (itemInHand.isEmpty()) // Sign has Elevator Data and hand is empty
            {
                if (player.hasPermission(module.getPerm().ADJUST.getId()))
                {
                    // Search order dependent on click
                    Vector3i target = data.getTarget();
                    target = findNextSign(loc, target, loc.getBlockPosition(), event instanceof InteractBlockEvent.Primary);
                    data.setTarget(target);
                    updateSign(loc, data);
                    event.setCancelled(true);
                }
            }
            else if (itemInHand.getType() == ItemTypes.PAPER && event instanceof InteractBlockEvent.Primary)
            {
                if (player.hasPermission(module.getPerm().RENAME.getId()))
                {
                    List<Text> list = loc.get(Keys.SIGN_LINES).get();
                    // Set First Line with name of renamed Item
                    list.set(0, itemInHand.get(Keys.DISPLAY_NAME).orElse(list.get(0)));
                    loc.offer(Keys.SIGN_LINES, list);
                    i18n.send(ACTION_BAR, player, POSITIVE, "Elevator name changed!");
                    event.setCancelled(true);
                }
            }
            return;
        }
        // else no sneak

        if (event instanceof InteractBlockEvent.Secondary && player.hasPermission(module.getPerm().USE.getId()))
        {
            Optional<Vector3i> target = event.getTargetBlock().get(IElevatorData.TARGET);
            if (target.isPresent())
            {
                if (loc.getExtent().get(target.get(), ElevatorData.class).isPresent())
                {
                    Vector3i sign = target.get();
                    Vector3d pPos = player.getLocation().getPosition();
                    Location<World> targetLoc = new Location<>(player.getWorld(), pPos.getX(), sign.getY() - 1, pPos.getZ());
                    if (!player.setLocationSafely(targetLoc))
                    {
                        i18n.send(ACTION_BAR, player, NEGATIVE, "Target obstructed");
                    }
                    event.setCancelled(true);
                }
                else
                {
                    i18n.send(ACTION_BAR, player, NEGATIVE, "Target sign was destroyed!");
                    event.setCancelled(true);
                }
            }
        }

        if (event instanceof InteractBlockEvent.Secondary)
        {
            Optional<ItemStack> itemInHand = player.getItemInHand(HandTypes.MAIN_HAND);
            if (itemInHand.isPresent())
            {
                if (player.hasPermission(module.getPerm().CREATE.getId()) && itemInHand.get().getType().equals(module.getConfig().creationItem))
                {
                    event.setCancelled(true);
                }
            }
        }
    }

    private void updateSign(Location<World> loc, ElevatorData data)
    {
        Text liftLine = Text.of(module.getConfig().liftDecor + " Lift " + module.getConfig().liftDecor);
        Text targetLine = Text.of("No Target");
        Text directionLine = Text.EMPTY;
        if (data.getTarget() != null)
        {
            Optional<List<Text>> lines = loc.getExtent().get(data.getTarget(), Keys.SIGN_LINES);
            targetLine = lines.map(l -> l.get(0)).orElse(targetLine);
            int blocks = loc.getBlockY() - data.getTarget().getY();
            String decor = blocks < 0 ? module.getConfig().upDecor : module.getConfig().downDecor;
            directionLine = Text.of(decor + " ",  Math.abs(blocks), " " + decor);
        }

        List<Text> list = loc.get(Keys.SIGN_LINES).get();
        list.set(1, liftLine);
        list.set(2, targetLine);
        list.set(3, directionLine);
        loc.offer(Keys.SIGN_LINES, list);
        loc.offer(data);
    }

    private Vector3i findNextSign(Location<World> loc, Vector3i previous, Vector3i startPos, boolean up)
    {
        startPos = previous == null ? startPos : previous;
        // Search for next Elevator sign
        BlockRay<World> ray = BlockRay.from(loc.getExtent(), startPos.toDouble())
                .direction(new Vector3d(0, up ? 1 : -1, 0))
                .narrowPhase(false)
                .stopFilter(b -> b.getBlockY() <= loc.getExtent().getBlockMax().getY())
                .stopFilter(b -> b.getBlockY() >= loc.getExtent().getBlockMin().getY()).build();
        while (ray.hasNext())
        {
            BlockRayHit<World> next = ray.next();
            if (next.getBlockPosition().equals(startPos))
            {
                continue;
            }
            Optional<ElevatorData> targetData = next.getLocation().get(ElevatorData.class);
            if (targetData.isPresent() && !next.getBlockPosition().equals(loc.getBlockPosition()))
            {
                return next.getBlockPosition();
            }
        }

        // nothing found? Return same location as before when it is valid
        Optional<ElevatorData> targetData = loc.getExtent().get(startPos, ElevatorData.class);
        return targetData.isPresent() ? previous : null;
    }
}
