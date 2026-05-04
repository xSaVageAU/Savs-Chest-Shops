package savage.chestshops.interaction;

import eu.pb4.common.economy.api.EconomyAccount;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import savage.chestshops.economy.EconomyWrapper;
import savage.chestshops.model.ChestShop;
import savage.chestshops.util.InventoryHelper;
import savage.chestshops.util.SignUtil;

import java.math.BigInteger;

/**
 * Executes the actual trade logic.
 */
public class TradeProcessor {

    public static void processTrade(ServerPlayer player, ChestShop shop, int amount) {
        if (amount <= 0) return;

        ServerLevel world = (ServerLevel) player.level();
        BigInteger totalPrice = shop.price().multiply(BigInteger.valueOf(amount));
        
        if (shop.isBuying()) {
            // Shop buys from Player (Player sells)
            handlePlayerSelling(player, shop, world, amount, totalPrice);
        } else {
            // Shop sells to Player (Player buys)
            handlePlayerBuying(player, shop, world, amount, totalPrice);
        }

        // Update sign
        BlockPos signPos = SignUtil.findSignForChest(world, shop.location().pos());
        if (signPos != null) {
            SignUtil.updateSign(world, signPos, shop);
        }
    }

    private static void handlePlayerBuying(ServerPlayer player, ChestShop shop, ServerLevel world, int amount, BigInteger totalCost) {
        EconomyAccount playerAccount = EconomyWrapper.getPrimaryAccount(player);
        if (playerAccount == null) {
            player.sendSystemMessage(Component.literal("§cAccount Error: Could not find your " + savage.chestshops.config.ModConfig.getConfig().economyProvider + " account!"));
            return;
        }

        if (!EconomyWrapper.canAfford(playerAccount, totalCost)) {
            player.sendSystemMessage(Component.literal("§cYou cannot afford this! Total cost: " + totalCost));
            return;
        }

        if (!shop.isAdmin()) {
            BlockEntity be = world.getBlockEntity(shop.location().pos());
            if (!(be instanceof Container container)) return;

            if (InventoryHelper.countItems(container, shop.item()) < amount) {
                player.sendSystemMessage(Component.literal("§cShop is out of stock!"));
                return;
            }

            // Transfer money to owner
            EconomyAccount ownerAccount = EconomyWrapper.getPrimaryAccount(world.getServer(), shop.ownerId());
            if (EconomyWrapper.transfer(playerAccount, ownerAccount, totalCost)) {
                // Move items
                InventoryHelper.removeItems(container, shop.item(), amount);
                var stack = shop.item().copy();
                stack.setCount(amount);
                InventoryHelper.addItemsToPlayer(player, stack);
                player.sendSystemMessage(Component.literal("§aSuccessfully bought " + amount + "x items!"));
            } else {
                player.sendSystemMessage(Component.literal("§cTransaction failed!"));
            }
        } else {
            // Admin Shop
            if (playerAccount.decreaseBalance(totalCost).isSuccessful()) {
                var stack = shop.item().copy();
                stack.setCount(amount);
                InventoryHelper.addItemsToPlayer(player, stack);
                player.sendSystemMessage(Component.literal("§aSuccessfully bought " + amount + "x items from Admin Shop!"));
            }
        }
    }

    private static void handlePlayerSelling(ServerPlayer player, ChestShop shop, ServerLevel world, int amount, BigInteger totalPayout) {
        if (InventoryHelper.countItems(player.getInventory(), shop.item()) < amount) {
            player.sendSystemMessage(Component.literal("§cYou don't have enough items to sell!"));
            return;
        }

        EconomyAccount playerAccount = EconomyWrapper.getPrimaryAccount(player);
        if (playerAccount == null) {
            player.sendSystemMessage(Component.literal("§cAccount Error: Could not find your " + savage.chestshops.config.ModConfig.getConfig().economyProvider + " account!"));
            return;
        }

        if (!shop.isAdmin()) {
            BlockEntity be = world.getBlockEntity(shop.location().pos());
            if (!(be instanceof Container container)) return;

            EconomyAccount ownerAccount = EconomyWrapper.getPrimaryAccount(world.getServer(), shop.ownerId());
            if (!EconomyWrapper.canAfford(ownerAccount, totalPayout)) {
                player.sendSystemMessage(Component.literal("§cShop owner cannot afford this!"));
                return;
            }

            if (EconomyWrapper.transfer(ownerAccount, playerAccount, totalPayout)) {
                // Move items
                InventoryHelper.removeItems(player.getInventory(), shop.item(), amount);
                var stack = shop.item().copy();
                stack.setCount(amount);
                if (!InventoryHelper.addItems(container, stack)) {
                    // Rollback if chest full
                    InventoryHelper.addItemsToPlayer(player, stack);
                    EconomyWrapper.transfer(playerAccount, ownerAccount, totalPayout);
                    player.sendSystemMessage(Component.literal("§cShop chest is full!"));
                } else {
                    player.sendSystemMessage(Component.literal("§aSuccessfully sold " + amount + "x items!"));
                }
            } else {
                player.sendSystemMessage(Component.literal("§cTransaction Failed: The bank blocked the transfer."));
            }
        } else {
            // Admin Shop
            InventoryHelper.removeItems(player.getInventory(), shop.item(), amount);
            playerAccount.increaseBalance(totalPayout);
            player.sendSystemMessage(Component.literal("§aSuccessfully sold " + amount + "x items to Admin Shop!"));
        }
    }
}
