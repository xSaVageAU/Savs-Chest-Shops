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

        // Prevent breaking shop chests and signs by non-owners
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return true;

            net.minecraft.core.GlobalPos targetPos = net.minecraft.core.GlobalPos.of(world.dimension(), pos);
            ChestShop shop = ShopRegistry.getInstance().getShop(targetPos);

            // If the block isn't a chest, maybe it's a sign attached to a shop chest?
            if (shop == null && state.getBlock() instanceof net.minecraft.world.level.block.SignBlock) {
                BlockPos attachedPos = savage.chestshops.util.SignUtil.getAttachedChest(world, pos);
                net.minecraft.core.GlobalPos attachedGlobalPos = net.minecraft.core.GlobalPos.of(world.dimension(), attachedPos);
                shop = ShopRegistry.getInstance().getShop(attachedGlobalPos);
                
                if (shop != null) {
                    // Check if the broken sign is actually the official shop sign
                    BlockPos officialSignPos = savage.chestshops.util.SignUtil.findSignForChest(world, attachedPos);
                    if (pos.equals(officialSignPos)) {
                        targetPos = attachedGlobalPos; // So we remove the actual shop entry if broken
                    } else {
                        shop = null; // It's just a decorative sign, let them break it normally
                    }
                }
            }

            if (shop != null) {
                boolean isChest = pos.equals(targetPos.pos());

                if (isChest) {
                    serverPlayer.sendSystemMessage(Component.literal("You cannot break a shop chest! Break the sign first or use /shop remove.").withStyle(net.minecraft.ChatFormatting.RED));
                    return false;
                }

                boolean isOwner = shop.ownerId().equals(serverPlayer.getUUID());
                boolean isAdmin = savage.chestshops.util.PermissionUtil.isAdmin(serverPlayer);

                if (!isOwner && !isAdmin) {
                    serverPlayer.sendSystemMessage(Component.literal("You cannot break this shop's sign!").withStyle(net.minecraft.ChatFormatting.RED));
                    return false;
                }
                
                // If owner breaks the sign, remove the shop
                ShopRegistry.getInstance().removeShop(targetPos);
                serverPlayer.sendSystemMessage(Component.literal("Shop removed.").withStyle(net.minecraft.ChatFormatting.GREEN));
            }
            return true;
        });
    }
}
