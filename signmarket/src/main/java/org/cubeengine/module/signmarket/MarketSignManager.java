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

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NONE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.module.signmarket.data.IMarketSignData.ADMIN_SIGN;
import static org.spongepowered.api.item.inventory.InventoryArchetypes.CHEST;
import static org.spongepowered.api.item.inventory.InventoryArchetypes.DISPENSER;
import static org.spongepowered.api.text.chat.ChatTypes.ACTION_BAR;
import static org.spongepowered.api.text.format.TextColors.DARK_PURPLE;
import static org.spongepowered.api.text.format.TextColors.GOLD;
import static org.spongepowered.api.text.format.TextColors.LIGHT_PURPLE;

import org.cubeengine.libcube.service.command.exception.PermissionDeniedException;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.inventoryguard.InventoryGuardFactory;
import org.cubeengine.module.signmarket.data.IMarketSignData;
import org.cubeengine.module.signmarket.data.ImmutableMarketSignData;
import org.cubeengine.module.signmarket.data.MarketSignData;
import org.cubeengine.module.signmarket.data.SignType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.property.Identifiable;
import org.spongepowered.api.item.inventory.property.InventoryDimension;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.item.inventory.property.SlotPos;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.service.economy.transaction.TransferResult;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextFormat;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class MarketSignManager
{

    private final I18n i18n;
    private final EconomyService es;
    private Signmarket module;
    private InventoryGuardFactory igf;
    private PluginContainer plugin;

    public MarketSignManager(I18n i18n, EconomyService es, Signmarket module, InventoryGuardFactory igf, PluginContainer plugin)
    {
        this.i18n = i18n;
        this.es = es;
        this.module = module;
        this.igf = igf;
        this.plugin = plugin;
    }

    private Map<UUID, ImmutableMarketSignData> previousSign = new HashMap<>(); // Player -> SignData
    private Map<UUID, Location<World>> activeSigns = new HashMap<>(); // Player -> SignLocs
    private Map<UUID, Long> breakingSign = new HashMap<>(); // Player -> currentTimeMillis
    private Map<UUID, Inventory> signInventories = new HashMap<>(); // SignID -> Inventory
    private Map<UUID, Integer> signInventoryStock = new HashMap<>(); // InventoryID -> initialStock

    /**
     * Checks if given MarketSignData is a valid sign
     * @param data the data to check
     * @param player the Player or null
     * @return whether the sign is valid or not
     */
    public boolean isValidSign(IMarketSignData data, Player player)
    {
        if (data.getSignType() == null)
        {
            if (player != null)
            {
                i18n.send(player, NEGATIVE, "No sign-type given!");
            }
            return false;
        }

        if (data.getAmount() == null || data.getAmount() <= 0)
        {
            if (player != null)
                i18n.send(player, NEGATIVE, "Invalid amount!");
            return false;
        }

        if (data.getPrice() == null || data.getPrice() <= 0)
        {
            if (player != null)
                i18n.send(player, NEGATIVE, "Invalid price!");
            return false;
        }

        if (data.getItem() == null)
        {
            if (player != null)
                i18n.send(player, NEGATIVE, "No item given!");
            return false;
        }

        return true;
    }

    /**
     * Drops the contents of given MarketSignData.
     * No Items will drop if this is a AdminSign.
     * The data has to be re-offered to the sign!
     *
     * @param data the MarketSignData
     */
    public MarketSignData dropContents(MarketSignData data, Location<World> at)
    {
        Integer stock = data.getStock();
        if (ADMIN_SIGN.equals(data.getOwner()) || stock == null || stock <= 0)
        {
            return data;
        }

        ItemStack copy = data.getItem().copy();
        copy.setQuantity(copy.getMaxStackQuantity());
        while (stock > copy.getMaxStackQuantity())
        {
            // TODO SpawnTypes.DROPPED_ITEM cause?
            spawn(at, copy);
            stock -= copy.getMaxStackQuantity();
        }
        copy.setQuantity(stock);
        spawn(at, copy);

        data.setStock(0);
        return data;
    }

    private void spawn(Location<World> at, ItemStack copy)
    {
        Entity spawn = at.getExtent().createEntity(EntityTypes.ITEM, at.getPosition());
        spawn.offer(Keys.REPRESENTED_ITEM, copy.createSnapshot());
        at.getExtent().spawnEntity(spawn); // TODO random spawn velocity?
    }

    /**
     * Executes the appropriate action on a sign
     * right-click: Use the sign. Buy or Sell. If Owner: Empty Stock
     * left-click: If Owner: Break + If Item in Hand: Refill Stock
     * shift-right-click: Inspect Sign and Show Additional Information
     * shift-left-click: Open Sign-Inventory: If Owner and Item in Hand: Refill Stock with all Items
     *
     * @param data the MarketSitnData
     * @param player the player
     * @param right true if right-click else false
     */
    public void executeSignAction(MarketSignData data, Location<World> at, Player player, boolean right)
    {
        if (isActive(at, player))
        {
            return;
        }

        boolean isOwner = data.isOwner(player.getUniqueId());

        if (isInEditMode(at))
        {
            i18n.send(player, NEGATIVE, "This sign is being edited right now!");
            return;
        }
        if (!isValidSign(data, null))
        {
            if (!right)
            {
                executeTryBreak(data, player, at);
            }
            else
            {
                i18n.send(ACTION_BAR, player, NEGATIVE, "This marketsign is invalid!");
            }
            return;
        }

        if (this.signInventories.containsKey(data.getID()))
        {
            i18n.send(player, NEGATIVE, "This signs inventory is being edited right now!");
            return;
        }

        if (player.get(Keys.IS_SNEAKING).get())
        {
            if (right)
            {
                executeShowInfo(data, player, at);
                return;
            } // else left
            if (isOwner && player.getItemInHand(HandTypes.MAIN_HAND).isPresent() && data.isItem(player.getItemInHand(HandTypes.MAIN_HAND).get()))
            {
                executeFill(data, player, player.getItemInHand(HandTypes.MAIN_HAND).get(), at, true);
                return;
            }
            executeShowInventory(data, player, isOwner, at);
            return;
        }
        if (right)
        {
            Sponge.getCauseStackManager().pushCause(player);
            executeUse(data, player, at);
            return;
        } // else left
        if (isOwner)
        {
            if (player.getItemInHand(HandTypes.MAIN_HAND).isPresent() && data.isItem(player.getItemInHand(HandTypes.MAIN_HAND).get()))
            {
                executeFill(data, player, player.getItemInHand(HandTypes.MAIN_HAND).get(), at, false);
                return;
            }
        }
        executeTryBreak(data, player, at);
    }

    public boolean tryBreakActive(Player player)
    {
        MarketSignData data = getCurrentData(player);
        Location<World> at = activeSigns.get(player.getUniqueId());
        return executeTryBreak(data, player, at);
    }

    public void modifyItemActive(Player player, ItemStack item)
    {
        MarketSignData data = getCurrentData(player);
        if (!data.isAdminOwner() && data.getStock() != null && data.getStock() != 0)
        {
            i18n.send(player, NEGATIVE, "You have to take all items out of the market sign to be able to change the item in it!");
            return;
        }
        Location<World> loc = activeSigns.get(player.getUniqueId());
        data.setItem(item, true);
        loc.offer(data);
        updateSignText(data, loc);
        i18n.send(player, POSITIVE, "Item in sign updated!");
    }

    public boolean executeTryBreak(MarketSignData data, Player player, Location<World> at)
    {
        if (data.isOwner(player.getUniqueId()))
        {
            if (!player.hasPermission(module.perms().EDIT_PLAYER_SELF.getId())) {
                i18n.send(player, NEGATIVE, "You are not allowed to break your own market signs!");
                return false;
            }
        }
        else if (data.isAdminOwner())
        {
            if (!player.hasPermission(module.perms().EDIT_ADMIN.getId()))
            {
                i18n.send(player, NEGATIVE, "You are not allowed to break admin market signs!");
                return false;
            }
        }
        else if (!player.hasPermission(module.perms().EDIT_PLAYER_OTHER.getId()))
        {
            i18n.send(player, NEGATIVE, "You are not allowed to break others market signs!");
            return false;
        }

        boolean isDoubleClick = breakingSign.containsKey(player.getUniqueId()) && System.currentTimeMillis() - breakingSign.get(player.getUniqueId()) <= 500;

        if (player.gameMode().get() != GameModes.CREATIVE)
        {
            // When valid ; not Admin ; has stock
            if (isValidSign(data, null) && !data.getOwner().equals(ADMIN_SIGN) && data.getStock() > 0)
            {
                if (!isDoubleClick)
                {
                    breakingSign.put(player.getUniqueId(), System.currentTimeMillis());
                    i18n.send(player, NEUTRAL, "Double click to empty and break the sign!");
                    return false;
                }
                // empty the sign first
                Sponge.getCauseStackManager().pushCause(player);
                takeItem(data, at, player); // TODO spawnType DISPENSE?
                if (data.getStock() == 0)
                {
                    breakingSign.remove(player.getUniqueId()); // double click again to actually break the sign
                }
                return false;
            }
        }

        if (player.getItemInHand(HandTypes.MAIN_HAND).isPresent())
        {
            i18n.send(ACTION_BAR, player, NEGATIVE, "Use your bare hands to break the sign!");
            return false;
        }

        // Sign is Empty or Create -> Now Break it (maybe)
        if (isDoubleClick) // 0.5 sec
        {
            Sponge.getCauseStackManager().pushCause(player);
            if (data.getStock() != null && data.getStock() == 1337)  //pssst i am not here
            {
                Entity lightning = at.getExtent().createEntity(EntityTypes.LIGHTNING, at.getPosition());
                at.getExtent().spawnEntity(lightning);
            }

            dropContents(data, at);
            at.remove(MarketSignData.class);
            at.remove(SignData.class);
            at.setBlock(BlockTypes.AIR.getDefaultState()); // TODO break particles + sound?
            if (player.gameMode().get() != GameModes.CREATIVE)
            {
                spawn(at, ItemStack.builder().itemType(ItemTypes.SIGN).quantity(1).build());
            }
            i18n.send(player, POSITIVE, "MarketSign destroyed!");
            return true;
        }
        breakingSign.put(player.getUniqueId(), System.currentTimeMillis());
        i18n.send(ACTION_BAR, player, NEUTRAL, "Double click to break the sign!");
        return false;
    }

    private void takeItem(MarketSignData data, Location<World> loc, Player player)
    {
        int amount = Math.min(data.getStock(), data.getItem().getMaxStackQuantity());
        if (amount == 0)
        {
            i18n.send(ACTION_BAR, player, NEGATIVE, "There are no more items stored in the sign!");
            return;
        }
        data.setStock(data.getStock() - amount);
        ItemStack copy = data.getItem().copy();
        copy.setQuantity(amount);
        spawn(loc, copy);
    }

    private void executeUse(MarketSignData data, Player player, Location<World> loc)
    {
        if (isInEditMode(loc))
        {
            i18n.send(ACTION_BAR, player, NEGATIVE, "This sign is being edited right now!");
            return;
        }
        if (!isValidSign(data, null))
        {
            return;
        }

        if (data.isOwner(player.getUniqueId()))
        {
            takeItem(data, loc, player);
            return;
        }

        if (!es.hasAccount(player.getUniqueId()))
        {
            i18n.send(player, NEGATIVE, "You do not have an account.");
            return;
        }

        UniqueAccount account = es.getOrCreateAccount(player.getUniqueId()).get();

        if (data.getSignType() == SignType.BUY)
        {
            executeBuy(data, player, account);
            return;
        }
        executeSell(data, player, account);
    }

    private void executeSell(MarketSignData data, Player player, UniqueAccount seller)
    {
        if (!player.hasPermission(module.perms().INTERACT_SELL.getId()))
        {
            i18n.send(ACTION_BAR, player, NEGATIVE, "You are not allowed to use sell market signs!");
            return;
        }

        if (data.isSatisfied())
        {
            i18n.send(ACTION_BAR, player, NEGATIVE, "This market sign is {text:satisfied:color=DARK_RED}! You can no longer sell items to it.");
            return;
        }

        if (data.getStock() != null && data.getMax() != -1 && data.getMax() < data.getStock() + data.getAmount())
        {
            i18n.send(ACTION_BAR, player, NEGATIVE, "This market sign is {text:full:color=DARK_RED}! You can no longer sell items to it.");
            return;
        }

        ItemStack copy = data.getItem().copy();
        copy.setQuantity(1);
        int itemsInInventory = player.getInventory().query(QueryOperationTypes.ITEM_STACK_IGNORE_QUANTITY.of(copy)).totalItems(); // TODO ignore repaircost?
        if (data.getAmount() > itemsInInventory)
        {
            i18n.send(ACTION_BAR, player, NEGATIVE, "You do not have enough items to sell!");
            return;
        }

        if (!data.isAdminOwner())
        {
            UniqueAccount owner = es.getOrCreateAccount(data.getOwner()).get();
            TransferResult result = owner.transfer(seller, es.getDefaultCurrency(), getPrice(data), Cause.of(EventContext.empty(), seller));
            if (result.getResult() != ResultType.SUCCESS)
            {
                i18n.send(ACTION_BAR, player, NEGATIVE, "The owner cannot afford the money to acquire your items!");
                return;
            }
        }
        else
        {
            TransactionResult result = seller.deposit(es.getDefaultCurrency(), getPrice(data), Cause.of(EventContext.empty(), seller));
            if (result.getResult() != ResultType.SUCCESS)
            {
                throw new IllegalStateException("Could not deposit");
            }
        }
        player.getInventory().query(QueryOperationTypes.ITEM_STACK_IGNORE_QUANTITY.of(copy)).poll(data.getAmount());
        if (data.getStock() != null)
        {
            data.setStock(data.getStock() + data.getAmount());
        }

        i18n.send(player, POSITIVE, "You sold {amount}x {input#item} for {txt#price}.", data.getAmount(),
                            data.getItem().getTranslation(), formatPrice(data));

    }

    private void executeBuy(MarketSignData data, Player player, UniqueAccount account)
    {
        if (!player.hasPermission(module.perms().INTERACT_BUY.getId()))
        {
            i18n.send(ACTION_BAR, player, NEGATIVE, "You are not allowed to use buy market signs!");
            return;
        }

        if (data.getStock() != null && data.getStock() < data.getAmount())
        {
            i18n.send(ACTION_BAR, player, NEGATIVE, "This market sign is {text:Sold Out:color=DARK_RED}!");
            return;
        }

        if (account.withdraw(es.getDefaultCurrency(), getPrice(data), Cause.of(EventContext.empty(), player)).getResult() != ResultType.SUCCESS)
        {
            i18n.send(ACTION_BAR, player, NEGATIVE, "You can't afford these items!");
            return;
        }

        ItemStack copy = data.getItem().copy();
        copy.setQuantity(data.getAmount());
        player.getInventory().offer(copy);
        int remaining = copy.getQuantity();
        if (remaining != 0)
        {
            player.getInventory().query(QueryOperationTypes.ITEM_STACK_IGNORE_QUANTITY.of(copy)).poll(data.getAmount() - remaining);
            i18n.send(ACTION_BAR, player, NEGATIVE, "You don't have enough space in your inventory for these items!");
            return;
        }


        if (!data.isAdminOwner())
        {
            es.getOrCreateAccount(data.getOwner()).get().deposit(es.getDefaultCurrency(), getPrice(data), Cause.of(EventContext.empty(), player));
        }

        if (data.getStock() != null)
        {
            data.setStock(data.getStock() - data.getAmount());
        }

        i18n.send(player, POSITIVE, "You bought {amount}x {input#item} for {txt#price}.",
                            data.getAmount(), data.getItem().getTranslation(), formatPrice(data));
    }

    private Text formatPrice(MarketSignData data)
    {
        if (data.getPrice() == null)
        {
            return Text.of("??");
        }
        return es.getDefaultCurrency().format(getPrice(data));
    }

    private BigDecimal getPrice(MarketSignData data)
    {
        return new BigDecimal(data.getPrice());
    }

    private void executeShowInventory(MarketSignData data, Player player, boolean isOwner, Location<World> loc)
    {
        if (isInEditMode(loc))
        {
            return;
        }
        if (isOwner || (!data.isAdminOwner() && player.hasPermission(module.perms().EDIT_PLAYER_OTHER.getId())))
        {
            Inventory inventory = signInventories.get(data.getID());
            ItemStack item = data.getItem();
            if (inventory != null)
            {
                i18n.send(ACTION_BAR, player, NEGATIVE, "This signs inventory is being edited right now!");
                // TODO dont allow this return;
            }

            Integer size = data.getSize();
            if (size == -1)
            {
                size = 6;
            }
            Inventory inv = Inventory.builder()
                    .of(CHEST)
                    .property(InventoryTitle.PROPERTY_NAME, InventoryTitle.of(Text.of(getOwnerName(data))))
                    .property(InventoryDimension.PROPERTY_NAME, new InventoryDimension(9, size))
                    .property("identifiable", new Identifiable()).build(plugin);
            signInventories.put(data.getID(), inv);
            updateSignText(data, loc);
            UUID key = UUID.randomUUID();
            signInventoryStock.put(/*TODO getProperty is always empty MinecraftInventoryAdapter inv.getProperty(IdentifiableProperty.class,
            "IdentifiableProperty").get().getValue()*/ key, Math.min(size * 9 * item
                    .getMaxStackQuantity(), data.getStock()));

            // TODO would be nice to just offer a 64+ item stack and split it up in inventory impl.
            // Offer stock to signinventory -->
            Integer stockToAdd = data.getStock();
            int maxStackSize = item.getMaxStackQuantity();
            while (stockToAdd > 0)
            {
                ItemStack addAll = item.copy();
                addAll.setQuantity(Math.min(stockToAdd, maxStackSize));
                stockToAdd -= addAll.getQuantity();
                inv.offer(addAll);
            }
            // --<

            ItemStack copy = item.copy();
            Runnable onClose = () -> {
                signInventories.remove(data.getID());
                if (data.getStock() != null)
                {
                    int newPageStock = inv.query(QueryOperationTypes.ITEM_STACK_IGNORE_QUANTITY.of(copy)).totalItems();
                    Integer totalStock = data.getStock();
                    Integer oldPageStock = signInventoryStock.get(key
                            /*TODO getProperty is always empty MinecraftInventoryAdapter
                            inv.getProperty(IdentifiableProperty.class, "IdentifiableProperty").get().getValue()*/);
                    oldPageStock = oldPageStock == null ? 0 : oldPageStock;
                    data.setStock(totalStock - (oldPageStock - newPageStock));

                    loc.offer(data);
                    updateSignText(data, loc);
                }
            };

            igf.prepareInv(inv, player.getUniqueId()).blockPutInAll().blockTakeOutAll().onClose(onClose);
            if (data.getSignType() == SignType.BUY)
            {
                igf.notBlockPutIn(item).notBlockTakeOut(item);
            }
            else
            {
                igf.notBlockTakeOut(item);
                // TODO permission to allow putting items into sell sign
            }
            igf.submitInventory(Signmarket.class, true);
            return;
        }
        if (player.hasPermission(module.perms().INTERACT_INVENTORY.getId()) || data.isOwner(player.getUniqueId()))
        {
            String name = getOwnerName(data);
            Inventory inventory = Inventory.builder().of(DISPENSER).property(InventoryTitle.PROPERTY_NAME, InventoryTitle.of(Text.of(name))).build(plugin);
            inventory.query(QueryOperationTypes.INVENTORY_PROPERTY.of(SlotPos.of(1,1))).set(data.getItem().copy()); // middle of dispenser
            igf.prepareInv(inventory, player.getUniqueId()).blockPutInAll().blockTakeOutAll().submitInventory(Signmarket.class, true);
            return;
        }
        i18n.send(ACTION_BAR, player, NEGATIVE, "You are not allowed to see the market signs inventories");
    }

    private void executeFill(MarketSignData data, Player player, ItemStack item, Location<World> loc, boolean all)
    {
        //all
        if ((data.isAdminOwner() || !data.isOwner(player.getUniqueId()))
            && !player.hasPermission(module.perms().EDIT_PLAYER_OTHER.getId()) ||
            data.getSignType() == SignType.SELL)
        {
            return;
        }
        if (isInEditMode(loc))
        {
            // TODO viewers of inventory?
            i18n.send(ACTION_BAR, player, NEGATIVE, "This signs inventory is being edited right now!");
            return;
        }
        ItemStack copy = item.copy();

        int quantity = item.getQuantity();
        if (all)
        {
            Inventory slots = player.getInventory().query(QueryOperationTypes.ITEM_STACK_IGNORE_QUANTITY.of(copy));
            quantity = slots.totalItems();
        }

        if (data.getMax() != -1)
        {
            int max = data.getMax() - data.getStock();
            if (quantity > max)
            {
                quantity = max;
                i18n.send(ACTION_BAR, player, NEGATIVE, "The market sign cannot hold all your items!");
            }
            if (max <= 0)
            {
                i18n.send(ACTION_BAR, player, NEGATIVE, "The market sign inventory is full!");
                return;
            }
        }
        data.setStock(data.getStock() + quantity);
        if (all)
        {
            player.getInventory().query(QueryOperationTypes.ITEM_STACK_IGNORE_QUANTITY.of(copy)).poll(quantity);
            i18n.send(ACTION_BAR, player, POSITIVE, "Added all ({amount}) {name#material} to the stock!", quantity,
                                item.getTranslation().get(player.getLocale()));
        }
        else
        {
            item.setQuantity(item.getQuantity() - quantity);
            player.setItemInHand(HandTypes.MAIN_HAND, item.getQuantity() == 0 ? null : item);
            i18n.send(ACTION_BAR, player, POSITIVE, "Added {amount}x {name#material} to the stock!", quantity,
                                item.getTranslation().get(player.getLocale()));
        }
    }

    public void executeShowInfo(MarketSignData data, Player player, Location<World> loc)
    {
        boolean inEditMode = isInEditMode(loc);
        if (inEditMode)
        {
            player.sendMessage(Text.of());
            i18n.send(player, NONE, "-- {text:Sign Market:color=PURPLE} - {text:Edit Mode:color=PURPLE} --");
        }
        else
        {
            player.sendMessage(Text.of());
            i18n.send(player, NONE, "--------- {text:Sign Market} ---------");
        }

        if (data.getSignType() == null)
        {
            player.sendMessage(Text.of(DARK_PURPLE, "new Sign"));
            return;
        }

        int amount = data.getAmount() == null ? 0 : data.getAmount();
        Text amountText = Text.of(String.valueOf(amount));
        if (amount == 0)
        {
            amountText = Text.of(TextColors.RED, "??");
        }
        if (data.getSignType() == SignType.BUY)
        {
            i18n.send(player, NONE, "{text:Buy:color=DARK_BLUE}: {txt#amount} for {txt#price} from {user#owner}",
                    amountText, formatPrice(data), getOwnerName(data));
        }
        else
        {
            i18n.send(player, NONE, "{text:Sell:color=DARK_BLUE}: {txt#amount} for {txt#price} to {user#owner}",
                    amountText, formatPrice(data), getOwnerName(data));
        }

        if (data.getItem() == null)
        {
            if (inEditMode)
            {
                i18n.send(player, TextFormat.of(TextColors.DARK_PURPLE), "No Item");
            }
            else
            {
                i18n.send(player, TextFormat.of(TextColors.DARK_RED), "No Item");
            }
        }
        else
        {
            Text itemText = getItemText(data, inEditMode).toBuilder().color(GOLD).onHover(TextActions.showItem(data.getItem().createSnapshot())).build();
            player.sendMessage(itemText);
        }

        if (data.getStock() != null)
        {
            if (data.getDemand() == null && data.getSize() != null && data.getSize() == -1)
            {
                i18n.send(player, NEUTRAL, "In stock: {amount}/{text:Infinite}", data.getStock());
            }
            else if (data.getItem() == null || data.getAmount() == null || data.getAmount() == 0)
            {
                i18n.send(player, NEUTRAL, "In stock: {amount}/{text:Unknown:color=RED}", data.getStock());
            }
            else
            {
                i18n.send(player, NEUTRAL, "In stock: {amount}/{amount#max}", data.getStock(), data.getMax());
            }
        }
    }

    public boolean isInEditMode(Location<World> loc)
    {
        return activeSigns.values().contains(loc);
    }

    public boolean isViewed(MarketSignData data)
    {
        return signInventories.containsKey(data.getID());
    }

    public void updateSignText(MarketSignData data, Location<World> loc)
    {
        SignData sign = loc.get(SignData.class).orElse(null);
        if (sign == null)
        {
            return;
        }

        boolean isValid = isValidSign(data, null);
        boolean inEditMode = isInEditMode(loc);
        boolean isAdmin = data.isAdminOwner();

        // First Line: SignType
        TextColor color = inEditMode ? DARK_PURPLE : !isValid ? TextColors.DARK_RED : isAdmin ? TextColors.BLUE : TextColors.DARK_BLUE;
        if (this.signInventories.containsKey(data.getID()))
        {
            color = LIGHT_PURPLE;
        }

        String raw;
        if (data.getSignType() != null)
        {
            raw = data.getSignType().getName(); // Buy or Sell
        }
        else if (inEditMode) // Edit
        {
            raw = "Edit";
        }
        else
        {
            raw = "Invalid"; // Invalid
        }
        if (isAdmin) // Append Admin?
        {
            raw = "Admin-" + raw;
        }
        else if (!inEditMode && isValid) // !isAdmin
        {
            if (data.getSignType() == SignType.BUY && data.getStock() == 0)
            {
                color = TextColors.RED;
                raw = "Sold out"; // Replace with Sold out
            }
            else if (data.getSignType() == SignType.SELL && data.isSatisfied())
            {
                color = TextColors.RED;
                raw = "Satisfied"; // Replace with Satisfied
            }
        }

        Text line1 = Text.of(color, TextStyles.BOLD, raw);

        // Second Line: Item
        Text line2 = getItemText(data, inEditMode);

        // Third Line: Amount
        color = inEditMode ? DARK_PURPLE : TextColors.DARK_RED;
        raw = "No amount";
        if (data.getAmount() != null)
        {
            raw = data.getAmount().toString();
            color = TextColors.BLACK;
        }
        String raw2 = "";
        TextColor color2 = TextColors.RED;
        if (data.getStock() != null && data.getAmount() != null)
        {
            if (data.getSignType() == SignType.BUY)
            {
                raw2 = " x" + data.getStock();
                if (data.getStock() >= data.getAmount())
                {
                    color2 = TextColors.DARK_BLUE;
                }
            }
            else // Sell
            {
                raw2 = " x?";
                color2 = TextColors.AQUA;
                if (data.getDemand() != null)
                {
                    raw2 = " x" + (data.getDemand() - data.getStock());
                    if (data.getStock() >= data.getDemand())
                    {
                        color2 = TextColors.DARK_RED;
                    }
                }
            }
        }

        Text line3 = Text.of(color, raw, color2, raw2);

        // Fourth Line: Price
        color = inEditMode ? DARK_PURPLE : TextColors.DARK_RED;
        Text line4 = Text.of(color, "No Price");

        if (data.getPrice() != null && data.getPrice() != 0)
        {
            line4 = formatPrice(data)
                        .toBuilder().color(TextColors.BLACK).build();
        }

        sign.setElements(Arrays.asList(line1, line2, line3, line4));
        loc.offer(sign);
    }

    private Text getItemText(MarketSignData data, boolean inEditMode)
    {
        TextColor color;
        String raw;
        ItemStack item = data.getItem();
        color = TextColors.DARK_RED;
        raw = "No Item";
        if (item != null)
        {
            color = TextColors.BLACK;
            raw = item.getTranslation().get();

            if (item.get(Keys.DISPLAY_NAME).isPresent())
            {
                color = TextColors.DARK_AQUA;
                raw = item.get(Keys.DISPLAY_NAME).get().toPlain();
            }

            if (item.get(Keys.ITEM_LORE).isPresent() ||
                (item.get(Keys.ITEM_ENCHANTMENTS).isPresent() && item.get(Keys.ITEM_ENCHANTMENTS).get().size() > 0))
            {
                color = TextColors.DARK_AQUA;
            }
        }

        return Text.of(inEditMode ? DARK_PURPLE : color, raw);
    }

    private String getOwnerName(MarketSignData data)
    {
        if (data.getOwner() == null)
        {
            return "???";
        }
        if (data.isAdminOwner())
        {
            return "Server";
        }

        UserStorageService uss = Sponge.getServiceManager().provide(UserStorageService.class).get();

        return uss.get(data.getOwner()).map(User::getName).orElse("???");
    }

    public boolean isActive(Location<World> loc, Player player)
    {
        return loc.equals(activeSigns.get(player.getUniqueId()));
    }

    public void setSign(Location<World> loc, Player player)
    {
        if (activeSigns.values().contains(loc))
        {
            i18n.send(ACTION_BAR, player, NEGATIVE, "Someone else is editing this sign!");
            return;
        }

        MarketSignData data = loc.get(MarketSignData.class).get();

        if (data.isAdminOwner())
        {
            if (!player.hasPermission(module.perms().EDIT_ADMIN.getId()))
            {
                throw new PermissionDeniedException(module.perms().EDIT_ADMIN);
            }
        }
        else
        {
            if (data.isOwner(player.getUniqueId()))
            {
                if (!player.hasPermission(module.perms().EDIT_PLAYER_SELF.getId()))
                {
                    throw new PermissionDeniedException(module.perms().EDIT_PLAYER_SELF);
                }
            }
            else
            {
                if (!player.hasPermission(module.perms().EDIT_PLAYER_OTHER.getId()))
                {
                    throw new PermissionDeniedException(module.perms().EDIT_PLAYER_OTHER);
                }
            }
        }

        Location<World> last = activeSigns.put(player.getUniqueId(), loc);
        if (last != null)
        {
            ImmutableMarketSignData prevData = last.get(MarketSignData.class).map(MarketSignData::asImmutable).orElse(previousSign.get(player.getUniqueId()));
            previousSign.put(player.getUniqueId(), prevData);
            updateSignText(prevData.asMutable(), last);
        }
        updateSignText(loc.get(MarketSignData.class).get(), loc);
        i18n.send(ACTION_BAR, player, POSITIVE, "Changed active sign!");
    }

    public void exitEditMode(Player player)
    {
        Location<World> loc = activeSigns.remove(player.getUniqueId());
        if (loc != null)
        {
            Optional<MarketSignData> data = loc.get(MarketSignData.class);
            if (data.isPresent())
            {
                updateSignText(data.get(), loc);
            }
        }
    }

    public Location<World> updateData(MarketSignData data, Player player)
    {
        Location<World> loc = activeSigns.get(player.getUniqueId());
        if (loc != null)
        {
            loc.offer(data);
            updateSignText(data, loc);
            previousSign.put(player.getUniqueId(), data.asImmutable());
        }
        return loc;
    }

    public MarketSignData getCurrentData(Player player)
    {
        Location<World> loc = activeSigns.get(player.getUniqueId());
        if (loc != null)
        {
            return loc.get(MarketSignData.class).orElse(null);
        }
        return null;
    }

    public ImmutableMarketSignData getPreviousData(Player player)
    {
        return previousSign.get(player.getUniqueId());
    }
}
