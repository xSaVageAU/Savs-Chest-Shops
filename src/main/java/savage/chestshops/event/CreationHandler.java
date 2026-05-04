package savage.chestshops.event;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.WallSignBlock;
import savage.chestshops.model.ChestShop;
import savage.chestshops.registry.ShopRegistry;
import savage.chestshops.util.SignUtil;

/**
 * Handles the final step of shop creation (sign setup).
 */
public class CreationHandler {

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide() || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            
            // If right-clicking a sign
            if (world.getBlockState(pos).getBlock() instanceof WallSignBlock || world.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.SignBlock) {
                BlockPos chestPos = SignUtil.getAttachedChest(world, pos);
                net.minecraft.core.GlobalPos globalPos = net.minecraft.core.GlobalPos.of(world.dimension(), chestPos);
                ChestShop shop = ShopRegistry.getInstance().getShop(globalPos);

                if (shop != null && shop.ownerId().equals(serverPlayer.getUUID())) {
                    // If the sign is not yet formatted, format it
                    // (We'll just always allow re-formatting by the owner for now)
                    SignUtil.updateSign((ServerLevel) world, pos, shop);
                    serverPlayer.sendSystemMessage(Component.literal("§aShop sign updated!"));
                    return InteractionResult.SUCCESS;
                }
            }
            
            return InteractionResult.PASS;
        });
    }
}
