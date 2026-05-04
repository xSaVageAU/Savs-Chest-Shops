package savage.chestshops.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import savage.chestshops.model.ChestShop;

/**
 * Utility for shop sign operations.
 */
public class SignUtil {

    public static void updateSign(ServerLevel world, BlockPos signPos, ChestShop shop) {
        BlockEntity be = world.getBlockEntity(signPos);
        if (be instanceof SignBlockEntity sign) {
            String type = shop.isBuying() ? "§1BUYING" : "§4SELLING";
            String itemName = shop.item().getHoverName().getString();
            if (itemName.length() > 15) {
                itemName = itemName.substring(0, 12) + "...";
            }

            int stock = ShopStockCalculator.calculateStock(world, shop);
            String stockText;
            if (stock == -1) {
                stockText = "Stock: ∞";
            } else {
                stockText = (shop.isBuying() ? "Space: " : "Stock: ") + stock;
            }

            Component line1 = shop.isAdmin() ? Component.literal("§4[Admin Shop]") : Component.literal("§1" + shop.ownerName());
            Component line2 = Component.literal(itemName);
            Component line3 = Component.literal("§0" + (shop.isBuying() ? "Buying" : "Selling") + ": " + savage.chestshops.economy.EconomyWrapper.format(shop.price()));
            Component line4 = Component.literal("§0" + stockText);

            sign.setText(sign.getFrontText()
                .setMessage(0, line1)
                .setMessage(1, line2)
                .setMessage(2, line3)
                .setMessage(3, line4), true);
            
            world.sendBlockUpdated(signPos, world.getBlockState(signPos), world.getBlockState(signPos), 3);
        }
    }

    public static boolean placeSign(Level world, BlockPos chestPos, ChestShop shop, Direction playerFacing) {
        Direction[] prioritizedDirections;
        if (playerFacing != null && playerFacing.getAxis().isHorizontal()) {
            Direction preferredSide = playerFacing.getOpposite();
            prioritizedDirections = new Direction[]{preferredSide, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        } else {
            prioritizedDirections = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        }

        for (Direction direction : prioritizedDirections) {
            BlockPos signPos = chestPos.relative(direction);
            BlockState signState = world.getBlockState(signPos);

            if (signState.isAir() || signState.canBeReplaced()) {
                BlockState wallSign = getWallSignForDirection(direction);
                if (wallSign != null) {
                    world.setBlock(signPos, wallSign, 3);
                    if (world instanceof ServerLevel serverLevel) {
                        updateSign(serverLevel, signPos, shop);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private static BlockState getWallSignForDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> net.minecraft.world.level.block.Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.NORTH);
            case SOUTH -> net.minecraft.world.level.block.Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.SOUTH);
            case EAST -> net.minecraft.world.level.block.Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.EAST);
            case WEST -> net.minecraft.world.level.block.Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.WEST);
            default -> null;
        };
    }

    public static BlockPos getAttachedChest(Level world, BlockPos signPos) {
        BlockState state = world.getBlockState(signPos);
        if (state.getBlock() instanceof WallSignBlock) {
            Direction dir = state.getValue(WallSignBlock.FACING).getOpposite();
            return signPos.relative(dir);
        }
        // If it's a standing sign, maybe it's on top of the chest?
        return signPos.below();
    }

    public static BlockPos findSignForChest(Level world, BlockPos chestPos) {
        for (Direction direction : Direction.values()) {
            if (!direction.getAxis().isHorizontal()) continue;
            BlockPos signPos = chestPos.relative(direction);
            BlockState state = world.getBlockState(signPos);
            if (state.getBlock() instanceof WallSignBlock) {
                if (state.getValue(WallSignBlock.FACING) == direction) {
                    return signPos;
                }
            }
        }
        // Also check top
        BlockPos topPos = chestPos.above();
        if (world.getBlockState(topPos).getBlock() instanceof net.minecraft.world.level.block.SignBlock) {
            return topPos;
        }
        return null;
    }
}
