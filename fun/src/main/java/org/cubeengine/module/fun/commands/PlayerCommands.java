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

import java.util.Collections;
import com.flowpowered.math.vector.Vector3d;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.fun.Fun;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.task.TaskManager;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.property.item.EquipmentProperty;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.equipment.EquipmentType;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.world.World;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

public class PlayerCommands
{
    private final Fun module;
    private final ExplosionListener explosionListener;
    private final I18n i18n;
    private final TaskManager tm;

    public PlayerCommands(Fun module, EventManager em, I18n i18n, TaskManager tm)
    {
        this.module = module;
        this.i18n = i18n;
        this.tm = tm;
        this.explosionListener = new ExplosionListener();
        em.registerListener(Fun.class, explosionListener);
    }
    
    @Command(desc = "Gives a player a hat")
    public void hat(CommandSource context, @Optional String item, @Named({"player", "p"}) Player player, @Flag boolean quiet)
    {
        ItemStack head;
        PlayerInventory senderInventory = null;
        PlayerInventory userInventory;
        
        if(player != null)
        {
            if(!module.perms().COMMAND_HAT_OTHER.isAuthorized( context ) )
            {
                i18n.sendTranslated(context, NEGATIVE, "You can't set the hat of an other player.");
                return;
            }
        }
        else if(context instanceof User)
        {
            player = (User)context;
        }
        else
        {
            i18n.sendTranslated(context, NEGATIVE, "You have to specify a player!");
            return;
        }
        
        if(item != null)
        {
            if(!module.perms().COMMAND_HAT_ITEM.isAuthorized(context))
            {
                i18n.sendTranslated(context, NEGATIVE, "You can only use your item in hand!");
                return;
            }
            head = Match.material().itemStack(item);
            if(head == null)
            {
                i18n.sendTranslated(context, NEGATIVE, "Item not found!");
                return;
            }
        }
        else if (context instanceof User)
        {
            senderInventory = ((User)context).getInventory();
            head = senderInventory.getItemInHand().clone();
        }
        else
        {
            i18n.sendTranslated(context, NEGATIVE, "Trying to be Notch? No hat for you!");
            i18n.sendTranslated(context, NEUTRAL, "Please specify an item!");
            return;
        }
        if (head.getItem() == ItemTypes.NONE)
        {
            i18n.sendTranslated(context, NEGATIVE, "You do not have any item in your hand!");
            return;
        }
        EquipmentType type = head.getItem().getDefaultProperty(EquipmentProperty.class)
                         .map(EquipmentProperty::getValue).orElse(null);
        if (type == null || type != EquipmentTypes.HEADWEAR)
        {
            if (!context.hasPermission(module.perms().COMMAND_HAT_MORE_ARMOR.getId()))
            {
                i18n.sendTranslated(context, NEGATIVE, "You are not allowed to use other armor as headpiece");
            }
        }

        userInventory = player.getInventory();
        
        head.setQuantity(1);
        
        if(item == null && senderInventory != null)
        {
            ItemStack clone = head.clone();
            clone.setAmount(amount - 1);
            senderInventory.setItemInHand( clone );
        }
        if(userInventory.getHelmet() != null)
        {
            userInventory.addItem(userInventory.getHelmet());
        }
        
        userInventory.setHelmet(head);
        
        if(!(quiet && module.perms().COMMAND_HAT_QUIET.isAuthorized(context)) && module.perms().COMMAND_HAT_NOTIFY.isAuthorized(player))
        {
            i18n.sendTranslated(player, POSITIVE, "Your hat was changed");
        }        
    }

