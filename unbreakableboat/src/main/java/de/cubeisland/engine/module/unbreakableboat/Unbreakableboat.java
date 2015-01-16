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
package de.cubeisland.engine.module.unbreakableboat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import de.cubeisland.engine.core.bukkit.BukkitCore;
import de.cubeisland.engine.core.module.Module;
import de.cubeisland.engine.core.util.ChatFormat;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import static org.bukkit.Material.BOAT;
import static org.bukkit.Material.LOG;
import static org.bukkit.block.BlockFace.UP;
import static org.bukkit.enchantments.Enchantment.DURABILITY;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

public class Unbreakableboat extends Module implements Listener
{
    private Map<Location, Player> prePlanned = new HashMap<>();
    private Set<UUID> unbreakable = new HashSet<>();
    private ItemStack boat = new ItemStack(BOAT, 1);

    @Override
    public void onEnable()
    {
        this.getCore().getEventManager().registerListener(this, this);
        boat.addUnsafeEnchantment(DURABILITY, 5);
        ItemMeta itemMeta = boat.getItemMeta();
        itemMeta.setDisplayName(ChatFormat.parseFormats("&6Sturdy Boat"));
        itemMeta.setLore(Arrays.asList(ChatFormat.parseFormats("&eCan take a lot!")));
        boat.setItemMeta(itemMeta);
        ShapedRecipe recipe = new ShapedRecipe(boat).shape("l l", "lll").setIngredient('l', LOG);
        Server server = ((BukkitCore)this.getCore()).getServer();
        server.addRecipe(recipe);
    }

    @EventHandler
    public void onVehicleBreak(VehicleDestroyEvent event)
    {
        Vehicle vehicle = event.getVehicle();
        if (vehicle instanceof Boat)
        {
            if (event.getAttacker() instanceof Player)
            {

                if (this.unbreakable.remove(vehicle.getUniqueId()))
                {
                    Location location = vehicle.getLocation();
                    location.getWorld().dropItemNaturally(location, boat.clone());
                    vehicle.remove();
                    event.setCancelled(true);
                    return;
                }
                return;
            }
            if (this.unbreakable.contains(vehicle.getUniqueId()))
            {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onVehiclePlace(VehicleCreateEvent event)
    {
        Location location = event.getVehicle().getLocation();
        location.getBlock().getLocation(location);
        Player player = this.prePlanned.remove(location);
        if (player != null)
        {
            this.unbreakable.add(event.getVehicle().getUniqueId());
        }
    }

    @EventHandler
    public void onBoatPreplace(PlayerInteractEvent event)
    {
        if (event.getAction() == RIGHT_CLICK_BLOCK && event.getPlayer().getItemInHand().getType() == BOAT)
        {
            if (event.getPlayer().getItemInHand().getEnchantmentLevel(DURABILITY) == 5)
            {
                this.prePlanned.put(event.getClickedBlock().getRelative(UP).getLocation(), event.getPlayer());
            }
        }
    }
}
