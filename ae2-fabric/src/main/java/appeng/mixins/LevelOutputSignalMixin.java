package appeng.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import appeng.block.networking.CableBusBlock;

/**
 * Block entities call {@code updateNeighbourForOutputSignal} when their content changes. NeoForge forwards this to
 * all neighbors ({@code IBlockExtension#onNeighborChange}), which AE2's parts (i.e. storage buses) use to detect
 * changes in adjacent inventories.
 */
@Mixin(Level.class)
public abstract class LevelOutputSignalMixin {
    @Inject(method = "updateNeighbourForOutputSignal", at = @At("HEAD"))
    private void ae2$notifyCableBuses(BlockPos pos, Block changedBlock, CallbackInfo ci) {
        var level = (Level) (Object) this;
        for (var direction : Direction.values()) {
            var neighborPos = pos.relative(direction);
            if (level.hasChunkAt(neighborPos)) {
                var state = level.getBlockState(neighborPos);
                if (state.getBlock() instanceof CableBusBlock cableBus) {
                    cableBus.onNeighborChange(state, level, neighborPos, pos);
                }
            }
        }
    }
}
