package savage.chestshops.event;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import savage.chestshops.model.ChestShop;
import savage.chestshops.registry.ShopRegistry;

/**
 * Handles protection for shop chests.
 */
public class ProtectionHandler {

    public static void register() {
        // Prevent opening shop chests by non-owners
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide() || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            BlockEntity be = world.getBlockEntity(pos);

            if (be instanceof ChestBlockEntity) {
                net.minecraft.core.GlobalPos globalPos = net.minecraft.core.GlobalPos.of(world.dimension(), pos);
                ChestShop shop = ShopRegistry.getInstance().getShop(globalPos);

                if (shop != null) {
                    boolean isOwner = shop.ownerId().equals(serverPlayer.getUUID());
                    boolean isAdmin = savage.chestshops.util.PermissionUtil.isAdmin(serverPlayer);

                    if (!isOwner && !isAdmin) {
                        serverPlayer.sendSystemMessage(Component.literal("This chest is protected by a shop! Only the owner can open it.").withStyle(net.minecraft.ChatFormatting.RED));
                        return InteractionResult.FAIL;
                    }
                }
            }
            return InteractionResult.PASS;
        });

        // Prevent breaking shop chests by non-owners
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return true;

            net.minecraft.core.GlobalPos globalPos = net.minecraft.core.GlobalPos.of(world.dimension(), pos);
            if (ShopRegistry.getInstance().isShop(globalPos)) {
                ChestShop shop = ShopRegistry.getInstance().getShop(globalPos);
                if (shop != null) {
                    boolean isOwner = shop.ownerId().equals(serverPlayer.getUUID());
                    boolean isAdmin = savage.chestshops.util.PermissionUtil.isAdmin(serverPlayer);

                    if (!isOwner && !isAdmin) {
                        serverPlayer.sendSystemMessage(Component.literal("You cannot break shop chests! Use /shop remove or break the sign first.").withStyle(net.minecraft.ChatFormatting.RED));
                        return false;
                    }
                    // If owner breaks it, remove the shop
                    ShopRegistry.getInstance().removeShop(globalPos);
                    serverPlayer.sendSystemMessage(Component.literal("Shop removed.").withStyle(net.minecraft.ChatFormatting.GREEN));
                }
            }
            return true;
        });
    }
}
