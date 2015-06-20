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
package de.cubeisland.engine.module.vaultlink.service;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import de.cubeisland.engine.service.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.Bukkit;

public class VaultEconomyService implements Economy
{
    private AtomicReference<net.milkbowl.vault.economy.Economy> vaultEconomy;

    public VaultEconomyService(AtomicReference<net.milkbowl.vault.economy.Economy> vaultEconomy)
    {
        this.vaultEconomy = vaultEconomy;
    }

    @Override
    public boolean isEnabled()
    {
        return vaultEconomy.get().isEnabled();
    }

    @Override
    public String getName()
    {
        return vaultEconomy.get().getName();
    }

    @Override
    public boolean hasBankSupport()
    {
        return vaultEconomy.get().hasBankSupport();
    }

    @Override
    public int fractionalDigits()
    {
        return vaultEconomy.get().fractionalDigits();
    }

    @Override
    public long fractionalDigitsFactor()
    {
        return (long)Math.pow(10, this.fractionalDigits());
    }

    @Override
    public double convertLongToDouble(long value)
    {
        return (double)value / this.fractionalDigitsFactor();
    }

    @Override
    public String format(double amount)
    {
        return vaultEconomy.get().format(amount);
    }

    @Override
    public String format(Locale locale, double amount)
    {
        return this.format(amount);
    }

    @Override
    public String currencyNamePlural()
    {
        return vaultEconomy.get().currencyNamePlural();
    }

    @Override
    public String currencyName()
    {
        return vaultEconomy.get().currencyNameSingular();
    }

    @Override
    public boolean hasAccount(UUID player)
    {
        return vaultEconomy.get().hasAccount(Bukkit.getOfflinePlayer(player));
    }

    @Override
    public boolean createAccount(UUID player)
    {
        return vaultEconomy.get().createPlayerAccount(Bukkit.getOfflinePlayer(player));
    }

    @Override
    public boolean deleteAccount(UUID player)
    {
        return false;
    }

    @Override
    public double getBalance(UUID player)
    {
        return vaultEconomy.get().getBalance(Bukkit.getOfflinePlayer(player));
    }

    @Override
    public boolean has(UUID player, double amount)
    {
        return vaultEconomy.get().has(Bukkit.getOfflinePlayer(player), amount);
    }

    @Override
    public boolean withdraw(UUID player, double amount)
    {
        return vaultEconomy.get().withdrawPlayer(Bukkit.getOfflinePlayer(player), amount).type == ResponseType.SUCCESS;
    }

    @Override
    public boolean deposit(UUID player, double amount)
    {
        return vaultEconomy.get().depositPlayer(Bukkit.getOfflinePlayer(player), amount).type == ResponseType.SUCCESS;
    }

    @Override
    public boolean bankExists(String name)
    {
        return vaultEconomy.get().getBanks().contains(name);
    }

    @Override
    public boolean createBank(String name, UUID owner)
    {
        return vaultEconomy.get().createBank(name, Bukkit.getOfflinePlayer(owner)).type == ResponseType.SUCCESS;
    }

    @Override
    public boolean deleteBank(String name)
    {
        return vaultEconomy.get().deleteBank(name).type == ResponseType.SUCCESS;
    }

    @Override
    public double getBankBalance(String name)
    {
        EconomyResponse response = vaultEconomy.get().bankBalance(name);
        if (response.type == ResponseType.SUCCESS)
        {
            return response.amount;
        }
        return 0;
    }

    @Override
    public boolean bankHas(String name, double amount)
    {
        return vaultEconomy.get().bankHas(name, amount).type == ResponseType.SUCCESS;
    }

    @Override
    public boolean bankWithdraw(String name, double amount)
    {
        return vaultEconomy.get().bankWithdraw(name, amount).type == ResponseType.SUCCESS;
    }

    @Override
    public boolean bankDeposit(String name, double amount)
    {
        return vaultEconomy.get().bankDeposit(name, amount).type == ResponseType.SUCCESS;
    }

    @Override
    public boolean isBankOwner(String name, UUID player)
    {
        return vaultEconomy.get().isBankOwner(name, Bukkit.getPlayer(player)).type == ResponseType.SUCCESS;
    }

    @Override
    public boolean isBankMember(String name, UUID player)
    {
        return vaultEconomy.get().isBankMember(name, Bukkit.getPlayer(player)).type == ResponseType.SUCCESS;
    }

    @Override
    public List<String> getBanks()
    {
        return vaultEconomy.get().getBanks();
    }

    @Override
    public Double parse(String price)
    {
       return this.parseFor(price, Locale.getDefault());
    }

    @Override
    public Double parseFor(String price, Locale locale)
    {
        try
        {
            return NumberFormat.getInstance(locale).parse(price).doubleValue();
        }
        catch (ParseException e)
        {
            return null;
        }
    }
}