    @Command(desc = "Creates an explosion")
    public void explosion(CommandSource context, @Optional Integer damage, @Named({"player", "p"}) User player,
                          @Flag boolean unsafe, @Flag boolean fire, @Flag boolean blockDamage, @Flag boolean playerDamage)
    {
        damage = damage == null ? 1 : damage;
        if (damage > this.module.getConfig().command.explosion.power)
        {
            i18n.sendTranslated(context, NEGATIVE, "The power of the explosion shouldn't be greater than {integer}", this.module.getConfig().command.explosion.power);
            return;
        }
        Location loc;
        if (player != null)
        {
            if (!context.equals(player))
            {
                if (!module.perms().COMMAND_EXPLOSION_OTHER.isAuthorized(context))
                {
                    i18n.sendTranslated(context, NEGATIVE, "You are not allowed to specify a player.");
                    return;
                }
            }
            loc = player.getLocation();
        }
        else
        {
            if (!(context instanceof Player))
            {
                i18n.sendTranslated(context, NEGATIVE, "This command can only be used by a player!");
                return;
            }
            loc = ((User)context).getTargetBlock(Collections.<Material>emptySet(), this.module.getConfig().command.explosion.distance).getLocation();
        }

        if (!module.perms().COMMAND_EXPLOSION_BLOCK_DAMAGE.isAuthorized(context) && (blockDamage || unsafe))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to break blocks");
            return;
        }
        if (!module.perms().COMMAND_EXPLOSION_FIRE.isAuthorized(context) && (fire || unsafe))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to set fireticks");
            return;
        }
        if (!module.perms().COMMAND_EXPLOSION_PLAYER_DAMAGE.isAuthorized(context) && (playerDamage || unsafe))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to damage another player");
            return;
        }

        if (!unsafe && !playerDamage)
        {
            explosionListener.add(loc);
        }

        loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), damage, fire || unsafe, blockDamage || unsafe);
    }

    @Command(alias = "strike", desc = "Throws a lightning bolt at a player or where you're looking")
    public void lightning(CommandSource context, @Optional Integer damage, @Named({"player","p"}) Player player,
                          @Named({"fireticks", "f"}) Integer seconds, @Flag boolean unsafe)
    {
        damage = damage == null ? -1 : damage;

        if (damage != -1 && !module.perms().COMMAND_LIGHTNING_PLAYER_DAMAGE.isAuthorized(context))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to specify the damage!");
            return;
        }
        if ((damage != -1 && damage < 0) || damage > 20)
        {
            i18n.sendTranslated(context, NEGATIVE, "The damage value has to be a number from 1 to 20");
            return;
        }
        if (unsafe && !module.perms().COMMAND_LIGHTNING_UNSAFE.isAuthorized(context))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to use the unsafe flag");
            return;
        }

        Location<World> location;
        if (player != null)
        {
            location = player.getLocation();
            player.offer(Keys.FIRE_TICKS, 20 * seconds);
            if (damage != -1)
            {
                player.damage(damage);
            }
        }
        else
        {
            if (!(context instanceof Player))
            {
                i18n.sendTranslated(context, NEGATIVE, "This command can only be used by a player!");
                return;
            }
            location = ((Player)context).getTargetBlock(Collections.emptySet(), this.module.getConfig().command.lightning.distance).getLocation();
        }

        if (unsafe)
        {
            location.getExtent().strikeLightning(location);
        }
        else
        {
            location.getExtent().strikeLightningEffect(location);
        }

    }

    @Command(desc = "Slaps a player")
    public void slap(CommandSource context, Player player, @Optional Integer damage)
    {
        damage = damage == null ? 3 : damage;

        if (damage < 1 || damage > 20)
        {
            i18n.sendTranslated(context, NEGATIVE, "Only damage values from 1 to 20 are allowed!");
            return;
        }

        final Vector3d userDirection = player.getRotation();
        player.damage(damage, DamageSource.builder().absolute().build(), Cause.of(NamedCause.source(context)));
        player.setVelocity(new Vector(userDirection.getX() * damage / 2, 0.05 * damage, userDirection.getZ() * damage / 2));
    }

    @Command(desc = "Burns a player")
    public void burn(CommandSource context, Player player, @Optional Integer seconds, @Flag boolean unset)
    {
        seconds = seconds == null ? 5 : seconds;
        if (unset)
        {
            seconds = 0;
        }
        else if (seconds < 1 || seconds > this.module.getConfig().command.burn.maxTime)
        {
            i18n.sendTranslated(context, NEGATIVE, "Only 1 to {integer} seconds are allowed!", this.module.getConfig().command.burn.maxTime);
            return;
        }

        player.offer(Keys.FIRE_TICKS, seconds * 20);
    }

    private class ExplosionListener
    {
        private Location location;

        public void add(Location location)
        {

            this.location = location;

            tm.runTaskDelayed(module, new Runnable()
            {
                @Override
                public void run()
                {
                    remove();
                }
            }, 1);
        }

        private void remove()
        {
            this.location = null;
        }

        @EventHandler
        public void onEntityDamageByBlock(EntityDamageByBlockEvent event)
        {
            if (this.location != null && event.getDamager() == null && event.getEntity() instanceof Player)
            {
                event.setCancelled(true);
            }
        }
    }
}
