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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType;
import org.cubeengine.module.elevator.data.ElevatorData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent.Secondary;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;
import org.spongepowered.plugin.PluginContainer;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Singleton
public class ElevatorListener
{
    private I18n i18n;
    private Elevator module;
    private PluginContainer plugin;

    @Inject
    public ElevatorListener(I18n i18n, Elevator module, PluginContainer plugin)
    {
        this.i18n = i18n;
        this.module = module;
        this.plugin = plugin;
    }

    @Listener
    public void onInteractBlock(InteractBlockEvent event, @Root ServerPlayer player)
    {
        if (event instanceof Cancellable)
        {
            if (((Cancellable)event).isCancelled())
            {
                return;
            }
        }
        if (!event.context().get(EventContextKeys.USED_HAND).map(h -> h == HandTypes.MAIN_HAND.get()).orElse(false))
        {
            return;
        }

        if (!event.block().location().flatMap(Location::blockEntity).map(l -> l.supports(Keys.SIGN_LINES)).orElse(false))
        {
            return;
        }

        final ServerLocation loc = event.block().location().get();

        final Optional<Vector3i> target = loc.get(ElevatorData.TARGET);
        final Optional<UUID> owner = loc.get(ElevatorData.OWNER);
        final ItemStack itemInHand = player.itemInHand(HandTypes.MAIN_HAND);

        Boolean sneak = player.get(Keys.IS_SNEAKING).orElse(false);
        if (sneak)
        {
            if (!owner.isPresent())
            {
                if (!(event instanceof InteractBlockEvent.Primary))
                {
                    return; // Only Punch to activate
                }

                if (!itemInHand.isEmpty())
                {
                    if (player.hasPermission(module.getPerm().CREATE.getId()) && itemInHand.type().equals(module.getConfig().creationItem))
                    {
                        loc.offer(ElevatorData.OWNER, player.uniqueId());
                        if (!player.get(Keys.GAME_MODE).map(mode -> mode.equals(GameModes.CREATIVE.get())).orElse(false))
                        {
                            itemInHand.setQuantity(itemInHand.quantity() - 1);
                        }
                        player.setItemInHand(HandTypes.MAIN_HAND, itemInHand);

                        List<Component> list = loc.get(Keys.SIGN_LINES).get();
                        // Set First Line with name of renamed Item
                        list.set(0, itemInHand.get(Keys.CUSTOM_NAME).orElse(list.get(0)));
                        loc.offer(Keys.SIGN_LINES, list);

                        i18n.send(ChatType.ACTION_BAR, player, POSITIVE, "Elevator created!");
                        updateSign(loc, null);

                        player.world().playSound(Sound.sound(SoundTypes.ENTITY_ENDER_EYE_DEATH, Source.PLAYER, 5f, 1), loc.position());
                        if (event instanceof Cancellable)
                        {
                            ((Cancellable)event).setCancelled(true);
                        }
                    }
                }
            }
            else if (itemInHand.isEmpty()) // Sign has Elevator Data and hand is empty
            {
                if (player.hasPermission(module.getPerm().ADJUST.getId()))
                {
                    // Search order dependent on click
                    final Vector3i newTarget = findNextSign(loc, target.orElse(null), loc.blockPosition(), event instanceof InteractBlockEvent.Primary);
                    if (newTarget == null)
                    {
                        player.world().playSound(Sound.sound(SoundTypes.ENTITY_ENDERMAN_AMBIENT, Source.PLAYER, 5f, 10), loc.position());
                    }
                    else
                    {
                        player.world().playSound(Sound.sound(SoundTypes.ENTITY_ENDER_EYE_DEATH, Source.PLAYER, 5f, 1), loc.position());
                    }
                    updateSign(loc, newTarget);
                    if (event instanceof Cancellable)
                    {
                        ((Cancellable)event).setCancelled(true);
                    }
                }
            }
            else if (itemInHand.type().isAnyOf(ItemTypes.PAPER) && event instanceof InteractBlockEvent.Primary)
            {
                if (player.hasPermission(module.getPerm().RENAME.getId()))
                {
                    List<Component> list = loc.get(Keys.SIGN_LINES).get();
                    // Set First Line with name of renamed Item
                    list.set(0, itemInHand.get(Keys.CUSTOM_NAME).orElse(list.get(0)));
                    loc.offer(Keys.SIGN_LINES, list);
                    i18n.send(ChatType.ACTION_BAR, player, POSITIVE, "Elevator name changed!");
                    player.world().playSound(Sound.sound(SoundTypes.BLOCK_WOOL_BREAK, Source.PLAYER, 5f, 10), loc.position());
                    Sponge.server().scheduler().submit(Task.builder().plugin(plugin).execute(() ->
                        player.world().playSound(Sound.sound(SoundTypes.BLOCK_WOOL_BREAK, Source.PLAYER, 5f, 10), loc.position())).delay(Ticks.of(2)).build());
                    Sponge.server().scheduler().submit(Task.builder().plugin(plugin).execute(() ->
                       player.world().playSound(Sound.sound(SoundTypes.BLOCK_WOOL_BREAK, Source.PLAYER, 2f, 1), loc.position())).delay(Ticks.of(4)).build());
                    if (event instanceof Cancellable)
                    {
                        ((Cancellable)event).setCancelled(true);
                    }
                }
            }
            return;
        }
        // else no sneak

        if (event instanceof InteractBlockEvent.Secondary && player.hasPermission(module.getPerm().USE.getId()))
        {
            if (target.isPresent())
            {
                Vector3i sign = target.get();
                Vector3d pPos = player.location().position();
                if (!player.world().get(target.get(), ElevatorData.OWNER).isPresent())
                {
                    updateSign(loc, null);
                    ((Secondary)event).setCancelled(true);
                    return;
                }
                ServerLocation targetLoc = ServerLocation.of(player.world(), pPos.getX(), sign.getY() -1, pPos.getZ());
                final Optional<ServerLocation> safeLoc = Sponge.server().teleportHelper().findSafeLocation(targetLoc);
                if (safeLoc.isPresent())
                {
                    player.setLocation(safeLoc.get());
                    player.world().playSound(Sound.sound(SoundTypes.ITEM_CHORUS_FRUIT_TELEPORT, Source.PLAYER, 5f, 10f), safeLoc.get().position());
                    final ParticleEffect particle = ParticleEffect.builder().type(ParticleTypes.PORTAL).quantity(50).offset(Vector3d.from(0.2, 0.5, 0.2)).build();
                    player.world().spawnParticles(particle, safeLoc.get().position().add(0, 1, 0));
                    player.world().spawnParticles(particle, pPos.add(0,1,0));
                }
                else
                {
                    i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "Target obstructed");
                }
                ((Secondary)event).setCancelled(true);
            }
            else if (owner.isPresent())
            {
                player.world().playSound(Sound.sound(SoundTypes.ENTITY_ENDERMAN_AMBIENT, Source.PLAYER, 5f, 10), loc.position());
                updateSign(loc, null);
            }
        }

