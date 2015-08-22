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
import de.cubeisland.engine.module.core.sponge.SpongeCore;
import de.cubeisland.engine.module.core.module.Module;
import org.cubeengine.module.core.util.ChatFormat;
import org.spongepowered.api.world.Location;
import org.bukkit.Server;
import org.bukkit.entity.Boat;
import org.spongepowered.api.entity.player.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import static org.bukkit.Material.BOAT;
import static org.bukkit.Material.LOG;
import static org.bukkit.block.BlockFace.UP;
import static org.bukkit.enchantments.Enchantment.DURABILITY;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

/**
 * A module providing a recipe for an (almost) unbreakable boat
 */
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
        Server server = ((SpongeCore)this.getCore()).getServer();
        server.addRecipe(recipe);
    }

    @EventHandler
    public void onVehicleBreak(VehicleDestroyEvent event)
    {
        Vehicle vehicle = event.getVehicle();
        if (!(vehicle instanceof Boat))
        {
            return;
        }
        if (event.getAttacker() instanceof Player)
        {
            if (this.unbreakable.remove(vehicle.getUniqueId()))
            {
                Location location = vehicle.getLocation();
                location.getWorld().dropItemNaturally(location, boat.clone());
                vehicle.remove();
                event.setCancelled(true);
            }
            return;
        }
        if (this.unbreakable.contains(vehicle.getUniqueId()))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onVehiclePlace(VehicleCreateEvent event)
    {
        // TODO waiting for https://hub.spigotmc.org/jira/browse/SPIGOT-694 to remove this ugly hack
        final Vehicle vehicle = event.getVehicle();
        getCore().getTaskManager().runTask(this, () -> onVehiclePlace0(vehicle));
    }

    private void onVehiclePlace0(Vehicle vehicle)
    {
        Location location = vehicle.getLocation();
        location.getBlock().getLocation(location);
        Player player = this.prePlanned.remove(location);
        if (player != null)
        {
            this.unbreakable.add(vehicle.getUniqueId());
        }
    }

    @EventHandler
    public void onBoatPreplace(PlayerInteractEvent event)
    {
        ItemStack inHand = event.getPlayer().getItemInHand();
        if (event.getAction() == RIGHT_CLICK_BLOCK
         && inHand.getType() == BOAT
         && inHand.getEnchantmentLevel(DURABILITY) == 5)
        {
            this.prePlanned.put(event.getClickedBlock().getRelative(UP).getLocation(), event.getPlayer());
        }
    }
}
