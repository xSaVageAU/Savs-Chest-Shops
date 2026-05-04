package savage.chestshops.event;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.chat.Component;
import savage.chestshops.model.ChestShop;
import savage.chestshops.interaction.TradeProcessor;
import savage.chestshops.interaction.TradeSessionManager;

/**
 * Handles chat input for shop trades.
 */
public class ChatEventHandler {

    public static void register() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            TradeSessionManager.PendingTrade session = TradeSessionManager.getInstance().getSession(sender.getUUID());

            if (session != null) {
                String content = message.signedContent().trim().toLowerCase();

                if (content.equals("cancel")) {
                    sender.sendSystemMessage(Component.literal("§eTrade cancelled."));
                    TradeSessionManager.getInstance().endSession(sender.getUUID());
                    return false;
                }

                try {
                    int amount;
                    if (content.equals("all")) {
                        ChestShop shop = session.getShop();
                        if (shop.isBuying()) {
                            // Player sells to shop: count how many items player has
                            amount = savage.chestshops.util.InventoryHelper.countItems(sender.getInventory(), shop.getItem());
                        } else {
                            // Player buys from shop: how many can they afford / how many are in stock
                            if (shop.isAdmin()) {
                                sender.sendSystemMessage(Component.literal("§cAdmin shops have infinite stock! Please type a specific amount to buy."));
                                TradeSessionManager.getInstance().endSession(sender.getUUID());
                                return false;
                            }
                            
                            var account = savage.chestshops.economy.EconomyWrapper.getPrimaryAccount(sender);
                            if (account == null) {
                                sender.sendSystemMessage(Component.literal("§cYou don't have an economy account!"));
                                TradeSessionManager.getInstance().endSession(sender.getUUID());
                                return false;
                            }

                            int canAfford = account.balance().divide(shop.getPrice()).intValue();
                            int shopHas = savage.chestshops.util.ShopStockCalculator.calculateStock((net.minecraft.server.level.ServerLevel)sender.level(), shop);
                            amount = Math.min(canAfford, shopHas);
                        }
                    } else {
                        amount = Integer.parseInt(content);
                    }

                    if (amount <= 0) {
                        sender.sendSystemMessage(Component.literal("§cInvalid amount! Transaction cancelled."));
                        TradeSessionManager.getInstance().endSession(sender.getUUID());
                        return false;
                    }

                    TradeProcessor.processTrade(sender, session.getShop(), amount);
                    TradeSessionManager.getInstance().endSession(sender.getUUID());
                    return false; // Suppress chat message

                } catch (NumberFormatException e) {
                    sender.sendSystemMessage(Component.literal("§cInvalid amount! Transaction cancelled."));
                    TradeSessionManager.getInstance().endSession(sender.getUUID());
                    return false;
                }
            }

            return true;
        });
    }
}
