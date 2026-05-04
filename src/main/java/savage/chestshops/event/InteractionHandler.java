package savage.chestshops.event;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.WallSignBlock;
import savage.chestshops.interaction.TradeSessionManager;
import savage.chestshops.model.ChestShop;
import savage.chestshops.registry.ShopRegistry;
import savage.chestshops.util.SignUtil;

/**
 * Handles interactions with shop signs.
 */
public class InteractionHandler {

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide() || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            
            // Check if it's a sign attached to a shop
            if (world.getBlockState(pos).getBlock() instanceof WallSignBlock || world.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.SignBlock) {
                BlockPos chestPos = SignUtil.getAttachedChest(world, pos);
                net.minecraft.core.GlobalPos globalPos = net.minecraft.core.GlobalPos.of(world.dimension(), chestPos);
                ChestShop shop = ShopRegistry.getInstance().getShop(globalPos);

                if (shop != null) {
                    // Check if player is the owner (Disallow for regular shops, allow for Admin shops)
                    if (shop.ownerId().equals(serverPlayer.getUUID()) && !shop.isAdmin()) {
                        serverPlayer.sendSystemMessage(Component.literal("You cannot trade with your own shop!").withStyle(net.minecraft.ChatFormatting.RED));
                        return InteractionResult.SUCCESS;
                    }

                    // Initiate trade
                    TradeSessionManager.getInstance().startSession(serverPlayer.getUUID(), shop);
                    
                    String action = shop.isBuying() ? "sell" : "buy";
                    serverPlayer.sendSystemMessage(Component.literal("Type the amount you want to " + action + " in chat.").withStyle(net.minecraft.ChatFormatting.YELLOW));
                    serverPlayer.sendSystemMessage(Component.literal("Type 'all' to " + action + " everything.").withStyle(net.minecraft.ChatFormatting.YELLOW));
                    
                    return InteractionResult.SUCCESS;
                }
            }
            
            return InteractionResult.PASS;
        });
    }
}
