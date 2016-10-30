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
package org.cubeengine.module.fun.commands;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import com.flowpowered.math.vector.Vector3d;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.command.CommandContext;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.module.fun.Fun;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.property.block.SolidCubeProperty;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSources;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.spongepowered.api.effect.particle.ParticleTypes.SMOKE;

public class RocketCommand
{
    private final Fun module;
    private TaskManager tm;
    private final RocketListener rocketListener;

    public RocketCommand(Fun module, EventManager em, TaskManager tm)
    {
        this.module = module;
        this.tm = tm;
        this.rocketListener = new RocketListener();
        em.registerListener(Fun.class, rocketListener);
    }

    public RocketListener getRocketListener()
    {
        return this.rocketListener;
    }

    @Command(desc = "Shoots a player upwards with a cool smoke effect")
    public void rocket(CommandContext context, @Default Player player, @Optional Integer height)
    {
        height = height == null ? 10 : height;
        if (height > this.module.getConfig().command.rocket.maxHeight)
        {
            context.sendTranslated(NEGATIVE, "Do you never wanna see {user} again?", player);
            return;
        }
        else if (height < 0)
        {
            context.sendTranslated(NEGATIVE, "The height has to be greater than 0");
            return;
        }

        rocketListener.addInstance(player, height);
    }

    public class RocketListener implements Runnable
    {
        private final Set<RocketCMDInstance> instances = new HashSet<>();
        private UUID taskId;

        public void addInstance(Player user, int height)
        {
            if (!this.contains(user))
            {
                instances.add(new RocketCMDInstance(user.getUniqueId(), height));

                if (taskId == null)
                {
                    this.taskId = tm.runTimer(Fun.class, this, 0, 2);
                }
            }
        }

        public Collection<Player> getUsers()
        {
            Set<Player> users = new HashSet<>();
            for (RocketCMDInstance instance : instances)
            {
                users.add(instance.getUser());
            }
            return users;
        }

        public boolean contains(Player aUser)
        {
            for (Player user : this.getUsers())
            {
                if (user.equals(aUser))
                {
                    return true;
                }
            }
            return false;
        }

        public void removeInstance(Player user)
        {
            RocketCMDInstance trash = null;
            
            for (RocketCMDInstance instance : instances)
            {
                if (instance.getUuid().equals(user.getUniqueId()))
                {
                    trash = instance;
                    break;
                }
            }
            
            if(trash != null)
            {
                this.instances.remove(trash);

                if (instances.isEmpty())
                {
                    tm.cancelTask(Fun.class, taskId);
                    taskId = null;
                }
            }
        }

        public Collection<RocketCMDInstance> getInstances()
        {
            return this.instances;
        }

        @Listener
        public void onEntityDamage(DamageEntityEvent event, @First DamageSource source)
        {
            if (event.getTargetEntity() instanceof Player && source.getType() == DamageSources.FALLING)
            {
                if (this.contains(((Player)event.getTargetEntity())))
                {
                    event.setCancelled(true);
                    this.removeInstance(((Player)event.getTargetEntity()));
                }
            }
        }

        @Override
        public void run()
        {
            for (RocketCMDInstance instance : this.getInstances())
            {
                final Player player = instance.getUser();

                if (player.isOnline())
                {
                    if (!instance.getDown())
                    {
                        Location userLocation = player.getLocation();
                        Vector3d pos = userLocation.getPosition();
                        ParticleEffect effect = ParticleEffect.builder().type(SMOKE).build();
                        player.getWorld().spawnParticles(effect, pos);
                        player.getWorld().spawnParticles(effect, pos.add(1, 0, 0));
                        player.getWorld().spawnParticles(effect, pos.add(-1, 0, 0));
                        player.getWorld().spawnParticles(effect, pos.add(0, 0, 1));
                        player.getWorld().spawnParticles(effect, pos.add(0, 0, -1));
                    }

                    if (instance.getNumberOfAirBlocksUnderFeet() == 0 && instance.getDown())
                    {
                        tm.runTaskDelayed(Fun.class, () -> removeInstance(player), 1);
                    }

                    if (instance.getNumberOfAirBlocksUnderFeet() < instance.getHeight() && instance.getNumberOfAirBlocksOverHead() > 2 && !instance.getDown())
                    {
                        double y = (double)(instance.getHeight() - instance.getNumberOfAirBlocksUnderFeet()) / 10;
                        y = (y < 10) ? y : 10;
                        player.setVelocity(new Vector3d(0, (y < 9) ? (y + 1) : y, 0));
                    }
                    else if (!instance.getDown())
                    {
                        instance.setDown();
                    }
                }
                else
                {
                    this.removeInstance(player);
                }
            }
        }

        private class RocketCMDInstance
        {
            private final UUID uuid;
            private final int height;
            private boolean down;

            private RocketCMDInstance(UUID uuid, int height)
            {
                this.uuid = uuid;
                this.height = height;
                this.down = false;
            }

            public void setDown()
            {
                this.down = true;
            }

            public boolean getDown()
            {
                return this.down;
            }

            public int getHeight()
            {
                return this.height;
            }

            public Player getUser()
            {
                return Sponge.getServer().getPlayer(uuid).orElse(null);
            }

            public UUID getUuid()
            {
                return this.uuid;
            }

            public int getNumberOfAirBlocksOverHead()
            {
                final Player user = this.getUser();
                if (user == null)
                {
                    return 0;
                }
                final Location<World> location = this.getUser().getLocation();
                if (location == null)
                {
                    return 0;
                }
                location.add(0, 1, 0);
                int numberOfAirBlocks = 0;

                while (!location.getBlock().getType().getProperty(SolidCubeProperty.class).isPresent() && location.getY() < location.getExtent().getDimension().getHeight())
                {
                    numberOfAirBlocks++;
                    location.add(0, 1, 0);
                }

                return numberOfAirBlocks;
            }

            public int getNumberOfAirBlocksUnderFeet()
            {
                final Player user = this.getUser();
                if (user == null)
                {
                    return 0;
                }
                final Location<World> location = this.getUser().getLocation();
                if (location == null)
                {
                    return 0;
                }
                location.add(0, -1, 0);
                int numberOfAirBlocks = 0;

                while (!location.getBlock().getProperty(SolidCubeProperty.class).isPresent() || location.getY() > location.getExtent().getDimension().getHeight())
                {
                    numberOfAirBlocks++;
                    location.add(0, -1, 0);
                }

                return numberOfAirBlocks;
            }
        }
    }
}
