package savage.chestshops.economy;

import eu.pb4.common.economy.api.CommonEconomy;
import eu.pb4.common.economy.api.EconomyAccount;
import eu.pb4.common.economy.api.EconomyTransaction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import savage.chestshops.SavsChestShops;

import java.math.BigInteger;
import java.util.Collection;
import java.util.UUID;

/**
 * Wrapper for the Common Economy API.
 */
public class EconomyWrapper {

    /**
     * Gets the first available account for a player.
     */
    public static EconomyAccount getPrimaryAccount(ServerPlayer player) {
        return getPrimaryAccount(player.level().getServer(), player.getUUID());
    }

    /**
     * Gets the primary account for an offline player (by UUID).
     */
    public static EconomyAccount getPrimaryAccount(MinecraftServer server, UUID uuid) {
        var config = savage.chestshops.config.ModConfig.getConfig();
        var profile = new com.mojang.authlib.GameProfile(uuid, "");

        // 1. Get the configured provider
        var provider = CommonEconomy.getProvider(config.economyProvider);
        if (provider == null) {
            SavsChestShops.LOGGER.error("Economy Provider not found: {}", config.economyProvider);
            return null;
        }

        // 2. Get the configured currency
        var currency = provider.getCurrency(server, config.currencyId);
        if (currency == null) {
            SavsChestShops.LOGGER.error("Currency not found: {} for provider {}", config.currencyId, config.economyProvider);
            return null;
        }

        // 3. Get the default account for this player/currency
        var accountId = provider.defaultAccount(server, profile, currency);
        if (accountId == null) {
            SavsChestShops.LOGGER.error("Default account not found for player {} in economy {}", uuid, config.economyProvider);
            return null;
        }

        // 4. Return the specific account
        return provider.getAccount(server, profile, accountId);
    }

    /**
     * Checks if an account has enough balance.
     */
    public static boolean canAfford(EconomyAccount account, BigInteger amount) {
        if (account == null) return false;
        return account.canDecreaseBalance(amount).isSuccessful();
    }

    /**
     * Transfers money between two accounts.
     */
    public static boolean transfer(EconomyAccount from, EconomyAccount to, BigInteger amount) {
        if (from == null || to == null) return false;

        EconomyTransaction withdraw = from.decreaseBalance(amount);
        if (withdraw.isSuccessful()) {
            EconomyTransaction deposit = to.increaseBalance(amount);
            if (deposit.isSuccessful()) {
                return true;
            } else {
                // Rollback withdrawal if deposit fails
                from.increaseBalance(amount);
            }
        }
        return false;
    }

    /**
     * Formats a balance for display using the configured currency.
     */
    public static String format(BigInteger amount) {
        var config = savage.chestshops.config.ModConfig.getConfig();
        var server = SavsChestShops.getServer();
        if (server == null) return amount.toString();

        var provider = CommonEconomy.getProvider(config.economyProvider);
        if (provider == null) return amount.toString();

        var currency = provider.getCurrency(server, config.currencyId);
        if (currency == null) return amount.toString();

        return currency.formatValueComponent(amount, false).getString();
    }

    /**
     * Formats a balance for display using an account's currency.
     */
    public static String format(EconomyAccount account, BigInteger amount) {
        if (account == null) return format(amount);
        return account.currency().formatValueComponent(amount, false).getString();
    }
}
