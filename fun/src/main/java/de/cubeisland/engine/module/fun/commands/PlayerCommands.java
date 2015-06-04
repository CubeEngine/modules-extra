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

import java.util.Collections;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Named;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.module.service.command.CommandSender;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.core.util.matcher.Match;
import de.cubeisland.engine.module.fun.Fun;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import static org.bukkit.Material.AIR;

public class PlayerCommands
{
    private final Fun module;
    private final ExplosionListener explosionListener;

    public PlayerCommands(Fun module)
    {
        this.module = module;
        this.explosionListener = new ExplosionListener();
        this.module.getCore().getEventManager().registerListener(module, explosionListener);
    }
    
    @Command(desc = "Gives a player a hat")
    public void hat(CommandSender context, @Optional String item, @Named({"player", "p"}) User player, @Flag boolean quiet)
    {
        ItemStack head;
        PlayerInventory senderInventory = null;
        PlayerInventory userInventory;
        
        if(player != null)
        {
            if(!module.perms().COMMAND_HAT_OTHER.isAuthorized( context ) )
            {
                context.sendTranslated(NEGATIVE, "You can't set the hat of an other player.");
                return;
            }
        }
        else if(context instanceof User)
        {
            player = (User)context;
        }
        else
        {
            context.sendTranslated(NEGATIVE, "You have to specify a player!");
            return;
        }
        
        if(item != null)
        {
            if(!module.perms().COMMAND_HAT_ITEM.isAuthorized(context))
            {
                context.sendTranslated(NEGATIVE, "You can only use your item in hand!");
                return;
            }
            head = Match.material().itemStack(item);
            if(head == null)
            {
                context.sendTranslated(NEGATIVE, "Item not found!");
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
            context.sendTranslated(NEGATIVE, "Trying to be Notch? No hat for you!");
            context.sendTranslated(NEUTRAL, "Please specify an item!");
            return;
        }
        if (head.getType() == AIR)
        {
            context.sendTranslated(NEGATIVE, "You do not have any item in your hand!");
            return;
        }
        switch (head.getType())
        {
            case LEATHER_BOOTS:
            case LEATHER_CHESTPLATE:
            case LEATHER_LEGGINGS:
            case IRON_BOOTS:
            case IRON_CHESTPLATE:
            case IRON_LEGGINGS:
            case DIAMOND_BOOTS:
            case DIAMOND_CHESTPLATE:
            case DIAMOND_LEGGINGS:
            case CHAINMAIL_BOOTS:
            case CHAINMAIL_CHESTPLATE:
            case CHAINMAIL_LEGGINGS:
                if (!module.perms().COMMAND_HAT_MORE_ARMOR.isAuthorized(context))
                {
                    context.sendTranslated(NEGATIVE, "You are not allowed to use other armor as headpiece");
                }
                return;
        }
        userInventory = player.getInventory();
        
        int amount = head.getAmount();
        head.setAmount(1);
        
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
            player.sendTranslated(POSITIVE, "Your hat was changed");
        }        
    }

    @Command(desc = "Creates an explosion")
    public void explosion(CommandSender context, @Optional Integer damage, @Named({"player", "p"}) User player,
                          @Flag boolean unsafe, @Flag boolean fire, @Flag boolean blockDamage, @Flag boolean playerDamage)
    {
        damage = damage == null ? 1 : damage;
        if (damage > this.module.getConfig().command.explosion.power)
        {
            context.sendTranslated(NEGATIVE, "The power of the explosion shouldn't be greater than {integer}", this.module.getConfig().command.explosion.power);
            return;
        }
        Location loc;
        if (player != null)
        {
            if (!context.equals(player))
            {
                if (!module.perms().COMMAND_EXPLOSION_OTHER.isAuthorized(context))
                {
                    context.sendTranslated(NEGATIVE, "You are not allowed to specify a player.");
                    return;
                }
            }
            loc = player.getLocation();
        }
        else
        {
            if (!(context instanceof User))
            {
                context.sendTranslated(NEGATIVE, "This command can only be used by a player!");
                return;
            }
            loc = ((User)context).getTargetBlock(Collections.<Material>emptySet(), this.module.getConfig().command.explosion.distance).getLocation();
        }

        if (!module.perms().COMMAND_EXPLOSION_BLOCK_DAMAGE.isAuthorized(context) && (blockDamage || unsafe))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to break blocks");
            return;
        }
        if (!module.perms().COMMAND_EXPLOSION_FIRE.isAuthorized(context) && (fire || unsafe))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to set fireticks");
            return;
        }
        if (!module.perms().COMMAND_EXPLOSION_PLAYER_DAMAGE.isAuthorized(context) && (playerDamage || unsafe))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to damage another player");
            return;
        }

        if (!unsafe && !playerDamage)
        {
            explosionListener.add(loc);
        }

        loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), damage, fire || unsafe, blockDamage || unsafe);
    }

    @Command(alias = "strike", desc = "Throws a lightning bolt at a player or where you're looking")
    public void lightning(CommandSender context, @Optional Integer damage, @Named({"player","p"}) User player,
                          @Named({"fireticks", "f"}) Integer seconds, @Flag boolean unsafe)
    {
        damage = damage == null ? -1 : damage;

        if (damage != -1 && !module.perms().COMMAND_LIGHTNING_PLAYER_DAMAGE.isAuthorized(context))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to specify the damage!");
            return;
        }
        if ((damage != -1 && damage < 0) || damage > 20)
        {
            context.sendTranslated(NEGATIVE, "The damage value has to be a number from 1 to 20");
            return;
        }
        if (unsafe && !module.perms().COMMAND_LIGHTNING_UNSAFE.isAuthorized(context))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to use the unsafe flag");
            return;
        }

        Location location;
        if (player != null)
        {
            location = player.getLocation();
            player.setFireTicks(20 * seconds);
            if (damage != -1)
            {
                player.damage(damage);
            }
        }
        else
        {
            if (!(context instanceof User))
            {
                context.sendTranslated(NEGATIVE, "This command can only be used by a player!");
                return;
            }
            location = ((User)context).getTargetBlock(Collections.emptySet(), this.module.getConfig().command.lightning.distance).getLocation();
        }

        if (unsafe)
        {
            location.getWorld().strikeLightning(location);
        }
        else
        {
            location.getWorld().strikeLightningEffect(location);
        }

    }

    @Command(desc = "Slaps a player")
    public void slap(CommandSender context, User player, @Optional Integer damage)
    {
        damage = damage == null ? 3 : damage;

        if (damage < 1 || damage > 20)
        {
            context.sendTranslated(NEGATIVE, "Only damage values from 1 to 20 are allowed!");
            return;
        }

        final Vector userDirection = player.getLocation().getDirection();
        player.damage(damage);
        player.setVelocity(new Vector(userDirection.getX() * damage / 2, 0.05 * damage, userDirection.getZ() * damage / 2));
    }

    @Command(desc = "Burns a player")
    public void burn(CommandSender context, User player, @Optional Integer seconds, @Flag boolean unset)
    {
        seconds = seconds == null ? 5 : seconds;
        if (unset)
        {
            seconds = 0;
        }
        else if (seconds < 1 || seconds > this.module.getConfig().command.burn.maxTime)
        {
            context.sendTranslated(NEGATIVE, "Only 1 to {integer} seconds are allowed!", this.module.getConfig().command.burn.maxTime);
            return;
        }

        player.setFireTicks(seconds * 20);
    }

    private class ExplosionListener implements Listener
    {
        private Location location;

        public void add(Location location)
        {

            this.location = location;

            module.getCore().getTaskManager().runTaskDelayed(module, new Runnable()
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
