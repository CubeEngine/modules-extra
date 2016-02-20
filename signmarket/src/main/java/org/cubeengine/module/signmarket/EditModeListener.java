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
package org.cubeengine.module.signmarket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import de.cubeisland.engine.butler.CommandInvocation;
import de.cubeisland.engine.butler.CommandSource;
import org.cubeengine.butler.completer.Completer;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parameter.IncorrectUsageException;
import org.cubeengine.service.command.CommandSource;
import org.cubeengine.service.command.CommandSender;
import org.cubeengine.service.command.conversation.ConversationCommand;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.user.User;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.world.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.spongepowered.api.item.inventory.ItemStack;

import static org.bukkit.event.Event.Result.DENY;
import static org.bukkit.event.block.Action.RIGHT_CLICK_AIR;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;
import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

@Command(name = "MarketSign", desc = "Edit Signmarket signs")
public class EditModeListener extends ConversationCommand
{
    private final MarketSignFactory signFactory;
    private final Signmarket module;
    private I18n i18n;
    private final Map<UUID, Location> currentSignLocation = new HashMap<>();
    private final Map<UUID, MarketSign> previousMarketSign = new HashMap<>();

    public EditModeListener(final Signmarket module, I18n i18n)
    {
        super(module);
        this.module = module;
        this.i18n = i18n;
        this.signFactory = module.getMarketSignFactory();
    }

    @Listener
    public void changeWorld(DisplaceEntityEvent.Teleport event)
    {
        // TODO check for worldchange
        if (this.module.getConfig().disableInWorlds.contains(event.getPlayer().getWorld().getName()))
        {
            Player Player = this.getModule().getCore().getUserManager().getExactUser(event.getPlayer().getUniqueId());
            if (this.hasUser(user))
            {
                i18n.sendTranslated(user, NEUTRAL, "MarketSigns are disabled in the configuration for this world!");
                this.removeUser(user);
                this.currentSignLocation.remove(user.getUniqueId());
            }
        }
    }

    @Override
    public void removeUser(Player user)
    {
        super.removeUser(user);
        i18n.sendTranslated(user, POSITIVE, "Exiting edit mode.");
    }

    @Restricted(Player.class)
    @Command(desc = "Exits the Editmode")
    public void exit(Player context)
    {
        MarketSign marketSign = getSign(context);
        this.removeUser(context);
        this.previousMarketSign.put(context.getUniqueId(), marketSign);
        this.currentSignLocation.remove(context.getUniqueId());
        marketSign.exitEditMode(context);
    }

