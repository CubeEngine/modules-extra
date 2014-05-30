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
package de.cubeisland.engine.module.signmarket;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;

import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.module.signmarket.storage.BlockModel;
import de.cubeisland.engine.module.signmarket.storage.ItemModel;
import de.cubeisland.engine.module.signmarket.storage.SignMarketBlockManager;
import de.cubeisland.engine.module.signmarket.storage.SignMarketItemManager;
import org.jooq.types.UInteger;

import static de.cubeisland.engine.core.util.formatter.MessageType.NEGATIVE;
import static de.cubeisland.engine.module.signmarket.storage.TableSignBlock.TABLE_SIGN_BLOCK;
import static de.cubeisland.engine.module.signmarket.storage.TableSignItem.TABLE_SIGN_ITEM;

public class MarketSignFactory
{
    private final Map<Location, MarketSign> marketSigns = new HashMap<>();

    private final SignMarketItemManager signMarketItemManager;
    private final SignMarketBlockManager signMarketBlockManager;

    private final Signmarket module;

    public MarketSignFactory(Signmarket module)
    {
        this.module = module;
        this.signMarketItemManager = new SignMarketItemManager(module);
        this.signMarketBlockManager = new SignMarketBlockManager(module);
    }

    public void loadInAllSigns()
    {
        this.signMarketItemManager.load();
        this.signMarketBlockManager.load();
        Set<UInteger> usedItemKeys = new HashSet<>();
        for (BlockModel blockModel : this.signMarketBlockManager.getLoadedModels())
        {
            ItemModel itemModel = this.signMarketItemManager.getInfoModel(blockModel.getValue(TABLE_SIGN_BLOCK.ITEMKEY));
            if (itemModel == null)
            {
                this.module.getLog().warn("Inconsistent Data! BlockInfo without Marketsigninfo!");
                continue;
            }
            MarketSign marketSign = new MarketSign(module, itemModel, blockModel);
            usedItemKeys.add(blockModel.getValue(TABLE_SIGN_BLOCK.ITEMKEY));
            this.marketSigns.put(blockModel.getLocation(),marketSign);
        }
        this.signMarketItemManager.deleteUnusedModels(usedItemKeys);
    }

    public MarketSign getSignAt(Location location)
    {
        if (location == null)
        {
            return null;
        }
        if (this.module.getConfig().disableInWorlds.contains(location.getWorld().getName()))
        {
            return null;
        }
        return this.marketSigns.get(location);
    }

    public MarketSign createSignAt(User user, Location location)
    {
        if (this.module.getConfig().disableInWorlds.contains(location.getWorld().getName()))
        {
            return null;
        }
        MarketSign marketSign = this.getSignAt(location);
        if (marketSign != null)
        {
            this.module.getLog().warn("Tried to create sign at occupied position!");
            return marketSign;
        }
        marketSign = new MarketSign(this.module, location);
        if (module.perms().SIGN_CREATE_ADMIN_CREATE.isAuthorized(user) && module.getConfig().enableAdmin)
        {
            marketSign.setAdminSign();
        }
        else if (module.perms().SIGN_CREATE_USER_CREATE.isAuthorized(user) && module.getConfig().enableUser)
        {
            marketSign.setOwner(user);
        }
        else
        {
            user.sendTranslated(NEGATIVE, "You are not allowed to create Admin or User MarketSigns!");
            return null;
        }
        if (marketSign.isAdminSign())
        {
            if (this.module.getConfig().allowAdminNoStock)
            {
                marketSign.setAdminSign();
                marketSign.setNoStock();
            }
            else if (this.module.getConfig().allowAdminStock)
            {
                marketSign.setAdminSign();
                marketSign.setNoStock();
            }
            marketSign.setSize(this.module.getConfig().maxAdminStock);
        }
        else
        {
            marketSign.setSize(this.module.getConfig().maxUserStock);
        }
        this.marketSigns.put(marketSign.getLocation(), marketSign);
        return marketSign;
    }

