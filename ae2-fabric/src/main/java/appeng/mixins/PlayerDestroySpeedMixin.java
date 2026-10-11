package appeng.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import appeng.hooks.SkyStoneBreakSpeed;

/**
 * Replaces NeoForge's PlayerEvent.BreakSpeed for {@link SkyStoneBreakSpeed}.
 */
@Mixin(Player.class)
public abstract class PlayerDestroySpeedMixin {
    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void ae2$modifyDestroySpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        var speed = cir.getReturnValueF();
        var newSpeed = SkyStoneBreakSpeed.handleBreakFaster((Player) (Object) this, state, speed);
        if (newSpeed != speed) {
            cir.setReturnValue(newSpeed);
        }
    }
}
