package de.cubeisland.cubeengine.fun.commands;

import de.cubeisland.cubeengine.core.command.CommandContext;
import de.cubeisland.cubeengine.core.command.annotation.Command;
import de.cubeisland.cubeengine.core.command.annotation.Flag;
import de.cubeisland.cubeengine.core.command.annotation.Param;
import de.cubeisland.cubeengine.core.permission.PermDefault;
import de.cubeisland.cubeengine.core.permission.PermissionManager;
import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.fun.Fun;
import de.cubeisland.cubeengine.fun.FunPerm;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import java.util.Set;
import static de.cubeisland.cubeengine.core.command.exception.IllegalParameterValue.illegalParameter;
import static de.cubeisland.cubeengine.core.command.exception.InvalidUsageException.invalidUsage;
import static de.cubeisland.cubeengine.core.command.exception.PermissionDeniedException.denyAccess;
import de.cubeisland.cubeengine.core.util.matcher.Match;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Fireball;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

public class ThrowCommands
{
    private final Map<String, ThrowTask> thrownItems;
    // entities that can't be safe due to bukkit flaws
    private final EnumSet<EntityType> BUGGED_ENTITES = EnumSet.of(EntityType.SMALL_FIREBALL, EntityType.FIREBALL);

    private static final String BASE_THROW_PERM = FunPerm.BASE + "throw.";

    private final Fun fun;
    private final ThrowListener throwListener;

    public ThrowCommands(Fun fun)
    {
        this.fun = fun;
        this.thrownItems = new THashMap<String, ThrowTask>();
        this.throwListener = new ThrowListener();
        fun.registerListener(this.throwListener);
        
        PermissionManager perm = fun.getCore().getPermissionManager();
        for (EntityType type : EntityType.values())
        {
            if (type.isSpawnable())
            {
                perm.registerPermission(fun, BASE_THROW_PERM + type.name().toLowerCase(Locale.ENGLISH).replace("_", "-"), PermDefault.OP);
            }
        }
    }

    @Command(
        names = "throw",
        desc = "Throw something!",
        max = 2,
        params = @Param(names = {"delay", "d"}, type = Integer.class),
        flags = @Flag(longName = "unsafe", name = "u"),
        usage = "<material> [amount] [delay <value>] [-unsafe]"
    )
    public void throwCommand(CommandContext context)
    {
        if (!(context.getSender() instanceof CommandSender))
        {
            context.sendMessage("fun", "&cThis command can only be used by a player!");
            return;
        }
        User user = (User)context.getSender();

        ThrowTask task = this.thrownItems.remove(user.getName());
        if (task != null)
        {
            task.stop();
            return;
        }
        
        if (context.getIndexed().isEmpty())
        {
            invalidUsage(context, "fun", "&cYou have to add the material you want to throw.");
        }
        
        int amount = context.getIndexed(1, Integer.class, -1);
        if ((amount > this.fun.getConfig().maxThrowNumber || amount < 1) && amount != -1)
        {
            illegalParameter(context, "fun", "&cThe amount has to be a number from 1 to %d", this.fun.getConfig().maxThrowNumber);
        }
        
        int delay = context.getNamed("delay", Integer.class, 3);
        if (delay > this.fun.getConfig().maxThrowDelay || delay < 0)
        {
            illegalParameter(context, "fun", "&cThe delay has to be a number from 0 to %d", this.fun.getConfig().maxThrowDelay);
        }
        
        String object = context.getString(0);
        EntityType type = Match.entity().any(object);
        if (type == null)
        {
            context.sendMessage("fun", "&cThe given object was not found!");
            return;
        }
        if (!type.isSpawnable())
        {
            illegalParameter(context, "fun", "&cThe Item %s is not supported!", object);
        }

        if (!user.hasPermission(BASE_THROW_PERM + type.name().toLowerCase(Locale.ENGLISH).replace("_", "-")))
        {
            denyAccess(context, "fun", "&cYou are not allowed to throw this");
        }
        
        if ((BUGGED_ENTITES.contains(type) || Match.entity().isMonster(type)) && !context.hasFlag("u"))
        {
            context.sendMessage("fun", "&eThis object can only be thrown in unsafe mode. Add -u to enable the unsafe mode.");
            return;
        }

        task = new ThrowTask(user, type, amount, delay, !context.hasFlag("u"));
        if (task.start())
        {
            this.thrownItems.put(user.getName(), task);

            if (amount == -1)
            {
                user.sendMessage("fun", "&aYou will kepp throwing until you run this command again.");
            }
        }
        else
        {
            context.sendMessage("fun", "&cFailed to throw this!");
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
            if (Explosive.class.isAssignableFrom(entityClass))
            {
                return false;
            }
            if (Arrow.class == entityClass)
            {
                return false;
            }
            return true;
        }

        public User getUser()
        {
            return this.user;
        }
        
        public boolean start()
        {
            if (this.amount == -1)
            {
                this.user.sendMessage("fun", "&aStarted throwing!");
            }
            this.taskId = fun.getCore().getTaskManager().scheduleSyncRepeatingTask(fun, this, 0, this.interval);
            return this.taskId != -1;
        }
        
        public void stop()
        {
            if (this.taskId != -1)
            {
                if (this.amount == -1)
                {
                    this.user.sendMessage("fun", "&aYou are no longer throwing.");
                }
                else
                {
                    this.user.sendMessage("fun", "&aAll objects thrown.");
                }
                fun.getCore().getTaskManager().cancelTask(fun, this.taskId);
                this.taskId = -1;
            }
        }
    
        private void throwItem()
        {
            final Location location = this.user.getEyeLocation();
            final Vector direction = location.getDirection();
            location.add(direction).add(direction);

            Entity entity = this.user.getWorld().spawnEntity(location, type);
            entity.setVelocity(direction.multiply(10));
            if (entity instanceof Projectile)
            {
                Projectile projectile = (Projectile)entity;
                projectile.setShooter(this.user.getPlayer());
                projectile.setBounce(false);
                if (projectile instanceof Fireball)
                {
                    ((Fireball)projectile).setDirection(direction);
                }
            }
            else if (entity instanceof ExperienceOrb)
            {
                ((ExperienceOrb)entity).setExperience(0);
            }
            if (this.preventDamage && !this.save)
            {
                throwListener.add(entity);
                if (entity instanceof Explosive)
                {
                    Explosive explosive = (Explosive)entity;
                    explosive.setIsIncendiary(false);
                    explosive.isIncendiary();
                    // explosive.setYield(0); TODO disabling the explosion vs catching it
                }
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
                thrownItems.remove(this.user.getName());
            }
        }
    }

    public class ThrowListener implements Listener
    {
        private final Set<Entity> entities;
        private Entity removal;

        public ThrowListener()
        {
            this.entities = new THashSet<Entity>();
            this.removal = null;
        }

        public void add(Entity entity)
        {
            this.entities.add(entity);
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event)
        {
            ThrowTask task = thrownItems.remove(event.getPlayer().getName());
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
                fun.getCore().getTaskManager().scheduleSyncDelayedTask(fun, new Runnable() {
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
