package savage.chestshops.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import savage.chestshops.model.ChestShop;

public class ShopStockCalculator {

    public static int calculateStock(ServerLevel world, ChestShop shop) {
        if (shop.isAdmin()) return -1;

        BlockEntity be = world.getBlockEntity(shop.location().pos());
        if (!(be instanceof Container container)) return 0;

        if (shop.isBuying()) {
            // Shop buys from player, so we calculate space
            return InventoryHelper.calculateSpace(container, shop.item());
        } else {
            // Shop sells to player, so we calculate stock
            return InventoryHelper.countItems(container, shop.item());
        }
    }
}
