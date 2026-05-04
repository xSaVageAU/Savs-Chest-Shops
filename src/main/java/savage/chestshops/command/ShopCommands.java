package savage.chestshops.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import savage.chestshops.model.ChestShop;
import savage.chestshops.registry.ShopRegistry;

import java.math.BigInteger;

/**
 * Commands for creating and managing shops.
 */
public class ShopCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("shop")
            .then(Commands.literal("create")
                .then(Commands.literal("buy")
                    .then(Commands.argument("price", StringArgumentType.string())
                        .executes(ctx -> createShop(ctx, true, false))))
                .then(Commands.literal("sell")
                    .then(Commands.argument("price", StringArgumentType.string())
                        .executes(ctx -> createShop(ctx, false, false))))
                .then(Commands.literal("admin")
                    .requires(source -> {
                        try {
                            return savage.chestshops.util.PermissionUtil.isAdmin(source.getPlayerOrException());
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .then(Commands.literal("buy")
                        .then(Commands.argument("price", StringArgumentType.string())
                            .executes(ctx -> createShop(ctx, true, true))))
                    .then(Commands.literal("sell")
                        .then(Commands.argument("price", StringArgumentType.string())
                            .executes(ctx -> createShop(ctx, false, true))))))
            .then(Commands.literal("remove")
                .executes(ctx -> removeShop(ctx))));
    }

    private static int createShop(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, boolean isBuying, boolean isAdmin) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            double priceDouble;
            try {
                priceDouble = Double.parseDouble(StringArgumentType.getString(context, "price"));
            } catch (NumberFormatException e) {
                context.getSource().sendFailure(Component.literal("§cInvalid price format! Use a number like 10.00"));
                return 0;
            }

            BigInteger price = BigInteger.valueOf((long) (priceDouble * 100));
            ItemStack heldItem = player.getMainHandItem();

            if (heldItem.isEmpty()) {
                context.getSource().sendFailure(Component.literal("§cHold an item in your hand first!"));
                return 0;
            }

            HitResult hit = player.pick(5.0, 0.0f, false);
            if (hit.getType() != HitResult.Type.BLOCK) {
                context.getSource().sendFailure(Component.literal("§cLook at a chest to create a shop!"));
                return 0;
            }

            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            BlockEntity be = player.level().getBlockEntity(pos);

            if (!(be instanceof Container)) {
                context.getSource().sendFailure(Component.literal("§cYou must look at a chest or container!"));
                return 0;
            }

            net.minecraft.core.GlobalPos globalPos = net.minecraft.core.GlobalPos.of(player.level().dimension(), pos);
            if (ShopRegistry.getInstance().isShop(globalPos)) {
                context.getSource().sendFailure(Component.literal("§cA shop already exists here!"));
                return 0;
            }

            ChestShop shop = new ChestShop(globalPos, player.getUUID(), player.getGameProfile().name(), heldItem.copy(), price, isBuying, isAdmin);
            ShopRegistry.getInstance().addShop(shop);
            
            if (savage.chestshops.util.SignUtil.placeSign(player.level(), pos, shop, player.getDirection())) {
                String formattedPrice = savage.chestshops.economy.EconomyWrapper.format(price);
                context.getSource().sendSuccess(() -> Component.literal("§aShop created for §e" + formattedPrice + "§a!"), true);
            } else {
                context.getSource().sendSuccess(() -> Component.literal("§eShop created, but failed to place sign. Place one manually."), true);
            }
            return 1;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cInvalid price format!"));
            return 0;
        }
    }

    private static int removeShop(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            HitResult hit = player.pick(5.0, 0.0f, false);
            if (hit.getType() != HitResult.Type.BLOCK) return 0;

            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            net.minecraft.core.GlobalPos globalPos = net.minecraft.core.GlobalPos.of(player.level().dimension(), pos);
            ChestShop shop = ShopRegistry.getInstance().getShop(globalPos);

            if (shop != null) {
                ShopRegistry.getInstance().removeShop(globalPos);
                context.getSource().sendSuccess(() -> Component.literal("§aShop removed."), true);
            } else {
                context.getSource().sendFailure(Component.literal("§cNo shop found at this location."));
            }
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }
}