        if (event instanceof InteractBlockEvent.Secondary && !itemInHand.isEmpty())
        {
            if (player.hasPermission(module.getPerm().CREATE.getId()) && itemInHand.type().isAnyOf(module.getConfig().creationItem))
            {
                ((Secondary)event).setCancelled(true);
            }
        }
    }

    private void updateSign(ServerLocation loc, Vector3i target)
    {
        final Component liftLine = Component.text(module.getConfig().liftDecor + " Lift " + module.getConfig().liftDecor);
        Component targetLine = Component.text("No Target");
        Component directionLine = Component.text(module.getConfig().upDecor + " SNEAK " + module.getConfig().downDecor);
        if (target != null)
        {
            final Optional<List<Component>> lines = loc.world().get(target, Keys.SIGN_LINES);
            targetLine = lines.map(l -> l.get(0)).orElse(targetLine);
            int blocks = loc.blockY() - target.getY();
            String decor = blocks < 0 ? module.getConfig().upDecor : module.getConfig().downDecor;
            directionLine = Component.text(decor + " " + Math.abs(blocks) + " " + decor);
            loc.offer(ElevatorData.TARGET, target);
        }
        else
        {
            loc.remove(ElevatorData.TARGET);
        }

        List<Component> list = loc.get(Keys.SIGN_LINES).get();
        list.set(1, liftLine);
        list.set(2, targetLine);
        list.set(3, directionLine);
        loc.offer(Keys.SIGN_LINES, list);
    }

    private Vector3i findNextSign(ServerLocation loc, Vector3i previous, Vector3i startPos, boolean up)
    {
        startPos = previous == null ? startPos : previous;
        // Search for next Elevator sign

        final int max = loc.world().blockMax().getY();
        final int min = loc.world().blockMin().getY();
        final int blockX = loc.blockX();
        final int blockZ = loc.blockZ();
        final ServerWorld world = loc.world();

        int blockY = startPos.getY();
        while (blockY <= max && blockY >= min)
        {
            if (up)
            {
                blockY++;
            }
            else
            {
                blockY--;
            }
            if (blockY == loc.blockY())
            {
                continue;
            }
            if (world.get(blockX, blockY, blockZ, ElevatorData.OWNER).isPresent())
            {
                return new Vector3i(blockX, blockY, blockZ);
            }
        }
        // nothing found? Return same location as before when it is valid
        if (previous != null && world.get(blockX, previous.getY(), blockZ, ElevatorData.OWNER).isPresent())
        {
            return previous;
        }
        return null;
    }
}
