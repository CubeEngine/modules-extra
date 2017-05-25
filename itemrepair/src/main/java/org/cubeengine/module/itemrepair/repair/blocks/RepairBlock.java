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
package org.cubeengine.module.itemrepair.repair.blocks;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.effect.sound.SoundTypes.BLOCK_ANVIL_BREAK;
import static org.spongepowered.api.effect.sound.SoundTypes.ENTITY_PLAYER_BURP;

import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.itemrepair.Itemrepair;
import org.cubeengine.module.itemrepair.material.BaseMaterial;
import org.cubeengine.module.itemrepair.material.BaseMaterialContainer;
import org.cubeengine.module.itemrepair.material.RepairItem;
import org.cubeengine.module.itemrepair.material.RepairItemContainer;
import org.cubeengine.module.itemrepair.repair.RepairBlockManager;
import org.cubeengine.module.itemrepair.repair.RepairRequest;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.meta.ItemEnchantment;
import org.spongepowered.api.data.value.mutable.MutableBoundedValue;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.property.InventoryCapacity;
import org.spongepowered.api.item.inventory.property.InventoryDimension;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.text.Text;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;

public class RepairBlock
{
    private final BaseMaterialContainer priceProvider;
    protected final RepairItemContainer itemProvider;
    private final RepairBlockManager repairBlockManager;
    private final Permission permission;

    private final Itemrepair module;

    private final Map<String, RepairBlockInventory> inventoryMap;

    private final RepairBlockConfig config;

    private final Random rand;
    private final String name;

    private final EconomyService economy;
    private I18n i18n;

    public RepairBlock(Itemrepair module, RepairBlockManager manager, String name, RepairBlockConfig config,
                       PermissionManager pm, EconomyService economy, I18n i18n)
    {
        this.module = module;
        this.name = name;
        this.repairBlockManager = manager;
        this.economy = economy;
        this.i18n = i18n;
        this.itemProvider = repairBlockManager.getItemProvider();
        this.priceProvider = itemProvider.getPriceProvider();
        this.permission = pm.register(Itemrepair.class, "block." + name, "", null);
        this.inventoryMap = new HashMap<>();
        this.rand = new Random(System.currentTimeMillis());
        this.config = config;
    }

    public final String getName()
    {
        return this.name;
    }

    public final String getTitle()
    {
        return this.config.title;
    }

    public final Permission getPermission()
    {
        return this.permission;
    }

    public final BlockType getMaterial()
    {
        return this.config.block;
    }

    public double calculatePrice(Iterable<ItemStack> items)
    {
        return this.calculatePrice(items, this.module.getConfig().price.enchantMultiplier.factor,
           this.module.getConfig().price.enchantMultiplier.base, this.config.costPercentage);
    }

    private double calculatePrice(Iterable<ItemStack> items, double enchantmentFactor, double enchantmentBase, float percentage)
    {
        double price = 0.0;

        ItemType type;
        RepairItem item;
        double currentPrice;
        for (ItemStack itemStack : items)
        {
            type = itemStack.getItem();
            item = itemProvider.of(type);
            currentPrice = 0;
            for (Entry<BaseMaterial, Integer> entry : item.getBaseMaterials().entrySet())
            {
                currentPrice += entry.getKey().getPrice() * entry.getValue();
            }
            MutableBoundedValue<Integer> dura = itemStack.getValue(Keys.ITEM_DURABILITY).get();
            currentPrice *= (double)Math.min(dura.get(), dura.getMaxValue()) / dura.getMaxValue();
            currentPrice *= getEnchantmentMultiplier(itemStack, enchantmentFactor, enchantmentBase);

            price += currentPrice;
        }
        price *= percentage/100;
        return price;
    }

    public RepairBlockInventory removeInventory(final Player player)
    {
        return this.inventoryMap.remove(player.getName());
    }

