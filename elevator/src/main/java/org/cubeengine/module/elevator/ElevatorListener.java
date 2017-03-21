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
    private ElevatorConfig config;

    public ElevatorListener(I18n i18n, ElevatorConfig config)
    {
        this.i18n = i18n;
        this.config = config;
    }

    @Listener
    public void onInteractBlock(InteractBlockEvent event, @Root Player player)
    {
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
            Optional<ItemStack> itemInHand = player.getItemInHand(HandTypes.MAIN_HAND);
            if (data == null)
            {
                if (!(event instanceof InteractBlockEvent.Primary))
                {
                    return; // Only Punch to activate
                }

                if (itemInHand.isPresent())
                {
                    if (itemInHand.get().getItem() == ItemTypes.ENDER_PEARL)  // TODO config
                    {
                        data = new ElevatorData();
                        data.setOwner(player.getUniqueId());
                        loc.offer(data);
                        ItemStack item = itemInHand.get();
                        item.setQuantity(item.getQuantity() - 1);
                        player.setItemInHand(HandTypes.MAIN_HAND, item);

                        List<Text> list = loc.get(Keys.SIGN_LINES).get();
                        // Set First Line with name of renamed Item
                        list.set(0, itemInHand.get().get(Keys.DISPLAY_NAME).orElse(list.get(0)));
                        loc.offer(Keys.SIGN_LINES, list);

                        i18n.sendTranslated(ACTION_BAR, player, POSITIVE, "Elevator created!");
                        updateSign(loc, data);
                        event.setCancelled(true);
                    }
                }
            }
            else if (!itemInHand.isPresent()) // Sign has Elevator Data and hand is empty
            {
                // Search order dependent on click
                Vector3i target = data.getTarget();
                target = findNextSign(loc, target == null ? loc.getPosition() : target.toDouble(), event instanceof InteractBlockEvent.Primary);
                data.setTarget(target);
                updateSign(loc, data);
                event.setCancelled(true);
            }
            else if (itemInHand.get().getItem() == ItemTypes.PAPER && event instanceof InteractBlockEvent.Primary)
            {
                List<Text> list = loc.get(Keys.SIGN_LINES).get();
                // Set First Line with name of renamed Item
                list.set(0, itemInHand.get().get(Keys.DISPLAY_NAME).orElse(list.get(0)));
                loc.offer(Keys.SIGN_LINES, list);
                i18n.sendTranslated(ACTION_BAR, player, POSITIVE, "Elevator name changed!");
                event.setCancelled(true);
            }
            return;
        }
        // else no sneak

        Optional<Vector3i> target = event.getTargetBlock().get(IElevatorData.TARGET);
        if (target.isPresent())
        {
            if (loc.getExtent().get(target.get(), ElevatorData.class).isPresent())
            {
                Vector3i sign = target.get();
                Vector3d pPos = player.getLocation().getPosition();
                Location<World> targetLoc = new Location<>(player.getWorld(), pPos.getX(), sign.getY() - 1, pPos.getZ());
                if (!player.setLocationSafely(targetLoc)) // TODO signs are not safe wtf?
                {
                    i18n.sendTranslated(ACTION_BAR, player, NEGATIVE, "Target obstructed");
                }
                event.setCancelled(true);
            }
            else
            {
                i18n.sendTranslated(ACTION_BAR, player, NEGATIVE, "Target sign was destroyed!");
                event.setCancelled(true);
            }
        }

    }

    private void updateSign(Location<World> loc, ElevatorData data)
    {
        Text liftLine = Text.of(config.liftDecor + " Lift " + config.liftDecor);
        Text targetLine = Text.of("No Target");
        Text directionLine = Text.EMPTY;
        if (data.getTarget() != null)
        {
            Optional<List<Text>> lines = loc.getExtent().get(data.getTarget(), Keys.SIGN_LINES);
            targetLine = lines.map(l -> l.get(0)).orElse(targetLine);
            int blocks = loc.getBlockY() - data.getTarget().getY();
            char decor = blocks < 0 ? config.upDecor : config.downDecor;
            directionLine = Text.of(decor + " ",  Math.abs(blocks), " " + decor);
        }

        List<Text> list = loc.get(Keys.SIGN_LINES).get();
        list.set(1, liftLine);
        list.set(2, targetLine);
        list.set(3, directionLine);
        loc.offer(Keys.SIGN_LINES, list);
        loc.offer(data);
    }

    private Vector3i findNextSign(Location<World> loc, Vector3d previous, boolean up)
    {
        // Search for next Elevator sign
        BlockRay<World> ray = BlockRay.from(loc.getExtent(), previous)
                .direction(new Vector3d(0, up ? 1 : -1, 0))
                .narrowPhase(false)
                .stopFilter(b -> b.getBlockY() <= loc.getExtent().getBlockMax().getY())
                .stopFilter(b -> b.getBlockY() >= loc.getExtent().getBlockMin().getY()).build();
        while (ray.hasNext())
        {
            BlockRayHit<World> next = ray.next();
            if (next.getBlockPosition().equals(previous.toInt()))
            {
                continue;
            }
            Optional<ElevatorData> targetData = next.getLocation().get(ElevatorData.class);
            if (targetData.isPresent() && !next.getBlockPosition().equals(loc.getBlockPosition()))
            {
                return next.getBlockPosition();
            }
        }

        /*
        // Continue Search for next Elevator sign from max or min Y location
        Vector3d start2 = new Vector3d(previous.getX(), up ? 0 : loc.getExtent().getBlockMax().getY(), previous.getZ());
        ray = BlockRay.from(loc.getExtent(), start2)
                .direction(new Vector3d(0, up ? 1 : -1, 0))
                .narrowPhase(false)
                .stopFilter(b -> b.getBlockY() <= loc.getExtent().getBlockMax().getY())
                .stopFilter(b -> b.getBlockY() >= loc.getExtent().getBlockMin().getY()).build();
        while (ray.hasNext())
        {
            BlockRayHit<World> next = ray.next();
            if (next.getBlockPosition().equals(loc.getBlockPosition()))
            {
                break;
            }
            Optional<ElevatorData> targetData = next.getLocation().get(ElevatorData.class);
            if (targetData.isPresent())
            {
                return next.getBlockPosition();
            }
        }
        */

        // nothing found? Return same location as before when it is valid
        Optional<ElevatorData> targetData = loc.getExtent().get(previous.toInt(), ElevatorData.class);
        return targetData.isPresent() ? previous.toInt() : null;
    }
}
