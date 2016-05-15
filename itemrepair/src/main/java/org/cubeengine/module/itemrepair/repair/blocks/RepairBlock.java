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
package org.cubeengine.module.itemrepair.repair.blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
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
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.Enchantment;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.economy.EconomyService;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.effect.sound.SoundTypes.ANVIL_BREAK;
import static org.spongepowered.api.effect.sound.SoundTypes.BURP;

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
            currentPrice *= (double)Math.min(itemStack.getDurability(), type.getMaxDurability()) / (double)type.getMaxDurability();
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
            inventory = new RepairBlockInventory(Bukkit.createInventory(player, 9 * 4, this.config.title), player);
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

    public boolean withdrawPlayer(Player user, double price)
    {
        economy.createAccount(user.getUniqueId()); // Make sure account exists
        if (economy.has(user.getUniqueId(), price) && economy.withdraw(user.getUniqueId(), price))
        {
            // TODO bankAccounts
            /*
            String account = this.plugin.getServerBank();
            if (eco.hasBankSupport() && !("".equals(account)))
            {
                eco.bankDeposit(account, amount);
            }
            else
            {
                account = this.plugin.getServerPlayer();
                if (!("".equals(account)) && eco.hasAccount(account))
                {
                    eco.depositPlayer(account, amount);
                }
            }
            */
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
            String format = economy.format(price);
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
                i18n.sendTranslated(player, NEUTRAL, "The repair would cost {input#amount} (+{decimal:2}%)", format, this.config.costPercentage - 100);
            }
            else if (this.config.costPercentage < 100)
            {
               i18n.sendTranslated(player, NEUTRAL, "The repair would cost {input#amount} (-{decimal:2}%)", format, 100 - this.config.costPercentage);
            }
            else
            {
                i18n.sendTranslated(player, NEUTRAL, "The repair would cost {input#amount}", format);
            }
            economy.createAccount(player.getUniqueId());
            i18n.sendTranslated(player, NEUTRAL, "You currently have {input#balance}", economy.format(player.getLocale(), economy.getBalance(player.getUniqueId())));
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
                    if (!entry.getValue().getEnchantments().isEmpty())
                    {
                        if (this.rand.nextInt(100) < this.config.looseEnchantmentsPercentage)
                        {
                            looseEnch = true;
                            for (Enchantment enchantment : entry.getValue().getEnchantments().keySet())
                            {
                                entry.getValue().removeEnchantment(enchantment);
                            }
                        }
                    }
                }
                else
                {
                    itemsBroken = true;
                    amount = item.getQuantity();
                    if (amount == 1)
                    {
                        inventory.inventory.clear(entry.getKey());
                    }
                    else
                    {
                        item.setAmount(amount - 1);
                        repairItem(item);
                    }
                }
            }
            if (itemsBroken)
            {
                i18n.sendTranslated(player, NEGATIVE, "You broke some of your items when repairing!");
                player.playSound(ANVIL_BREAK, player.getLocation().getPosition(), 1, 0);
            }
            if (repairFail)
            {
                i18n.sendTranslated(player, NEGATIVE, "You failed to repair some of your items!");
                player.playSound(BURP,player.getLocation().getPosition(), 1,0);
            }
            if (looseEnch)
            {
                i18n.sendTranslated(player, NEGATIVE, "Oh no! Some of your items lost their magical power.");
                player.playSound(SoundTypes.GHAST_SCREAM, player.getLocation().getPosition(), 1);
            }
            i18n.sendTranslated(player, POSITIVE, "You paid {input#amount} to repair your items!", economy.format(price));
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
        for (Integer level : item.getEnchantments().values())
        {
            enchantmentLevel += level;
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
        repairItem(item, (short)0);
    }

    public static void repairItem(ItemStack item, short durability)
    {
        if (item != null)
        {
            item.setDurability(durability);
        }
    }

    public static void removeHeldItem(Player player)
    {
        PlayerInventory inventory = player.getInventory();
        inventory.clear(inventory.getHeldItemSlot());
    }
}