    @Restricted(Player.class)
    @Command(desc = "Copies the settings from the previous sign")
    public void copy(CommandSource context)
    {
        MarketSign sign = getSign((Player)context.getSource());
        MarketSign prevSign = this.previousMarketSign.get(context.getSource().getUniqueId());
        if (prevSign == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "No market sign at previous position.");
            return;
        }
        if (prevSign.isAdminSign() && !module.perms().SIGN_CREATE_ADMIN_CREATE.isAuthorized(context.getSource()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to copy admin signs!");
            return;
        }
        else if (!prevSign.isAdminSign() && !module.perms().SIGN_CREATE_USER_CREATE.isAuthorized(context.getSource()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to copy player signs!");
            return;
        }
        sign.copyValuesFrom(prevSign);
        showInfoAndUpdate(sign, (User)context.getSource());
    }

    private MarketSign getSign(Player user)
    {
        UUID uuid = user.getUniqueId();
        Location loc = this.currentSignLocation.get(uuid);
        if (loc == null)
        {
            throw new IncorrectUsageException("Please select a sign to edit.");
        }
        MarketSign signAt = this.signFactory.getSignAt(loc);
        if (signAt == null)
        {
            throw new IncorrectUsageException("No market sign at position! This should not happen!");
        }
        this.setEditingSign(user, signAt);
        return signAt;
    }

    private boolean setEditingSign(Player user, MarketSign marketSign)
    {
        if (marketSign == null)
        {
            return true;
        }
        Location previous = this.currentSignLocation.put(user.getUniqueId(), marketSign.getLocation());
        if (!marketSign.getLocation().equals(previous))
        {
            MarketSign previousSign = this.signFactory.getSignAt(previous);
            if (previousSign != null)
            {
                this.previousMarketSign.put(user.getUniqueId(), previousSign);
                previousSign.exitEditMode(user);
            }
            if (!checkAllowedEditing(marketSign, user))
            {
                return true;
            }
            marketSign.enterEditMode();
            i18n.sendTranslated(user, POSITIVE, "Changed active sign!");
            return true;
        }
        if (!checkAllowedEditing(marketSign, user))
        {
            return true;
        }
        marketSign.enterEditMode();
        return false;
    }

    private boolean checkAllowedEditing(MarketSign marketSign, Player user)
    {
        if (marketSign.isAdminSign() && !module.perms().SIGN_CREATE_ADMIN_CREATE.isAuthorized(user))
        {
            i18n.sendTranslated(user, NEGATIVE, "You are not allowed to edit admin signs!");
            this.currentSignLocation.remove(user.getUniqueId());
            return false;
        }
        else if (!marketSign.isAdminSign() && !module.perms().SIGN_CREATE_USER_CREATE.isAuthorized(user))
        {
            i18n.sendTranslated(user, NEGATIVE, "You are not allowed to edit player signs!");
            this.currentSignLocation.remove(user.getUniqueId());
            return false;
        }
        if (!marketSign.isAdminSign() && !marketSign.isOwner(user)
            && !module.perms().SIGN_CREATE_USER_OTHER.isAuthorized(user))
        {
            i18n.sendTranslated(user, NEGATIVE, "You are not allowed to edit Signs of other players!");
            this.currentSignLocation.remove(user.getUniqueId());
            return false;
        }
        return true;
    }

    @Restricted(Player.class)
    @Command(desc = "Changes the sign to a buy-sign")
    public void buy(CommandSource context)
    {
        MarketSign sign = getSign((User)context.getSource());
        if (sign.isAdminSign() && !module.perms().SIGN_CREATE_ADMIN_BUY.isAuthorized(context.getSource()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to create admin buy signs!");
            return;
        }
        if (!sign.isAdminSign() && !module.perms().SIGN_CREATE_USER_BUY.isAuthorized(context.getSource()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to create player buy signs!");
            return;
        }
        sign.setTypeBuy();
        showInfoAndUpdate(sign, (User)context.getSource());
    }

    @Restricted(Player.class)
    @Command(desc = "Changes the sign to a sell-sign")
    public void sell(CommandSource context)
    {
        MarketSign sign = getSign((User)context.getSource());
        if (sign.isAdminSign() && !module.perms().SIGN_CREATE_ADMIN_SELL.isAuthorized(context.getSource()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to create admin sell signs!");
            return;
        }
        if (!sign.isAdminSign() && !module.perms().SIGN_CREATE_USER_SELL.isAuthorized(context.getSource()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to create player sell signs!");
            return;
        }
        sign.setTypeSell();
        showInfoAndUpdate(sign, (User)context.getSource());
    }

    @Restricted(Player.class)
    @Command(desc = "Changes the demand of a sign")
    public void demand(CommandSource context, Integer demand)
    {
        MarketSign sign = getSign((User)context.getSource());
        if (!module.perms().SIGN_CREATE_USER_DEMAND.isAuthorized(context.getSource()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to set a demand!");
            return;
        }
        if (!sign.hasType())
        {
            sign.setTypeSell();
        }
        if (sign.isTypeBuy())
        {
            i18n.sendTranslated(context, NEGATIVE, "Buy signs cannot have a demand!");
            return;
        }
        if (sign.isAdminSign())
        {
            i18n.sendTranslated(context, NEGATIVE, "Admin signs cannot have a demand!");
            return;
        }
        if (demand == -1)
        {
            sign.setNoDemand();
        }
        else
        {
            sign.setDemand(demand);
        }
        showInfoAndUpdate(sign, (User)context.getSource());
    }

    @Restricted(Player.class)
    @Command(desc = "Changes the demand of a sign")
    public void nodemand(CommandSource context)
    {
        MarketSign sign = getSign((User)context.getSource());
        sign.setNoDemand();
        showInfoAndUpdate(sign, (User)context.getSource());
    }

    @Restricted(Player.class)
    @Command(desc = "Changes the sign to an admin-sign")
    public void admin(CommandSource context)
    {
        MarketSign sign = getSign((User)context.getSource());
        if (!module.perms().SIGN_CREATE_ADMIN_CREATE.isAuthorized(context.getSource()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to create admin signs");
            return;
        }
        sign.setAdminSign();
        if (this.module.getConfig().maxAdminStock != -1 && (sign.hasInfiniteSize()
            || sign.getChestSize() > this.module.getConfig().maxAdminStock))
        {
            sign.setSize(this.module.getConfig().maxAdminStock);
        }
        showInfoAndUpdate(sign, (User)context.getSource());
    }

    @Restricted(Player.class)
    @Command(desc = "Changes the sign to an player-sign")
    public void player(CommandSource context)
    {
        MarketSign sign = getSign((User)context.getSource());
        if (!module.perms().SIGN_CREATE_USER_CREATE.isAuthorized(context.getSource()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to create player signs");
            return;
        }
        sign.setOwner((User)context.getSource());
        if (this.module.getConfig().maxUserStock != -1 && (sign.hasInfiniteSize()
            || sign.getChestSize() > this.module.getConfig().maxUserStock))
        {
            sign.setSize(this.module.getConfig().maxUserStock);
        }
        showInfoAndUpdate(sign, (User)context.getSource());
    }

    @Restricted(Player.class)
    @Command(desc = "Changes the signs owner")
    public void owner(CommandSource context, Player owner)
    {
        MarketSign sign = getSign((User)context.getSource());
        if (!module.perms().SIGN_CREATE_USER_OTHER.isAuthorized(context.getSource()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to create player signs for other players");
            return;
        }
        sign.setOwner(owner);
        showInfoAndUpdate(sign, (User)context.getSource());
    }

    @Restricted(Player.class)
    @Command(desc = "Changes whether the sign has stock")
    public void stock(CommandSource context)
    {
        MarketSign sign = getSign((User)context.getSource());
        if (!sign.isAdminSign())
        {
            i18n.sendTranslated(context, NEGATIVE, "Player signs cannot have no stock!");
            return;
        }
        if (sign.hasStock())
        {
            if (!this.module.getConfig().allowAdminNoStock)
            {
                i18n.sendTranslated(context, NEGATIVE, "Admin-signs without stock are not allowed!");
                return;
            }
            if (!module.perms().SIGN_CREATE_ADMIN_NOSTOCK.isAuthorized(context.getSource()))
            {
                i18n.sendTranslated(context, NEGATIVE, "You are not allowed to create admin-signs with no stock");
                return;
            }
            sign.setNoStock();
            showInfoAndUpdate(sign, (User)context.getSource());
            return;
        }
        if (!this.module.getConfig().allowAdminStock)
        {
            i18n.sendTranslated(context, NEGATIVE, "Admin-signs with stock are not allowed!");
            return;
        }
        if (!module.perms().SIGN_CREATE_ADMIN_STOCK.isAuthorized(context.getSource()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to create admin-signs with stock");
            return;
        }
        sign.setStock(0);
        showInfoAndUpdate(sign, (User)context.getSource());
    }

    @Restricted(Player.class)
    @Command(desc = "Sets the signs stock")
    public void setstock(CommandSource context, Integer amount)
    {
        MarketSign sign = getSign((User)context.getSource());
        if (!module.perms().SIGN_SETSTOCK.isAuthorized(context.getSource()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to set the stock!");
            return;
        }
        if (!sign.hasStock())
        {
            i18n.sendTranslated(context, NEGATIVE, "This sign has no stock! Use \"stock\" first to enable it!");
            return;
        }
        sign.setStock(amount);
        sign.syncOnMe = true;
        showInfoAndUpdate(sign, (User)context.getSource());
    }

    @Restricted(Player.class)
    @Command(desc = "Sets the price")
    public void price(CommandSource context, Double price)
    {
        MarketSign sign = getSign((User)context.getSource());
        if (price < 0)
        {
            i18n.sendTranslated(context, NEGATIVE, "A negative price!? Are you serious?");
            return;
        }
        sign.setPrice((long)(price * sign.economy.fractionalDigitsFactor()));
        showInfoAndUpdate(sign, (User)context.getSource());
    }

    @Restricted(Player.class)
    @Command(desc = "Sets the amount")
    public void amount(CommandSource context, Integer amount)
    {
        MarketSign sign = getSign((User)context.getSource());
        if (amount < 0)
        {
            i18n.sendTranslated(context, NEGATIVE, "Negative amounts could be unfair! Just sayin'");
            return;
        }
        sign.setAmount(amount);
        showInfoAndUpdate(sign, (User)context.getSource());
    }

    @Restricted(Player.class)
    @Command(desc = "Sets the item")
    public void item(CommandSource context, ItemStack item)
    {
        MarketSign sign = getSign((User)context.getSource());
        if (sign.isAdminSign())
        {
            sign.setItemStack(item, false);
            showInfoAndUpdate(sign, (User)context.getSource());
            return;
        }
        if (sign.hasStock() && sign.getStock() != 0)
        {
            i18n.sendTranslated(context, NEGATIVE, "You have to take all items out of the market-sign to be able to change the item in it!");
            return;
        }
        sign.setItemStack(item, false);
        showInfoAndUpdate(sign, (User)context.getSource());
    }

    @Restricted(Player.class)
    @Command(desc = "Sets the signs inventory size")
    public void size(CommandSource context, Integer size)
    {
        MarketSign sign = getSign((User)context.getSource());
        if (!module.perms().SIGN_SIZE_CHANGE.isAuthorized(context.getSource()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to change the sign inventory-size.");
            return;
        }
        if (size == 0 || size > 6 || size < -1)
        {
            i18n.sendTranslated(context, NEGATIVE, "Invalid size! Use -1 for infinite OR 1-6 inventory-lines!");
            return;
        }
        if (size == -1 && !module.perms().SIGN_SIZE_INFINITE.isAuthorized(context.getSource()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to set infinite inventories!");
            return;
        }
        if (sign.isAdminSign())
        {
            int maxAdmin = this.module.getConfig().maxAdminStock;
            if (maxAdmin != -1 && (size > maxAdmin || size == -1))
            {
                i18n.sendTranslated(context, NEGATIVE, "The maximum size of admin-signs is set to {amount}!", maxAdmin);
                return;
            }
        }
        else // user-sign
        {
            int maxPlayer = this.module.getConfig().maxUserStock;
            if (maxPlayer != -1 && (size > maxPlayer || size == -1))
            {
                i18n.sendTranslated(context, NEGATIVE, "The maximum size of player signs is set to {amount}!", maxUser);
                return;
            }
        }
        sign.setSize(size);
        sign.syncOnMe = true;
        showInfoAndUpdate(sign, (User)context.getSource());
    }

    private void showInfoAndUpdate(MarketSign sign, Player user)
    {
        sign.showInfo(user);
        sign.updateSignText();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onClick(PlayerInteractEvent event)
    {
        if (event.useItemInHand().equals(DENY))
        {
            return;
        }

        Player Player = this.getModule().getCore().getUserManager().getExactUser(event.getPlayer().getUniqueId());
        if (!this.hasUser(user))
        {
            return;
        }
        if (this.module.getConfig().disableInWorlds.contains(event.getPlayer().getWorld().getName()))
        {
            i18n.sendTranslated(user, NEUTRAL, "MarketSigns are disabled in the configuration for this world!");
            return;
        }
        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK))
        {
            if (!(event.getClickedBlock().getState() instanceof Sign))
            {
                return;
            }
            event.setCancelled(true);
            event.setUseItemInHand(DENY);
            Location newLoc = event.getClickedBlock().getLocation();
            if (!newLoc.equals(this.currentSignLocation.get(user.getUniqueId()))
                && this.currentSignLocation.values().contains(newLoc))
            {
                i18n.sendTranslated(user, NEGATIVE, "Someone else is editing this sign!");
                return;
            }
            MarketSign curSign = this.signFactory.getSignAt(newLoc);
            if (curSign == null)
            {
                if (!user.isSneaking())
                {
                    i18n.sendTranslated(user, NEGATIVE, "That is not a market sign!");
                    i18n.sendTranslated(user, NEUTRAL, "Use shift leftclick to convert the sign.");
                    return;
                }
                curSign = this.signFactory.createSignAt(user, newLoc);
                this.setEditingSign(user, curSign);
                return;
            }
            if (curSign.isInEditMode())
            {
                if (curSign.tryBreak(user))
                {
                    this.previousMarketSign.put(user.getUniqueId(), curSign);
                    this.currentSignLocation.remove(user.getUniqueId());
                }
                return;
            }
            this.setEditingSign(user, curSign);
            return;
        }
        // else:
        if (event.getPlayer().isSneaking())
        {
            return;
        }
        BlockState signFound = null;
        if (event.getAction() == RIGHT_CLICK_AIR)
        {
            if (event.getPlayer().getItemInHand() != null && event.getPlayer().getItemInHand().getTypeId() != 0)
            {
                signFound = MarketSignListener.getTargettedSign(event.getPlayer());
            }
        }
        else if (event.getAction() == RIGHT_CLICK_BLOCK && event.getClickedBlock().getState() instanceof Sign)
        {
            signFound = event.getClickedBlock().getState();
        }
        if (signFound == null)
        {
            return;
        }
        event.setCancelled(true);
        event.setUseItemInHand(DENY);
        Location curLoc = signFound.getLocation();
        MarketSign curSign = this.signFactory.getSignAt(curLoc);
        if (curSign == null)
        {
            i18n.sendTranslated(user, NEUTRAL, "This sign is not a market-sign!");
            return; // not a market-sign
        }
        if (!this.setEditingSign(user, curSign))
        {
            if (user.getItemInHand() == null || user.getItemInHand().getTypeId() == 0)
            {
                return;
            }
            if (!curSign.isAdminSign() && curSign.hasStock() && curSign.getStock() != 0)
            {
                i18n.sendTranslated(user, NEGATIVE, "You have to take all items out of the market sign to be able to change the item in it!");
                return;
            }
            curSign.setItemStack(user.getItemInHand(), true);
            curSign.updateSignText();
            i18n.sendTranslated(user, POSITIVE, "Item in sign updated!");
        }
    }

    @EventHandler
    public void onSignPlace(BlockPlaceEvent event)
    {
        if (!(event.getBlockPlaced().getState() instanceof Sign))
        {
            return;
        }
        Player Player = this.getModule().getCore().getUserManager().getExactUser(event.getPlayer().getUniqueId());
        if (!this.hasUser(user))
        {
            return;
        }
        if (this.module.getConfig().disableInWorlds.contains(event.getPlayer().getWorld().getName()))
        {
            i18n.sendTranslated(user, NEUTRAL, "MarketSigns are disabled in the configuration for this world!");
            return;
        }
        if (!module.perms().SIGN_CREATE_ADMIN_CREATE.isAuthorized(user))
        {
            if (!module.perms().SIGN_CREATE_USER_CREATE.isAuthorized(user))
            {
                i18n.sendTranslated(user, NEGATIVE, "You are not allowed to create market signs!");
                event.setCancelled(true);
                return;
            }
        }
        this.setEditingSign(user, this.signFactory.createSignAt(user, event.getBlockPlaced().getLocation()));
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event)
    {
        Player Player = this.getModule().getCore().getUserManager().getExactUser(event.getPlayer().getUniqueId());
        if (!this.hasUser(user))
        {
            return;
        }
        if (this.module.getConfig().disableInWorlds.contains(event.getPlayer().getWorld().getName()))
        {
            i18n.sendTranslated(user, NEUTRAL, "MarketSigns are disabled in the configuration for this world!");
            return;
        }
        Location loc = event.getBlock().getLocation();
        if (loc.equals(this.currentSignLocation.get(user.getUniqueId())))
        {
            event.setCancelled(true);
        }
    }

    private class SignSizeCompleter implements Completer
    {
        @Override
        public List<String> getSuggestions(CommandInvocation invocation)
        {
            CommandSource commandSource = invocation.getCommandSource();
            if (commandSource instanceof CommandSender)
            {
                if (module.perms().SIGN_SIZE_INFINITE.isAuthorized((CommandSender)commandSource))
                {
                    return Arrays.asList("6", "5", "4", "3", "2", "1", "-1");
                }
                return Arrays.asList("6", "5", "4", "3", "2", "1");
            }
            return new ArrayList<>();
        }
    }
}
