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
package org.cubeengine.module.backpack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.cubeengine.reflect.Reflector;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.util.Tristate;

import static org.cubeengine.libcube.service.filesystem.FileExtensionFilter.DAT;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

public class BackpackManager
{
    private final Backpack module;
    private Reflector reflector;
    private I18n i18n;
    private final Path backpackPath;

    private Map<UUID, Map<String, BackpackInventory>> allPacks = new HashMap<>();

    public BackpackManager(Backpack module, Reflector reflector, I18n i18n)
    {
        this.module = module;
        this.reflector = reflector;
        this.i18n = i18n;
        this.backpackPath = module.getModulePath().resolve("backpacks");
    }

    protected void loadBackpacks(UUID player)
    {
        // TODO replace using custom data
        try
        {
            Path folder = this.backpackPath.resolve(player.toString());
            Files.createDirectories(folder);

            // When already loaded discard previous
            Map<String, BackpackInventory> backPacks = allPacks.get(player);
            if (backPacks != null)
            {
                backPacks.values().forEach(BackpackInventory::closeInventory);
            }
            backPacks = new HashMap<>();
            allPacks.put(player, backPacks);

            for (Path path : Files.newDirectoryStream(folder, DAT))
            {
                String name = StringUtils.stripFileExtension(path.getFileName().toString());
                BackpackData load = reflector.load(BackpackData.class, path.toFile());
                backPacks.put(name, new BackpackInventory(module, load, name));
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
    }

    public void openBackpack(Player sender, User forUser, boolean outOfContext, String name)
    {
        if (!allPacks.containsKey(forUser.getUniqueId()) || allPacks.get(forUser.getUniqueId()).isEmpty())
        {
            loadBackpacks(forUser.getUniqueId());
        }
        BackpackInventory pack = getBackpack(forUser, name);
        if (pack == null)
        {
            packNotExistMessage(sender, forUser, name);
            return;
        }
        if (!outOfContext && !sender.hasPermission(getPackPerm(sender, pack)))
        {
            i18n.send(sender, NEGATIVE, "This backpack is not available in your current context!");
            return;
        }

        pack.openInventory(sender);
    }

    private void packNotExistMessage(CommandSource sender, User player, String name)
    {
        if (sender != player)
        {
            i18n.send(sender, NEGATIVE, "{user} does not have a backpack named {input#backpack}!", player, name);
            return;
        }
        i18n.send(sender, NEGATIVE, "You don't have a backpack named {input#backpack}!", name);
    }

    private BackpackInventory getBackpack(User player, String name)
    {
        Map<String, BackpackInventory> packs = allPacks.get(player.getUniqueId());
        BackpackInventory pack = null;
        if (packs != null)
        {
            pack = packs.get(name);
        }
        return pack;
    }

    public void createBackpack(CommandSource sender, User player, String name, boolean blockInput)
    {
        BackpackInventory backpack = getBackpack(player, name);
        if (backpack != null)
        {
            packExistsMessage(sender, player, name);
            return;
        }

        try
        {
            Path folder = this.backpackPath.resolve(player.getUniqueId().toString());
            Files.createDirectories(folder);

            BackpackData data = reflector.create(BackpackData.class);
            data.allowItemsIn = !blockInput;
            data.setFile(folder.resolve(name + DAT.getExtention()).toFile());
            data.save();

            Map<String, BackpackInventory> packs = allPacks.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
            packs.put(name, new BackpackInventory(module, data, name));

            i18n.send(sender, POSITIVE, "Created backpack {input#backpack} for {user}", name, player);
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private void packExistsMessage(CommandSource sender, User player, String name)
    {
        if (sender == player)
        {
            i18n.send(sender, NEGATIVE, "A backpack named {input#backpack} already exists", name);
            return;
        }
        i18n.send(sender, NEGATIVE, "{user} already had a backpack named {input#backpack}", player, name);
        return;
    }

    public void giveItem(CommandSource sender, User player, String name, ItemStack itemToGive)
    {
        BackpackInventory pack = getBackpack(player, name);
        if (pack == null)
        {
            packNotExistMessage(sender, player, name);
            if (sender != player)
            {
                i18n.send(sender, NEGATIVE, "{user} does not have a backpack named {input#backpack} in this world!", player, name);
                return;
            }
            i18n.send(sender, NEGATIVE, "You don't have a backpack named {input#backpack} in this world!", name);
            return;
        }
        pack.addItem(itemToGive);
        i18n.send(sender, POSITIVE, "Item added to backpack!");
        if (sender != player && player.isOnline())
        {
            i18n.send(player.getPlayer().get(), POSITIVE, "You received items in your backpack {input#backpack}", name);
        }
    }

    public void modifyBackpack(CommandSource sender, User player, String name, Boolean blockInput)
    {
        BackpackInventory pack = getBackpack(player, name);
        if (pack == null)
        {
            packNotExistMessage(sender, player, name);
            return;
        }
        
        if (blockInput != null)
        {
            pack.data.allowItemsIn = !blockInput;
            if (blockInput)
            {
                i18n.send(sender, POSITIVE, "Items are not allowed to go in!");
            }
            else
            {
                i18n.send(sender, POSITIVE, "Items are allowed to go in!");
            }
        }
        pack.data.save();
    }

    public void setBackpackContext(CommandSource ctx, User player, String name, Context context, Tristate state)
    {
        BackpackInventory pack = getBackpack(player, name);
        if (pack == null)
        {
            packNotExistMessage(ctx, player, name);
            return;
        }

        String packPerm = getPackPerm(player, pack);
        player.getSubjectData().setPermission(Collections.singleton(context), packPerm, state);

        if (state == Tristate.UNDEFINED)
        {
            i18n.send(ctx, POSITIVE, "Removed permission {name} in context {context}", packPerm, context);
            return;
        }
        i18n.send(ctx, POSITIVE, "Set permission {name} in context {context} to {}", packPerm, context, state.asBoolean());
    }

    private String getPackPerm(User player, BackpackInventory pack)
    {
        String packName = pack.getName().equals(player.getName()) ? ".playerbackpack" : ".namedbackpack." + pack.getName();
        return module.perms().USE.getId() + packName.toLowerCase();
    }

    @Listener
    public void onInventoryClose(InteractInventoryEvent.Close event, @First Player player)
    {
        Container inventory = event.getTargetInventory();
        if (inventory instanceof CarriedInventory)
        {
            if (((CarriedInventory)inventory).getCarrier().isPresent() && ((CarriedInventory)inventory).getCarrier().get() instanceof BackpackHolder)
            {
                BackpackHolder holder = (BackpackHolder)((CarriedInventory)inventory).getCarrier().get();
                holder.getBackpack().closeInventory(inventory, player);
            }
        }
    }

    public Set<String> getBackpackNames(UUID player)
    {
        if (!allPacks.containsKey(player) || allPacks.get(player).isEmpty())
        {
            loadBackpacks(player);
        }
        return allPacks.get(player).keySet();
    }
}
