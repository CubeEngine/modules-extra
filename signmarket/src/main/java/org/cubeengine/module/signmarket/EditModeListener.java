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

import java.util.Optional;
import java.util.UUID;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.module.signmarket.data.IMarketSignData;
import org.cubeengine.module.signmarket.data.ImmutableMarketSignData;
import org.cubeengine.module.signmarket.data.MarketSignData;
import org.cubeengine.module.signmarket.data.SignType;
import org.cubeengine.libcube.service.command.annotation.ParameterPermission;
import org.cubeengine.libcube.service.command.conversation.ConversationCommand;
import org.cubeengine.libcube.service.command.exception.PermissionDeniedException;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent.Primary;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.world.World;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Command(name = "MarketSign", desc = "Edit Signmarket signs")
public class EditModeListener extends ConversationCommand
{
    private final Signmarket module;
    private I18n i18n;
    private MarketSignManager manager;

    public EditModeListener(final Signmarket module, I18n i18n, MarketSignManager manager)
    {
        super(module);
        this.module = module;
        this.i18n = i18n;
        this.manager = manager;
    }

    @Listener
    public void changeWorld(DisplaceEntityEvent.Teleport event)
    {
        if (!(event.getTargetEntity() instanceof Player))
        {
            return;
        }
        if (!((Player)event.getTargetEntity()).hasPermission(module.perms().EDIT_USE.getId()))
        {
            manager.exitEditMode(((Player)event.getTargetEntity()));
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
        manager.exitEditMode(context);
        removeUser(context);
    }

    @Restricted(Player.class)
    @Command(desc = "Copies the settings from the previous sign")
    public void copy(Player context)
    {
        ImmutableMarketSignData data = manager.getPreviousData(context);
        if (data != null)
        {
            i18n.sendTranslated(context, NEGATIVE, "No previous market sign");
            return;
        }

        if (data.getOwner().equals(IMarketSignData.ADMIN_SIGN))
        {
            if (!context.hasPermission(module.perms().EDIT_ADMIN.getId()))
            {
                throw new PermissionDeniedException(module.perms().EDIT_ADMIN);
            }
        }
        else
        {
            if (!context.hasPermission(module.perms().EDIT_USE.getId()))
            {
                throw new PermissionDeniedException(module.perms().EDIT_USE);
            }
        }

        MarketSignData copy = data.asMutable();
        if (!copy.isAdminOwner())
        {
            copy.setStock(0);
        }
        Location<World> loc = manager.updateData(copy, context);
        manager.executeShowInfo(copy, context, loc);
    }

    @Command(desc = "Changes the sign to a buy-sign")
    public void buy(Player context)
    {
        // TODO perms for sell/Buy maybe?
        MarketSignData data = manager.getCurrentData(context);
        if (data == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "No active sign!");
            return;
        }

        data.setSignType(SignType.BUY);

        Location<World> loc = manager.updateData(data, context);
        manager.executeShowInfo(data, context, loc);
    }

    @Command(desc = "Changes the sign to a sell-sign")
    public void sell(Player context)
    {
        // TODO perms for sell/Buy maybe?
        MarketSignData data = manager.getCurrentData(context);
        if (data == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "No active sign!");
            return;
        }

        data.setSignType(SignType.SELL);

