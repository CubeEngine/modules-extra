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
package org.cubeengine.module.fun.commands;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

import com.flowpowered.math.vector.Vector3d;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.matcher.EntityMatcher;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.libcube.util.CauseUtil;
import org.cubeengine.module.fun.Fun;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.property.entity.EyeHeightProperty;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.ExperienceOrb;
import org.spongepowered.api.entity.explosive.Explosive;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.entity.projectile.arrow.Arrow;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.explosion.Explosion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ThrowCommands
{
    private final Map<UUID, ThrowTask> thrownItems;
    
    // entities that can't be safe due to sponge flaws
    private final Set<EntityType> BUGGED_ENTITIES = new HashSet<>();

    {
        BUGGED_ENTITIES.add(EntityTypes.SMALL_FIREBALL);
        BUGGED_ENTITIES.add(EntityTypes.FIREBALL);
    }

    private TaskManager tm;
    private final Fun module;
    private EntityMatcher entityMatcher;
    private final ThrowListener throwListener;
    private I18n i18n;

    Map<EntityType, Permission> perms = new HashMap<>();

    public ThrowCommands(Fun module, EventManager em, PermissionManager pm, TaskManager tm, EntityMatcher entityMatcher, I18n i18n)
    {
        this.module = module;
        this.tm = tm;
        this.entityMatcher = entityMatcher;
        this.i18n = i18n;
        this.thrownItems = new HashMap<>();
        this.throwListener = new ThrowListener();
        em.registerListener(Fun.class, this.throwListener);
        for (EntityType type : Sponge.getRegistry().getAllOf(EntityType.class)) // TODO only entities that can be thrown
        {
            Permission perm = pm.register(Fun.class, type.getName().toLowerCase().replace("_", "-"), "", module.perms().COMMAND_THROW);
            perms.put(type, perm);
        }
    }

    @Command(name = "throw", desc = "Throw something!")
    @Restricted(value = Player.class, msg = "This command can only be used by a player!")
    public void throwCommand(Player context, String material, @Optional Integer amount,
                             @Named({ "delay", "d" }) Integer delay, @Flag boolean unsafe)
    {
        EntityType type = null;
        boolean showNotification = true;

        ThrowTask task = this.thrownItems.remove(context.getUniqueId());
        if (task != null)
        {
            int aDelay = delay == null ? task.getInterval() : delay;
            if (material == null || (type = entityMatcher.any(material, context.getLocale())) == task.getType()
                && task.getInterval() == aDelay
                && task.getPreventDamage() != unsafe && delay == null)
            {
                task.stop(true);
                return;
            }
            task.stop(showNotification = false);
        }

        amount = amount == null ? -1 : 1;
        if ((amount > this.module.getConfig().command.throwSection.maxAmount || amount < 1) && amount != -1)
        {
            i18n.sendTranslated(context, NEGATIVE, "The amount must be a number from 1 to {integer}", this.module.getConfig().command.throwSection.maxAmount);
            return;
        }

        delay = delay == null ? 3 : delay;
        if (delay > this.module.getConfig().command.throwSection.maxDelay || delay < 0)
        {
            i18n.sendTranslated(context, NEGATIVE, "The delay must be a number from 0 to {integer}", this.module.getConfig().command.throwSection.maxDelay);
            return;
        }
        
        if(unsafe && !context.hasPermission(module.perms().COMMAND_THROW_UNSAFE.getId()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to execute this command in unsafe mode.");
            return;
        }

        if (type == null)
        {
            type = entityMatcher.any(material, context.getLocale());
        }
        if (type == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "The given object was not found!");
            return;
        }

        // TODO
        /*
        if (!type.isSpawnable())
        {
            i18n.sendTranslated(context, NEGATIVE, "The Item {name#item} is not supported!", material);
            return;
        }
        */

        if (!context.hasPermission(perms.get(type).getId()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to throw this.");
            return;
        }

        if ((BUGGED_ENTITIES.contains(type) || entityMatcher.isMonster(type)) && !unsafe)
        {
            i18n.sendTranslated(context, NEUTRAL, "This object can only be thrown in unsafe mode. Add -u to enable the unsafe mode.");
            return;
        }

        task = new ThrowTask(context, type, amount, delay, !unsafe);
        if (task.start(showNotification))
        {
            this.thrownItems.put(context.getUniqueId(), task);
        }
        else
        {
            i18n.sendTranslated(context, NEGATIVE, "Failed to throw this!");
        }
    }

    private class ThrowTask implements Runnable
    {
        private final EntityType type;
        private final Player player;
        private final int interval;
        private final boolean save;
        private final boolean preventDamage;
        private int amount;
        private UUID taskId;

        public ThrowTask(Player player, EntityType type, int amount, int interval, boolean preventDamage)
        {
            this.player = player;
            this.type = type;
            this.amount = amount;
            this.interval = interval;
            this.preventDamage = preventDamage;
            this.save = this.isSafe(type.getEntityClass());
        }

        private boolean isSafe(Class entityClass)
        {
            return !(Explosive.class.isAssignableFrom(entityClass) || Arrow.class == entityClass);
        }

        public Player getPlayer()
        {
            return this.player;
        }

        public EntityType getType()
        {
            return this.type;
        }

        public int getInterval()
        {
            return this.interval;
        }

        public boolean getPreventDamage()
        {
            return this.preventDamage;
        }

        public boolean start()
        {
            return this.start(true);
        }

        public boolean start(boolean notify)
        {
            if (this.amount == -1 && notify)
            {
                i18n.sendTranslated(this.player, POSITIVE, "Started throwing!");
                i18n.sendTranslated(this.player, POSITIVE, "You will keep throwing until you run this command again.");
            }
            this.taskId = tm.runTimer(Fun.class, this, 0, this.interval);
            return this.taskId != null;
        }

        public void stop()
        {
            this.stop(true);
        }

        public void stop(boolean notify)
        {
            if (this.taskId != null)
            {
                if (notify)
                {
                    if (this.amount == -1)
                    {
                        i18n.sendTranslated(this.player, POSITIVE, "You are no longer throwing.");
                    }
                    else
                    {
                        i18n.sendTranslated(this.player, POSITIVE, "All objects thrown.");
                    }
                }
                tm.cancelTask(Fun.class, this.taskId);
                this.taskId = null;
            }
        }

        @SuppressWarnings("unchecked")
        private void throwItem()
        {

            Vector3d rotation = player.getTransform().getRotationAsQuaternion().getDirection().normalize();
            Location location = this.player.getLocation()
                    .add(0, player.getProperty(EyeHeightProperty.class).get().getValue(), 0)
                    .add(rotation.mul(2));

            Entity entity;
            if (Projectile.class.isAssignableFrom(this.type.getEntityClass()))
            {
                entity = this.player.launchProjectile((Class<? extends Projectile>)this.type.getEntityClass()).get();
            }
            else
            {
                entity = player.getWorld().createEntity(type, location.getPosition());
                player.getWorld().spawnEntity(entity, CauseUtil.spawnCause(player));
                entity.setVelocity(rotation.mul(8));
                if (entity instanceof ExperienceOrb)
                {
                    entity.offer(Keys.CONTAINED_EXPERIENCE, 0);
                }
            }
            if (this.preventDamage && !this.save)
            {
                throwListener.add(entity);
            }
        }

        @Override
        public void run()
        {
            this.throwItem();
            if (this.amount > 0)
            {
                this.amount--;
            }
            if (amount == 0)
            {
                this.stop();
                thrownItems.remove(this.player.getUniqueId());
            }
        }
    }

    public class ThrowListener
    {
        private final Set<Entity> entities;
        private Entity removal;

        public ThrowListener()
        {
            this.entities = new HashSet<>();
            this.removal = null;
        }

        public void add(Entity entity)
        {
            this.entities.add(entity);
        }

        @Listener
        public void onPlayerQuit(ClientConnectionEvent.Disconnect event)
        {
            ThrowTask task = thrownItems.remove(event.getTargetEntity().getUniqueId());
            if (task != null)
            {
                task.stop();
            }
        }

        @Listener
        public void onBlockDamage(ExplosionEvent.Pre event, @First Entity cause)
        {
            if (this.handleEntity(cause))
            {
                event.setExplosion(Explosion.builder().from(event.getExplosion()).shouldBreakBlocks(false).shouldDamageEntities(false).build());
            }
        }

        private boolean handleEntity(final Entity entity)
        {
            if (this.entities.contains(entity) && this.removal != entity)
            {
                tm.runTask(Fun.class, () -> {
                    entities.remove(removal);
                    removal = null;
                });
                return true;
            }
            return false;
        }
    }
}
