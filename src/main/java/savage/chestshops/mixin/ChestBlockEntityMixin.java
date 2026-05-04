package savage.chestshops.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class ChestBlockEntityMixin {

    @Inject(method = "setChanged", at = @At("TAIL"))
    private void onSetChanged(CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;
        if (self instanceof ChestBlockEntity && self.getLevel() != null && !self.getLevel().isClientSide()) {
            BlockPos pos = self.getBlockPos();
            net.minecraft.core.GlobalPos globalPos = net.minecraft.core.GlobalPos.of(self.getLevel().dimension(), pos);
            if (savage.chestshops.registry.ShopRegistry.getInstance().isShop(globalPos)) {
                savage.chestshops.registry.ShopRegistry.getInstance().markDirty(globalPos);
            }
        }
    }
}
