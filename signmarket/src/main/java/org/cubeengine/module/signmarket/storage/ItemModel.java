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
package org.cubeengine.module.signmarket.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Transient;
import org.cubeengine.module.core.util.StringUtils;
import org.cubeengine.module.signmarket.MarketSign;
import org.cubeengine.service.database.AsyncRecord;
import org.jooq.types.UInteger;
import org.jooq.types.UShort;
import org.spongepowered.api.item.Enchantment;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;

public class ItemModel extends AsyncRecord<ItemModel> implements InventoryHolder, Cloneable
{
    @Transient
    private final Set<MarketSign> sharedStockSigns = new HashSet<>();
    @Transient
    public Inventory inventory;
    @Transient
    private ItemStack itemStack;

    public ItemModel()
    {
        super(TableSignItem.TABLE_SIGN_ITEM);
        this.setValue(TableSignItem.TABLE_SIGN_ITEM.KEY, UInteger.valueOf(0));
    }

    private String getEnchantmentsAsString(ItemStack item)
    {
        Map<Enchantment, Integer> enchs;
        if (item.getItemMeta() instanceof EnchantmentStorageMeta)
        {
            EnchantmentStorageMeta itemMeta = (EnchantmentStorageMeta)item.getItemMeta();
            enchs = itemMeta.getStoredEnchants();
        }
        else
        {
            enchs = item.getEnchantments();
        }
        if (!enchs.isEmpty())
        {
            List<String> enchStrings = new ArrayList<>();
            for (Enchantment ench : enchs.keySet())
            {
                enchStrings.add(ench.getId() + ":" + enchs.get(ench));
            }
            return StringUtils.implode(",", enchStrings);
        }
        return null;
    }

    public boolean matchesItem(ItemStack itemInHand)
    {
        ItemStack itemInSign = this.getItemStack();
        if (itemInSign.hasItemMeta() && itemInHand.hasItemMeta())
        {
            if (itemInSign.getItemMeta() instanceof Repairable && itemInHand.getItemMeta() instanceof Repairable)
            {
                ItemMeta itemMeta = itemInSign.getItemMeta();
                ((Repairable)itemMeta).setRepairCost(((Repairable)itemInHand.getItemMeta()).getRepairCost());
                itemInSign.setItemMeta(itemMeta); // repairCost is not saved
            }
        }
        return this.getItemStack().isSimilar(itemInHand);
    }