    /**
     * Deletes a marketSign forever!
     * This will delete the blockModel and the itemModel if it is no longer referenced.
     *
     * @param marketSign
     */
    public void delete(MarketSign marketSign)
    {
        this.marketSigns.remove(marketSign.getLocation());
        this.signMarketBlockManager.delete(marketSign.getBlockInfo());
        this.module.getLog().debug("{} deleted block-model #{}", marketSign.isAdminSign() ? "Server" : marketSign.getOwner().getDisplayName(), marketSign.getBlockInfo().getValue(
            TABLE_SIGN_BLOCK.KEY));
        ItemModel itemInfo = marketSign.getItemInfo();
        itemInfo.removeSign(marketSign);
        if (itemInfo.isNotReferenced())
        {
            this.signMarketItemManager.delete(itemInfo);
            this.module.getLog().debug("{} deleted item-model #{}", marketSign.isAdminSign() ? "Server" : marketSign.getOwner().getDisplayName(), itemInfo.getValue(
                TABLE_SIGN_BLOCK.KEY));
        }
    }

    public void syncAndSaveSign(MarketSign sign)
    {
        if (sign.getItemInfo().getValue(TABLE_SIGN_ITEM.KEY).longValue() == 0 || sign.getItemInfo().getReferenced().size() == 1) // de-synced sign OR possibly sync-able sign
        {
            for (MarketSign other : this.marketSigns.values())
            {
                if (other.hasDemand()) // skip if limited demand
                {
                    continue;
                }
                if (((sign.isAdminSign() && other.isAdminSign()) || (!sign.isAdminSign() && sign.getRawOwner().equals(other.getRawOwner()) && sign != other))  // same owner (but not same sign)
                    && sign.canSync(other)) // both have stock AND same item -> doSync
                {
                    // apply the found item-info to the marketsign
                    ItemModel itemModel = sign.setItemInfo(other.getItemInfo());
                    if (sign.syncOnMe) // stock OR stock-size change
                    {
                        sign.setStock(itemModel.getValue(TABLE_SIGN_ITEM.STOCK).intValue());
                        sign.setSize(itemModel.getValue(TABLE_SIGN_ITEM.SIZE).intValue());
                        sign.syncOnMe = false;
                    }
                    this.saveOrUpdate(sign);
                    this.module.getLog().debug("block-model #{} synced onto the item-model #{} (size: {})" ,
                                               sign.getBlockInfo().getValue(
                                                   TABLE_SIGN_BLOCK.KEY), sign.getItemInfo().getValue(
                        TABLE_SIGN_ITEM.KEY), sign.getItemInfo().getReferenced().size());
                    if (itemModel.getValue(TABLE_SIGN_ITEM.KEY).longValue() != 0 && itemModel.isNotReferenced())
                    {
                        this.signMarketItemManager.delete(itemModel); // delete if no more referenced
                        this.module.getLog().debug("{} deleted item-model #{}", sign.isAdminSign() ? "Server" : sign.getOwner().getDisplayName(), sign.getItemInfo().getValue(
                            TABLE_SIGN_ITEM.KEY));
                    }
                    sign.getItemInfo().updateSignTexts(); // update all signs that use the same itemInfo
                    return;
                }
            }
            // no sync -> new ItemModel
        }
        this.saveOrUpdate(sign);
        sign.getItemInfo().updateSignTexts(); // update all signs that use the same itemInfo
    }

    private void saveOrUpdate(MarketSign marketSign)
    {
        if (marketSign.isValidSign(null))
        {
            if (marketSign.getItemInfo().getValue(TABLE_SIGN_ITEM.KEY).longValue() == 0) // itemInfo not saved in database
            {
                this.signMarketItemManager.store(marketSign.getItemInfo());
                this.module.getLog().debug("{} stored item-model #{}", marketSign.isAdminSign() ? "Server" : marketSign.getOwner().getDisplayName(), marketSign.getItemInfo().getValue(
                    TABLE_SIGN_ITEM.KEY));
                // set freshly assigned itemData reference in BlockInfo
                marketSign.getBlockInfo().setValue(TABLE_SIGN_BLOCK.ITEMKEY, marketSign.getItemInfo().getValue(TABLE_SIGN_ITEM.KEY));
            }
            else // update
            {
                this.signMarketItemManager.update(marketSign.getItemInfo());
            }
            if (marketSign.getBlockInfo().getValue(TABLE_SIGN_BLOCK.KEY).longValue() == 0) // blockInfo not saved in database
            {
                this.signMarketBlockManager.store(marketSign.getBlockInfo());
                this.module.getLog().debug("{} stored block-model #{}", marketSign.isAdminSign() ? "Server" : marketSign.getOwner().getDisplayName(), marketSign.getBlockInfo().getValue(TABLE_SIGN_BLOCK.KEY));
            }
            else // update
            {
                this.signMarketBlockManager.update(marketSign.getBlockInfo());
            }
        }
    }
}