    public RepairBlockInventory getInventory(final Player player)
    {
        if (player == null)
        {
            return null;
        }
        RepairBlockInventory inventory = this.inventoryMap.get(player.getName());
        if (inventory == null)
        {
            Inventory inv = Inventory.builder().of(InventoryArchetypes.CHEST)
                    .property(InventoryDimension.PROPERTY_NAME, InventoryDimension.of(9,4))
                    .property(InventoryCapacity.class.getSimpleName().toLowerCase(), InventoryCapacity.of(9*4))
                    .property(InventoryTitle.PROPERTY_NAME, InventoryTitle.of(Text.of(getTitle())))
                    .build(module.getPlugin());
            inventory = new RepairBlockInventory(inv, player);
            this.inventoryMap.put(player.getName(), inventory);
        }
        return inventory;
    }

    public class RepairBlockInventory
    {
        public final Inventory inventory;
        public final Player player;

        public RepairBlockInventory(Inventory inventory, Player player)
        {
            this.inventory = inventory;
            this.player = player;
        }
    }

    public boolean withdrawPlayer(Player player, double price)
    {
        UniqueAccount acc = economy.getOrCreateAccount(player.getUniqueId()).get();// Make sure account exists
        TransactionResult result = acc.withdraw(economy.getDefaultCurrency(), new BigDecimal(price), Cause.of(NamedCause.source(player)));
        if (result.getResult() == ResultType.SUCCESS)
        {
            // TODO bankAccounts
            return true;
        }
        return false;
    }

    public RepairRequest requestRepair(RepairBlockInventory inventory)
    {
        Player player = inventory.player;
        Map<Integer, ItemStack> items = this.itemProvider.getRepairableItems(inventory.inventory);
        if (items.size() > 0)
        {
            Double price = calculatePrice(items.values());
            Text format = economy.getDefaultCurrency().format(new BigDecimal(price));
            if (this.config.breakPercentage > 0)
            {
                i18n.sendTranslated(player, NEGATIVE, "Items will break with a chance of {decimal:2}%", this.config.breakPercentage);
            }
            if (this.config.failPercentage > 0)
            {
                i18n.sendTranslated(player, NEGATIVE, "Items will not repair with a chance of {decimal:2}%", this.config.failPercentage);
            }
            if (this.config.looseEnchantmentsPercentage > 0)
            {
                i18n.sendTranslated(player, NEGATIVE, "Items will loose all enchantments with a chance of {decimal:2}%", this.config.looseEnchantmentsPercentage);
            }
            if (this.config.costPercentage > 100)
            {
                i18n.sendTranslated(player, NEUTRAL, "The repair would cost {txt#amount} (+{decimal:2}%)", format, this.config.costPercentage - 100);
            }
            else if (this.config.costPercentage < 100)
            {
               i18n.sendTranslated(player, NEUTRAL, "The repair would cost {txt#amount} (-{decimal:2}%)", format, 100 - this.config.costPercentage);
            }
            else
            {
                i18n.sendTranslated(player, NEUTRAL, "The repair would cost {txt#amount}", format);
            }
            UniqueAccount acc = economy.getOrCreateAccount(player.getUniqueId()).get();
            i18n.sendTranslated(player, NEUTRAL, "You currently have {txt#balance}", economy.getDefaultCurrency().format(acc.getBalance(economy.getDefaultCurrency())));
            i18n.sendTranslated(player, POSITIVE, "{text:Leftclick} again to repair all your damaged items.");
            return new RepairRequest(this, inventory, items, price);
        }
        else
        {
            i18n.sendTranslated(player, NEGATIVE, "There are no items to repair!");
        }
        return null;
    }

