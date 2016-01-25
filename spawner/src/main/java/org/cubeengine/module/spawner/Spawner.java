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
package org.cubeengine.module.spawner;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import org.cubeengine.service.event.EventManager;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.permission.PermissionManager;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.item.Enchantments;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.permission.PermissionDescription;

import static org.cubeengine.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.block.BlockTypes.MOB_SPAWNER;
import static org.spongepowered.api.entity.EntityTypes.*;
import static org.spongepowered.api.event.Order.POST;

/**
 * A module to gather monster spawners with silk touch and reactivate them using spawneggs
 */
@ModuleInfo(name = "Spawner", description = "Lets you move spawners")
public class Spawner extends Module
{
    private ItemStack spawnerItem;
    private PermissionDescription eggPerms;
    private Map<EntityType, PermissionDescription> perms = new HashMap<>();

    @Inject private PermissionManager pm;
    @Inject private EventManager em;
    @Inject private I18n i18n;

    @Override
    public void onEnable()
    {
        this.eggPerms = pm.register(this, "egg", "", null);
        this.initPerms();
        this.spawnerItem = ItemStack.of(ItemTypes.MOB_SPAWNER, 1);
        spawnerItem.addUnsafeEnchantment(LURE, 1);
        em.registerListener(this, this);
    }

    private void initPerms()
    {
        this.initPerm(CREEPER, SKELETON, SPIDER, ZOMBIE, SLIME, GHAST,
                      PIG_ZOMBIE, ENDERMAN, CAVE_SPIDER, SILVERFISH,
                      BLAZE, MAGMA_CUBE, WITCH, BAT, PIG, SHEEP, COW,
                      CHICKEN, SQUID, WOLF, MUSHROOM_COW, OCELOT,
                      HORSE, VILLAGER);
    }

    private void initPerm(EntityType... types)
    {
        for (EntityType type : types)
        {
            PermissionDescription child = pm.register(this, type.getName(), "", eggPerms);
            this.perms.put(type, child);
        }
    }

    @Listener(order = POST, ignoreCancelled = true)
    public void onBlockBreak(ChangeBlockEvent.Break event, @Filter Player player)
    {
        Optional<ItemStack> inHand = player.getItemInHand();
        if (inHand.isPresent() &&
            inHand.containsEnchantment(Enchantments.SILK_TOUCH) &&
            event.getBlock().getType() == MOB_SPAWNER)
        {
            User user = this.getCore().getUserManager().getExactUser(player.getUniqueId());

            ItemStack clone = spawnerItem.clone();
            ItemMeta itemMeta = clone.getItemMeta();
            itemMeta.setDisplayName(i18n.getTranslation(player, NONE, "Inactive Monster Spawner"));
            clone.setItemMeta(itemMeta);
            event.getPlayer().getWorld().dropItemNaturally(event.getBlock().getLocation(), clone);

            i18n.sendTranslated(user, POSITIVE, "Dropped inactive Monster Spawner!");

            event.setExpToDrop(0);
        }
    }

    @Listener(ignoreCancelled = true, order = POST)
    public void onBlockPlace(ChangeBlockEvent.Place event)
    {
        if (event.getBlockPlaced().getType() == MOB_SPAWNER &&
            event.getPlayer().getItemInHand().getEnchantmentLevel(LURE) == 1)
        {
            CreatureSpawner spawner = (CreatureSpawner)event.getBlock().getState();
            spawner.setSpawnedType(SNOWBALL);
            User user = this.getCore().getUserManager().getExactUser(event.getPlayer().getUniqueId());
            i18n.sendTranslated(user, POSITIVE, "Inactive Monster Spawner placed!");
        }
    }

    @Listener(ignoreCancelled = true, order = POST)
    public void onInteract(InteractBlockEvent.Secondary event, @Filter Player player)
    {
        if (event.getClickedBlock().getType() == MOB_SPAWNER
         && event.getPlayer().getItemInHand().getType() == BlockTypes.MONSTER_EGG)
        {
            event.setCancelled(true);
            User user = this.getCore().getUserManager().getExactUser(event.getPlayer().getUniqueId());
            CreatureSpawner state = (CreatureSpawner)event.getClickedBlock().getState();
            if (state.getSpawnedType() == SNOWBALL)
            {
                this.handleInteract(user, state, (SpawnEgg)event.getPlayer().getItemInHand().getData());
                return;
            }
            i18n.sendTranslated(user, NEGATIVE, "You can only change inactive Monster Spawner!");
        }
    }

    private void handleInteract(Player user, CreatureSpawner spawner, SpawnEgg egg)
    {
        PermissionDescription perm = this.perms.get(egg.getSpawnedType());
        if (perm == null && !this.eggPerms.isAuthorized(user))
        {
            i18n.sendTranslated(user, NEGATIVE, "Invalid SpawnEgg!");
            return;
        }
        if (perm != null && !perm.isAuthorized(user))
        {
            i18n.sendTranslated(user, NEGATIVE, "You are not allowed to change Monster Spawner to this EntityType!");
            return;
        }
        spawner.setSpawnedType(egg.getSpawnedType());
        spawner.update();
        if (user.getGameMode() != GameModes.CREATIVE)
        {
            int amount = user.getItemInHand().getAmount() - 1;
            user.getItemInHand().setAmount(amount);
            if (amount == 0)
            {
                user.setItemInHand(null);
            }
        }
        i18n.sendTranslated(user, POSITIVE, "Monster Spawner activated!");
    }
}
