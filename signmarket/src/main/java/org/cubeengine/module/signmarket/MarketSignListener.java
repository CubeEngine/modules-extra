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
package org.cubeengine.module.signmarket;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.BlockUtil;
import org.cubeengine.module.signmarket.data.ImmutableMarketSignData;
import org.cubeengine.module.signmarket.data.MarketSignData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent.Primary;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.spongepowered.api.block.BlockTypes.AIR;
import static org.spongepowered.api.block.BlockTypes.STANDING_SIGN;
import static org.spongepowered.api.block.BlockTypes.WALL_SIGN;
import static org.spongepowered.api.util.Direction.DOWN;
import static org.spongepowered.api.util.Direction.UP;

public class MarketSignListener
{
    private final  MarketSignManager manager;
    private final Signmarket module;
    private final I18n i18n;

    public MarketSignListener(MarketSignManager manager, Signmarket module, I18n i18n)
    {
        this.manager = manager;
        this.module = module;
        this.i18n = i18n;
    }

    @Listener
    public void onBlockBreak(ChangeBlockEvent.Break event, @First Player player)
    {
        for (Transaction<BlockSnapshot> transaction : event.getTransactions())
        {
            BlockSnapshot orig = transaction.getOriginal();
            BlockType type = orig.getState().getType();
            if (type == STANDING_SIGN || type == WALL_SIGN)
            {
                Optional<ImmutableMarketSignData> signData = orig.get(ImmutableMarketSignData.class);
                if (signData.isPresent())
                {
                    if (signData.get().getSignType() != null)
                    {
                        event.setCancelled(true);
                        return;
                    }
                }
            }

            Location<World> origLoc = orig.getLocation().get();
            for (Direction blockFace : BlockUtil.BLOCK_FACES)
            {
                if (blockFace == DOWN)
                {
                    continue;
                }
                Location<World> relative = origLoc.getRelative(blockFace);
                if (!relative.get(MarketSignData.class).isPresent())
                {
                    continue;
                }
                if (blockFace == UP)
                {
                    if (relative.getBlockType() == STANDING_SIGN)
                    {
                        event.setCancelled(true);
                        return;
                    }
                }
                else
                {
                    if (relative.getBlockType() == WALL_SIGN
                     && relative.get(Keys.DIRECTION).get() == blockFace)
                    {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @Listener
    public void changeWorld(MoveEntityEvent.Teleport event)
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

    @Listener
    public void onClick(InteractBlockEvent event, @First Player player)
    {
        if (!event.getTargetBlock().getLocation().isPresent())
        {
            return;
        }
        Location<World> loc = event.getTargetBlock().getLocation().get();
        BlockType blockType = loc.getBlockType();
        if (!Arrays.asList(STANDING_SIGN, WALL_SIGN).contains(blockType))
        {
            return; // Not even a sign -> ignore
        }

        boolean editMode = module.getEditModeCommand().hasUser(player);
        Boolean sneaking = player.get(Keys.IS_SNEAKING).get();
        boolean punch = event instanceof Primary;

        Optional<MarketSignData> mSignData = event.getTargetBlock().get(ImmutableMarketSignData.class).map(ImmutableMarketSignData::asMutable);
        if (mSignData.isPresent())
        {
            manager.updateSignText(mSignData.get(), loc);
        }

        if (editMode)
        {
            if (!player.hasPermission(module.perms().EDIT_USE.getId()))
            {
                i18n.send(player, NEGATIVE, "You are not allowed to edit MarketSigns here");
                manager.exitEditMode(player);
                module.getEditModeCommand().removeUser(player);
                return;
            }

            if (manager.isActive(loc, player)) // Its the active MarketSign -> Break or Modify Item
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
                    else
                    {
                        event.setCancelled(true); // TODO resend signtext?
                    }
                }

                if (!punch && player.getItemInHand(HandTypes.MAIN_HAND).isPresent())
                {
                    manager.modifyItemActive(player, player.getItemInHand(HandTypes.MAIN_HAND).get());
                }
                event.setCancelled(true);
                return;
            }

            if (mSignData.isPresent()) // Its another MarketSign
            {
                manager.setSign(loc, player); // Set Current Sign
                event.setCancelled(true);
                return;
            }

            // Its a sign ; but no data yet
            if (sneaking && !punch) // Sneak Interact -> Allow Interaction
            {
                return;
            }
            if (sneaking && punch) // Sneak AND Punch to convert sign
            {
                // sneak + punch -> convert it!
                MarketSignData data = new MarketSignData();
                data.setID(UUID.randomUUID());
                data.setOwner(player.getUniqueId());
                loc.offer(data); // Sign converted! Now set active
                manager.setSign(loc, player);
                event.setCancelled(true);
                return;
            }
            i18n.send(player, NEGATIVE, "That is not a market sign!");
            i18n.send(player, NEUTRAL, "Sneak and punch the sign convert it.");
            event.setCancelled(true);
            return;
        }
        if (!mSignData.isPresent())
        {
            return;
        }

        MarketSignData data = mSignData.get();
        manager.executeSignAction(data, loc, player, event instanceof InteractBlockEvent.Secondary);
        if (blockType != AIR)
        {
            // TODO sign somehow is retained /w some invalid data
            loc.offer(data);
            manager.updateSignText(data, loc);
            //event.setCancelled(true);
        }
    }

    @Listener(order = Order.POST)
    public void onSignPlace(ChangeBlockEvent.Place event, @First Player player)
    {
        if (event.getTransactions().size() > 1 || !module.getEditModeCommand().hasUser(player))
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
                    i18n.send(player, NEGATIVE, "You are not allowed to create market signs!");
                    return;
                }

                MarketSignData data = new MarketSignData();
                data.setID(UUID.randomUUID());
                data.setOwner(player.getUniqueId());
                Location<World> loc = trans.getFinal().getLocation().get();
                loc.offer(data);
                manager.setSign(loc, player);
                Sponge.getCauseStackManager().pushCause(player);
                player.closeInventory();
            }
        }
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