    public void repair(RepairRequest request)
    {
        double price = request.getPrice();
        RepairBlockInventory inventory = request.getInventory();
        Player player = inventory.player;
        if (withdrawPlayer(player, price))
        {
            boolean itemsBroken = false;
            boolean repairFail = false;
            boolean looseEnch = false;
            ItemStack item;
            int amount;
            for (Map.Entry<Integer, ItemStack> entry : request.getItems().entrySet())
            {
                item = entry.getValue();
                if (this.rand.nextInt(100) >= this.config.breakPercentage)
                {
                    if (this.rand.nextInt(100) >= this.config.failPercentage)
                    {
                        repairItem(entry.getValue());
                    }
                    else
                    {
                        repairFail = true;
                    }
                    Optional<List<ItemEnchantment>> enchs = entry.getValue().get(Keys.ITEM_ENCHANTMENTS);
                    if (enchs.isPresent() && !enchs.get().isEmpty())
                    {
                        if (this.rand.nextInt(100) < this.config.looseEnchantmentsPercentage)
                        {
                            looseEnch = true;
                            entry.getValue().remove(Keys.ITEM_ENCHANTMENTS);
                        }
                    }
                }
                else
                {
                    itemsBroken = true;
                    amount = item.getQuantity();
                    item.setQuantity(amount - 1);
                    repairItem(item);
                }
                inventory.inventory.query(SlotIndex.of(entry.getKey())).set(entry.getValue());
            }
            if (itemsBroken)
            {
                i18n.sendTranslated(player, NEGATIVE, "You broke some of your items when repairing!");
                player.playSound(BLOCK_ANVIL_BREAK, player.getLocation().getPosition(), 1, 0);
            }
            if (repairFail)
            {
                i18n.sendTranslated(player, NEGATIVE, "You failed to repair some of your items!");
                player.playSound(ENTITY_PLAYER_BURP,player.getLocation().getPosition(), 1,0);
            }
            if (looseEnch)
            {
                i18n.sendTranslated(player, NEGATIVE, "Oh no! Some of your items lost their magical power.");
                player.playSound(SoundTypes.ENTITY_GHAST_SCREAM, player.getLocation().getPosition(), 1);
            }
            i18n.sendTranslated(player, POSITIVE, "You paid {txt#amount} to repair your items!", economy.getDefaultCurrency().format(new BigDecimal(price)));
            if (this.config.costPercentage > 100)
            {
                i18n.sendTranslated(player, POSITIVE, "Thats {decimal#percent:2}% of the normal price!", this.config.costPercentage);
            }
            else if (this.config.costPercentage < 100)
            {
                i18n.sendTranslated(player, POSITIVE, "Thats {decimal#percent:2}% less then the normal price!", 100 - this.config.costPercentage);
            }
        }
        else
        {
           i18n.sendTranslated(player, NEGATIVE, "You don't have enough money to repair these items!");
        }
    }

    /*
     * Utilities
     */

    public static double getEnchantmentMultiplier(ItemStack item, double factor, double base)
    {
        double enchantmentLevel = 0;
        Optional<List<ItemEnchantment>> enchs = item.get(Keys.ITEM_ENCHANTMENTS);
        if (enchs.isPresent() && !enchs.get().isEmpty())
        {
            for (ItemEnchantment enchantment : enchs.get())
            {
                enchantmentLevel += enchantment.getLevel();
            }
        }

        if (enchantmentLevel > 0)
        {
            double enchantmentMultiplier = factor * Math.pow(base, enchantmentLevel);

            enchantmentMultiplier = enchantmentMultiplier / 100.0 + 1.0;

            return enchantmentMultiplier;
        }
        else
        {
            return 1.0;
        }
    }

    public static void repairItems(RepairRequest request)
    {
        repairItems(request.getItems().values());
    }

    public static void repairItems(Iterable<ItemStack> items)
    {
        repairItems(items, (short)0);
    }

    public static void repairItems(Iterable<ItemStack> items, short durability)
    {
        for (ItemStack item : items)
        {
            repairItem(item, durability);
        }
    }

    public static void repairItem(ItemStack item)
    {
        repairItem(item, item.getValue(Keys.ITEM_DURABILITY).get().getMaxValue());
    }

    public static void repairItem(ItemStack item, int durability)
    {
        if (item != null)
        {
            item.offer(Keys.ITEM_DURABILITY, durability);
        }
    }
}
