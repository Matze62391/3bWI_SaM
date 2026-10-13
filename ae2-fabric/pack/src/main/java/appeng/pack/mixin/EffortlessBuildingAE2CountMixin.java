package appeng.pack.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Effortless Building adds the blocks a player carries to the amount on the linked AE2 network. A network with an
 * infinite cell (or more than 2^31 items) reports Integer.MAX_VALUE, so the sum overflowed to a negative amount and the
 * build preview crashed the client with an IndexOutOfBoundsException in ItemUsageTracker. Capping the network amount
 * keeps the sum positive; it is far more than any build needs.
 */
@Mixin(targets = "nl.requios.effortlessbuilding.compat.ae2.AE2Integration", remap = false)
public class EffortlessBuildingAE2CountMixin {
    private static final int MAX_NETWORK_COUNT = 1 << 30;

    @ModifyReturnValue(method = { "countOnNetwork", "getCachedCount" }, at = @At("RETURN"))
    private static int ae2pack$capNetworkCount(int count) {
        return Math.min(count, MAX_NETWORK_COUNT);
    }
}