        Location<World> loc = manager.updateData(data, context);
        manager.executeShowInfo(data, context, loc);
    }

    @Command(desc = "Changes the demand of a sign")
    public void demand(Player context, Integer demand)
    {
        MarketSignData data = manager.getCurrentData(context);
        if (data == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "No active sign!");
            return;
        }

        if (data.getSignType() == null)
        {
            data.setSignType(SignType.SELL);
        }
        else if (data.getSignType() == SignType.BUY)
        {
            i18n.sendTranslated(context, NEGATIVE, "Buy signs cannot have a demand!");
            return;
        }

        data.setDemand(demand);
        Location<World> loc = manager.updateData(data, context);
        manager.executeShowInfo(data, context, loc);
    }

    @Command(desc = "Changes the demand of a sign")
    public void nodemand(Player context)
    {
        MarketSignData data = manager.getCurrentData(context);
        if (data == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "No active sign!");
            return;
        }

        data.setDemand(null);
        Location<World> loc = manager.updateData(data, context);
        manager.executeShowInfo(data, context, loc);
    }

    @Command(desc = "Changes the sign to an admin-sign")
    public void admin(Player context)
    {
        if (!context.hasPermission(module.perms().EDIT_ADMIN.getId()))
        {
            throw new PermissionDeniedException(module.perms().EDIT_ADMIN);
        }

        MarketSignData data = manager.getCurrentData(context);
        if (data == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "No active sign!");
            return;
        }

        data.setOwner(IMarketSignData.ADMIN_SIGN);

        Location<World> loc = manager.updateData(data, context);
        manager.executeShowInfo(data, context, loc);
    }

    @Command(desc = "Changes the sign to an player-sign")
    public void player(Player context)
    {
        if (!context.hasPermission(module.perms().EDIT_PLAYER_SELF.getId()))
        {
            throw new PermissionDeniedException(module.perms().EDIT_PLAYER_SELF);
        }

        MarketSignData data = manager.getCurrentData(context);
        if (data == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "No active sign!");
            return;
        }

        if (data.isAdminOwner())
        {
            data.setSize(6);
        }
        data.setOwner(context.getUniqueId());
        data.setStock(0);

        Location<World> loc = manager.updateData(data, context);
        manager.executeShowInfo(data, context, loc);
    }

    @Command(desc = "Changes the signs owner")
    public void owner(Player context, Player owner)
    {
        if (!context.equals(owner))
        {
            if (!context.hasPermission(module.perms().EDIT_PLAYER_OTHER.getId()))
            {
                throw new PermissionDeniedException(module.perms().EDIT_PLAYER_OTHER);
            }
        }
        else
        {
            if (!context.hasPermission(module.perms().EDIT_PLAYER_SELF.getId()))
            {
                throw new PermissionDeniedException(module.perms().EDIT_PLAYER_SELF);
            }
        }

        MarketSignData data = manager.getCurrentData(context);
        if (data == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "No active sign!");
            return;
        }

        data.setOwner(owner.getUniqueId());
        Location<World> loc = manager.updateData(data, context);
        manager.executeShowInfo(data, context, loc);
    }

    @Command(desc = "Changes whether the sign has stock")
    public void stock(Player context)
    {
        MarketSignData data = manager.getCurrentData(context);
        if (data == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "No active sign!");
            return;
        }

        if (!data.isAdminOwner())
        {
            i18n.sendTranslated(context, NEGATIVE, "Player signs cannot have no stock!");
            return;
        }

        if (data.getStock() == null)
        {
            data.setStock(0);
        }
        else
        {
            data.setStock(null);
        }
        Location<World> loc = manager.updateData(data, context);
        manager.executeShowInfo(data, context, loc);
    }

    @Command(desc = "Sets the signs stock")
    public void setstock(Player context, Integer amount)
    {
        MarketSignData data = manager.getCurrentData(context);
        if (data == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "No active sign!");
            return;
        }

        if (data.getStock() == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "This sign has no stock! Use \"stock\" first to enable it!");
            return;
        }

        data.setStock(amount);
        Location<World> loc = manager.updateData(data, context);
        manager.executeShowInfo(data, context, loc);
    }

    @Command(desc = "Sets the price")
    public void price(Player context, Double price)
    {
        if (price < 0)
        {
            i18n.sendTranslated(context, NEGATIVE, "A negative price!? Are you serious?");
            return;
        }

        MarketSignData data = manager.getCurrentData(context);
        if (data == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "No active sign!");
            return;
        }

        data.setPrice(price);
        Location<World> loc = manager.updateData(data, context);
        manager.executeShowInfo(data, context, loc);
    }

    @Command(desc = "Sets the amount")
    public void amount(Player context, Integer amount)
    {
        if (amount < 0)
        {
            i18n.sendTranslated(context, NEGATIVE, "Negative amounts could be unfair! Just sayin'");
            return;
        }

        MarketSignData data = manager.getCurrentData(context);
        if (data == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "No active sign!");
            return;
        }

        data.setAmount(amount);
        Location<World> loc = manager.updateData(data, context);
        manager.executeShowInfo(data, context, loc);
    }

    @Command(desc = "Sets the item")
    public void item(Player context, ItemStack item)
    {
        MarketSignData data = manager.getCurrentData(context);
        if (data == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "No active sign!");
            return;
        }

        if (!data.isAdminOwner() && data.getStock() != 0)
        {
            i18n.sendTranslated(context, NEGATIVE, "You have to take all items out of the market-sign to be able to change the item in it!");
            return;
        }
        data.setItem(item, false);
        Location<World> loc = manager.updateData(data, context);
        manager.executeShowInfo(data, context, loc);
    }

    @Command(desc = "Sets the signs inventory size")
    public void size(Player context, @org.cubeengine.butler.parametric.Optional Integer size,
                                     @ParameterPermission @Flag boolean infinite)
    {
        if (size == null || size < 0)
        {
            size = infinite ? -1 : 0;
        }
        if (size == 0 || size > 6)
        {
            i18n.sendTranslated(context, NEGATIVE, "Invalid size! Use -i for infinite OR 1-6 inventory-lines!");
            return;
        }

        MarketSignData data = manager.getCurrentData(context);
        if (data == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "No active sign!");
            return;
        }
        data.setSize(size);
        Location<World> loc = manager.updateData(data, context);
        manager.executeShowInfo(data, context, loc);
    }

    @Listener
    public void onClick(InteractBlockEvent event, @First Player player)
    {
        if (!this.hasUser(player))
        {
            return;
        }
        Optional<Location<World>> loc = event.getTargetBlock().getLocation();
        if (!loc.isPresent())
        {
            return;
        }

        if (!player.hasPermission(module.perms().EDIT_USE.getId()))
        {
            i18n.sendTranslated(player, NEGATIVE, "You are not allowed to edit MarketSigns here");
            manager.exitEditMode(player);
            removeUser(player);
            return;
        }

        boolean punch = event instanceof Primary;
        Boolean sneaking = player.get(Keys.IS_SNEAKING).get();

        if (manager.isActive(loc.get(), player)) // Its the active MarketSign -> Break or Modify Item
        {
            if (sneaking) // Do nothing if sneaking
            {
                return;
            }
            if (punch)
            {
                if (manager.tryBreakActive(player))
                {
                    return;
                }
            }
            else if (player.getItemInHand().isPresent())
            {
                manager.modifyItemActive(player, player.getItemInHand().get());
            }
            event.setCancelled(true);
            return;
        }

        if (loc.get().get(MarketSignData.class).isPresent()) // Its another MarketSign
        {
            manager.setSign(loc.get(), player); // Set Current Sign
            event.setCancelled(true);
            return;
        }

        if (loc.get().getBlockType() != BlockTypes.STANDING_SIGN && loc.get().getBlockType() != BlockTypes.WALL_SIGN)
        {
            // Not even a sign -> ignore
            return;
        }
        // Its a sign ; but no data yet
        if (sneaking && !punch)
        {
            return;
        }
        if (!sneaking || !punch)
        {
            i18n.sendTranslated(player, NEGATIVE, "That is not a market sign!");
            i18n.sendTranslated(player, NEUTRAL, "Sneak and punch the sign convert it.");
            event.setCancelled(true);
            return;
        }
        // sneark + punch -> convert it!
        MarketSignData data = new MarketSignData();
        data.setID(UUID.randomUUID());
        data.setOwner(player.getUniqueId());
        loc.get().offer(data); // Sign converted! Now set active
        manager.setSign(loc.get(), player);
        event.setCancelled(true);
    }

    @Listener(order = Order.POST)
    public void onSignPlace(ChangeBlockEvent.Place event, @First Player player)
    {
        /*
        if (event.getTransactions().size() > 1 || !hasUser(player))
        {
            return;
        }

        for (Transaction<BlockSnapshot> trans : event.getTransactions())
        {
            BlockType type = trans.getFinal().getState().getType();
            if (type == BlockTypes.STANDING_SIGN || type == BlockTypes.WALL_SIGN) // placed sign
            {
                if (!player.hasPermission(module.perms().EDIT_USE.getId()))
                {
                    event.setCancelled(true);
                    i18n.sendTranslated(player, NEGATIVE, "You are not allowed to create market signs!");
                    return;
                }

                MarketSignData data = new MarketSignData();
                data.setID(UUID.randomUUID());
                trans.setCustom(trans.getFinal().with(data.asImmutable()).get());
               // TODO this is too soon manager.setSign(trans.getFinal().getLocation().get(), player);
            }
        }
        */
    }

    @Listener
    public void onSignChange(ChangeSignEvent event)
    {
        Optional<MarketSignData> data = event.getTargetTile().get(MarketSignData.class);
        if (data.isPresent())
        {
            manager.updateSignText(data.get(), event.getTargetTile().getLocation());
        }
    }
}
