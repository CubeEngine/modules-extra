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
package de.cubeisland.engine.module.fun.commands;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import de.cubeisland.engine.butler.filter.Restricted;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Named;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.core.permission.Permission;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.matcher.Match;
import de.cubeisland.engine.module.fun.Fun;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import static de.cubeisland.engine.core.util.formatter.MessageType.*;

public class ThrowCommands
{
    private final Map<UUID, ThrowTask> thrownItems;
    // entities that can't be safe due to bukkit flaws
    private final EnumSet<EntityType> BUGGED_ENTITIES = EnumSet.of(EntityType.SMALL_FIREBALL, EntityType.FIREBALL);

    private final Fun module;
    private final ThrowListener throwListener;

    Map<EntityType, Permission> perms = new HashMap<>();

    public ThrowCommands(Fun module)
    {
        this.module = module;
        this.thrownItems = new HashMap<>();
        this.throwListener = new ThrowListener();
        module.getCore().getEventManager().registerListener(module, this.throwListener);
        for (EntityType type : EntityType.values()) // TODO only entities that can be thrown
        {
            perms.put(type, module.perms().COMMAND_THROW.child(type.name().toLowerCase(Locale.ENGLISH).replace("_", "-")));
            module.getCore().getPermissionManager().registerPermission(module, perms.get(type));
        }
    }

    @Command(name = "throw", desc = "Throw something!")
    @Restricted(value = User.class, msg = "This command can only be used by a player!")
    public void throwCommand(User context, String material, @Optional Integer amount,
                             @Named({ "delay", "d" }) Integer delay, @Flag boolean unsafe)
    {
        EntityType type = null;
        boolean showNotification = true;

        ThrowTask task = this.thrownItems.remove(context.getUniqueId());
        if (task != null)
        {
            int aDelay = delay == null ? task.getInterval() : delay;
            if (material == null || (type = Match.entity().any(material)) == task.getType()
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
            context.sendTranslated(NEGATIVE, "The amount must be a number from 1 to {integer}", this.module.getConfig().command.throwSection.maxAmount);
            return;
        }

        delay = delay == null ? 3 : delay;
        if (delay > this.module.getConfig().command.throwSection.maxDelay || delay < 0)
        {
            context.sendTranslated(NEGATIVE, "The delay must be a number from 0 to {integer}", this.module.getConfig().command.throwSection.maxDelay);
            return;
        }
        
        if(unsafe && !module.perms().COMMAND_THROW_UNSAFE.isAuthorized( context ) )
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to execute this command in unsafe mode.");
            return;
        }

        if (type == null)
        {
            type = Match.entity().any(material);
        }
        if (type == null)
        {
            context.sendTranslated(NEGATIVE, "The given object was not found!");
            return;
        }
        if (!type.isSpawnable())
        {
            context.sendTranslated(NEGATIVE, "The Item {name#item} is not supported!", material);
            return;
        }

        if (!perms.get(type).isAuthorized(context))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to throw this.");
            return;
        }

        if ((BUGGED_ENTITIES.contains(type) || Match.entity().isMonster(type)) && !unsafe)
        {
            context.sendTranslated(NEUTRAL, "This object can only be thrown in unsafe mode. Add -u to enable the unsafe mode.");
            return;
        }

        task = new ThrowTask(context, type, amount, delay, !unsafe);
        if (task.start(showNotification))
        {
            this.thrownItems.put(context.getUniqueId(), task);
        }
        else
        {
            context.sendTranslated(NEGATIVE, "Failed to throw this!");
        }
    }

    private class ThrowTask implements Runnable
    {
        private final EntityType type;
        private final User user;
        private final int interval;
        private final boolean save;
        private final boolean preventDamage;
        private int amount;
        private int taskId;

        public ThrowTask(User user, EntityType type, int amount, int interval, boolean preventDamage)
        {
            this.user = user;
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

        public User getUser()
        {
            return this.user;
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
                this.user.sendTranslated(POSITIVE, "Started throwing!");
                this.user.sendTranslated(POSITIVE, "You will keep throwing until you run this command again.");
            }
            this.taskId = module.getCore().getTaskManager().runTimer(module, this, 0, this.interval);
            return this.taskId != -1;
        }

        public void stop()
        {
            this.stop(true);
        }

        public void stop(boolean notify)
        {
            if (this.taskId != -1)
            {
                if (notify)
                {
                    if (this.amount == -1)
                    {
                        this.user.sendTranslated(POSITIVE, "You are no longer throwing.");
                    }
                    else
                    {
                        this.user.sendTranslated(POSITIVE, "All objects thrown.");
                    }
                }
                module.getCore().getTaskManager().cancelTask(module, this.taskId);
                this.taskId = -1;
            }
        }

        @SuppressWarnings("unchecked")
        private void throwItem()
        {
            final Location location = this.user.getEyeLocation();
            final Vector direction = location.getDirection();
            location.add(direction).add(direction);

            Entity entity;
            if (Projectile.class.isAssignableFrom(this.type.getEntityClass()))
            {
                entity = this.user.launchProjectile((Class<? extends Projectile>)this.type.getEntityClass());
            }
            else
            {
                entity = this.user.getWorld().spawnEntity(location, type);
                entity.setVelocity(direction.multiply(8));
                if (entity instanceof ExperienceOrb)
                {
                    ((ExperienceOrb)entity).setExperience(0);
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
                thrownItems.remove(this.user.getUniqueId());
            }
        }
    }

    public class ThrowListener implements Listener
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

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event)
        {
            ThrowTask task = thrownItems.remove(event.getPlayer().getUniqueId());
            if (task != null)
            {
                task.stop();
            }
        }

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onBlockDamage(EntityExplodeEvent event)
        {
            if (this.handleEntity(event.getEntity()))
            {
                event.blockList().clear();
            }
        }

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onEntityByEntityDamage(EntityDamageByEntityEvent event)
        {
            if (this.handleEntity(event.getEntity()))
            {
                event.setDamage(0);
            }
        }

        private boolean handleEntity(final Entity entity)
        {
            if (this.entities.contains(entity) && this.removal != entity)
            {
                module.getCore().getTaskManager().runTask(module, new Runnable()
                {
                    @Override
                    public void run()
                    {
                        entities.remove(removal);
                        removal = null;
                    }
                });
                return true;
            }
            return false;
        }
    }
}
