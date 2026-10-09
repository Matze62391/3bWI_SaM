package appeng.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.LivingEntity;

import appeng.block.networking.CableBusBlock;

/**
 * Lets parts on cable buses act as ladders (NeoForge's {@code IBlockExtension#isLadder}).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityClimbableMixin {
    @Inject(method = "onClimbable", at = @At("RETURN"), cancellable = true)
    private void ae2$cableBusLadder(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            return;
        }
        var self = (LivingEntity) (Object) this;
        if (self.isSpectator()) {
            return;
        }
        var pos = self.blockPosition();
        var state = self.level().getBlockState(pos);
        if (state.getBlock() instanceof CableBusBlock cableBus && cableBus.isLadder(state, self.level(), pos, self)) {
            cir.setReturnValue(true);
        }
    }
}
