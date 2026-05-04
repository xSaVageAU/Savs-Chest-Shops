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
            player.sendSystemMessage(Component.literal("Account Error: Could not find your " + savage.chestshops.config.ModConfig.getConfig().economyProvider + " account!").withStyle(net.minecraft.ChatFormatting.RED));
            return;
        }

        if (!EconomyWrapper.canAfford(playerAccount, totalCost)) {
            player.sendSystemMessage(Component.literal("You cannot afford this! Total cost: " + totalCost).withStyle(net.minecraft.ChatFormatting.RED));
            return;
        }

        if (!shop.isAdmin()) {
            BlockEntity be = world.getBlockEntity(shop.location().pos());
            if (!(be instanceof Container container)) return;

            if (InventoryHelper.countItems(container, shop.item()) < amount) {
                player.sendSystemMessage(Component.literal("Shop is out of stock!").withStyle(net.minecraft.ChatFormatting.RED));
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
                player.sendSystemMessage(Component.literal("Successfully bought " + amount + "x items!").withStyle(net.minecraft.ChatFormatting.GREEN));
            } else {
                player.sendSystemMessage(Component.literal("Transaction failed!").withStyle(net.minecraft.ChatFormatting.RED));
            }
        } else {
            // Admin Shop
            if (playerAccount.decreaseBalance(totalCost).isSuccessful()) {
                var stack = shop.item().copy();
                stack.setCount(amount);
                InventoryHelper.addItemsToPlayer(player, stack);
                player.sendSystemMessage(Component.literal("Successfully bought " + amount + "x items from Admin Shop!").withStyle(net.minecraft.ChatFormatting.GREEN));
            }
        }
    }

    private static void handlePlayerSelling(ServerPlayer player, ChestShop shop, ServerLevel world, int amount, BigInteger totalPayout) {
        if (InventoryHelper.countItems(player.getInventory(), shop.item()) < amount) {
            player.sendSystemMessage(Component.literal("You don't have enough items to sell!").withStyle(net.minecraft.ChatFormatting.RED));
            return;
        }

        EconomyAccount playerAccount = EconomyWrapper.getPrimaryAccount(player);
        if (playerAccount == null) {
            player.sendSystemMessage(Component.literal("Account Error: Could not find your " + savage.chestshops.config.ModConfig.getConfig().economyProvider + " account!").withStyle(net.minecraft.ChatFormatting.RED));
            return;
        }

        if (!shop.isAdmin()) {
            BlockEntity be = world.getBlockEntity(shop.location().pos());
            if (!(be instanceof Container container)) return;

            EconomyAccount ownerAccount = EconomyWrapper.getPrimaryAccount(world.getServer(), shop.ownerId());
            if (!EconomyWrapper.canAfford(ownerAccount, totalPayout)) {
                player.sendSystemMessage(Component.literal("Shop owner cannot afford this!").withStyle(net.minecraft.ChatFormatting.RED));
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
                    player.sendSystemMessage(Component.literal("Shop chest is full!").withStyle(net.minecraft.ChatFormatting.RED));
                } else {
                    player.sendSystemMessage(Component.literal("Successfully sold " + amount + "x items!").withStyle(net.minecraft.ChatFormatting.GREEN));
                }
            } else {
                player.sendSystemMessage(Component.literal("Transaction Failed: The bank blocked the transfer.").withStyle(net.minecraft.ChatFormatting.RED));
            }
        } else {
            // Admin Shop
            InventoryHelper.removeItems(player.getInventory(), shop.item(), amount);
            playerAccount.increaseBalance(totalPayout);
            player.sendSystemMessage(Component.literal("Successfully sold " + amount + "x items to Admin Shop!").withStyle(net.minecraft.ChatFormatting.GREEN));
        }
    }
}