    /**
     * Returns the ItemStack of the item saved in this sign with amount 0.
     *
     * @return the itemstack
     */
    public ItemStack getItemStack()
    {
        if (this.itemStack == null)
        {
            String item = this.getValue(TableSignItem.TABLE_SIGN_ITEM.ITEM);
            if (item == null)
            {
                return null;
            }
            this.itemStack = new ItemStack(Material.valueOf(item), 0, this.getValue(TableSignItem.TABLE_SIGN_ITEM.DAMAGEVALUE).shortValue());
            ItemMeta meta = this.itemStack.getItemMeta();
            if (this.getValue(TableSignItem.TABLE_SIGN_ITEM.CUSTOMNAME) != null)
            {
                meta.setDisplayName(this.getValue(TableSignItem.TABLE_SIGN_ITEM.CUSTOMNAME));
            }
            if (this.getValue(TableSignItem.TABLE_SIGN_ITEM.LORE) != null)
            {
                meta.setLore(Arrays.asList(StringUtils.explode("\n", this.getValue(TableSignItem.TABLE_SIGN_ITEM.LORE))));
            }
            if (this.getValue(TableSignItem.TABLE_SIGN_ITEM.ENCHANTMENTS) != null)
            {
                String[] enchStrings = StringUtils.explode(",", this.getValue(TableSignItem.TABLE_SIGN_ITEM.ENCHANTMENTS));
                for (String enchString : enchStrings)
                {
                    String[] split = StringUtils.explode(":", enchString);
                    Enchantment ench = Enchantment.getById(Integer.parseInt(split[0]));
                    int level = Integer.parseInt(split[1]);
                    if (meta instanceof EnchantmentStorageMeta)
                    {
                        ((EnchantmentStorageMeta)meta).addStoredEnchant(ench, level, true);
                    }
                    else
                    {
                        meta.addEnchant(ench, level, true);
                    }
                }
            }
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    public void setItemStack(ItemStack item)
    {
        this.setValue(TableSignItem.TABLE_SIGN_ITEM.ITEM, item.getType().name());
        this.setValue(TableSignItem.TABLE_SIGN_ITEM.DAMAGEVALUE, UShort.valueOf(item.getDurability()));
        this.setValue(TableSignItem.TABLE_SIGN_ITEM.ENCHANTMENTS, this.getEnchantmentsAsString(item));
        this.setValue(TableSignItem.TABLE_SIGN_ITEM.CUSTOMNAME, null);
        this.setValue(TableSignItem.TABLE_SIGN_ITEM.LORE, null);
        ItemMeta meta = item.getItemMeta();
        if (meta.hasDisplayName())
        {
            this.setValue(TableSignItem.TABLE_SIGN_ITEM.CUSTOMNAME, meta.getDisplayName());
        }
        if (meta.hasLore())
        {
            this.setValue(TableSignItem.TABLE_SIGN_ITEM.LORE, StringUtils.implode("\n", meta.getLore()));
        }
        // Transient Fields:
        this.itemStack = null;
        this.inventory = null;
    }

    public void removeSign(MarketSign marketSign)
    {
        this.sharedStockSigns.remove(marketSign);
    }

    public void addSign(MarketSign marketSign)
    {
        this.sharedStockSigns.add(marketSign);
    }

    public boolean isNotReferenced()
    {
        return this.sharedStockSigns.isEmpty();
    }

    public boolean sharesStock()
    {
        return this.sharedStockSigns.size() > 1;
    }

    public void updateSignTexts()
    {
        for (MarketSign sign : this.sharedStockSigns)
        {
            sign.updateSignText();
        }
    }

    @Override
    public Inventory getInventory()
    {
        return this.inventory;
    }

    public void initInventory(Inventory inventory)
    {
        this.inventory = inventory;
    }

    public Set<MarketSign> getReferenced()
    {
        return this.sharedStockSigns;
    }

    public ItemModel clone()
    {
        ItemModel itemInfo = CubeEngine.getCore().getDB().getDSL().newRecord(TableSignItem.TABLE_SIGN_ITEM);
        itemInfo.copyValuesFrom(this);
        return itemInfo;
    }

    public void copyValuesFrom(ItemModel itemInfo)
    {
        this.setValue(TableSignItem.TABLE_SIGN_ITEM.STOCK, itemInfo.getValue(TableSignItem.TABLE_SIGN_ITEM.STOCK));
        this.setValue(TableSignItem.TABLE_SIGN_ITEM.ITEM, itemInfo.getValue(TableSignItem.TABLE_SIGN_ITEM.ITEM));
        this.setValue(TableSignItem.TABLE_SIGN_ITEM.DAMAGEVALUE, itemInfo.getValue(TableSignItem.TABLE_SIGN_ITEM.DAMAGEVALUE));
        this.setValue(TableSignItem.TABLE_SIGN_ITEM.CUSTOMNAME, itemInfo.getValue(TableSignItem.TABLE_SIGN_ITEM.CUSTOMNAME));
        this.setValue(TableSignItem.TABLE_SIGN_ITEM.LORE, itemInfo.getValue(TableSignItem.TABLE_SIGN_ITEM.LORE));
        this.setValue(TableSignItem.TABLE_SIGN_ITEM.ENCHANTMENTS, itemInfo.getValue(TableSignItem.TABLE_SIGN_ITEM.ENCHANTMENTS));
        this.setValue(TableSignItem.TABLE_SIGN_ITEM.SIZE, itemInfo.getValue(TableSignItem.TABLE_SIGN_ITEM.SIZE));
        // Transient field:
        this.inventory = null;
        this.itemStack = null;
    }
}
