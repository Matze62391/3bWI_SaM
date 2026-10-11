package appeng.mixins.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import appeng.block.networking.CableBusBlock;
import appeng.client.block.cablebus.CableBusBlockClientExtensions;
import appeng.client.renderer.spatialstorage.SpatialStorageEnvironment;

/**
 * Replaces the hit and destroy particles of cable bus blocks with particles of the attached parts. NeoForge offers
 * this through client block extensions.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelParticlesMixin {
    @Shadow
    private void playBreakingSound(BlockPos pos, BlockState blockState) {
        throw new AssertionError();
    }

    @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
    private void ae2$addDestroyBlockEffect(BlockPos pos, BlockState blockState, CallbackInfo ci) {
        if (blockState.getBlock() instanceof CableBusBlock cableBus) {
            var self = (ClientLevel) (Object) this;
            new CableBusBlockClientExtensions(cableBus).addDestroyEffects(blockState, self, pos,
                    Minecraft.getInstance().particleEngine);
            ci.cancel();
        }
    }

    @Inject(method = "addBreakingBlockEffects", at = @At("HEAD"), cancellable = true)
    private void ae2$addBreakingBlockEffects(BlockPos pos, Direction direction, boolean playSound,
            CallbackInfo ci) {
        var self = (ClientLevel) (Object) this;
        var blockState = self.getBlockState(pos);
        if (blockState.getBlock() instanceof CableBusBlock cableBus) {
            if (playSound) {
                playBreakingSound(pos, blockState);
            }
            new CableBusBlockClientExtensions(cableBus).addHitEffects(blockState, self, pos, direction,
                    Minecraft.getInstance().particleEngine);
            ci.cancel();
        }
    }

    @Inject(method = "tickWeatherEffects", at = @At("HEAD"), cancellable = true)
    private void ae2$skipSpatialWeatherTick(CallbackInfo ci) {
        if (SpatialStorageEnvironment.isActive()) {
            ci.cancel();
        }
    }
}
