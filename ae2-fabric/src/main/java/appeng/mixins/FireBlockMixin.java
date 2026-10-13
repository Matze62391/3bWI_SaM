package appeng.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;

import appeng.block.misc.TinyTNTBlock;

/**
 * Ignites tiny TNT when fire spreads to it, like vanilla TNT (NeoForge's {@code IBlockExtension#onCaughtFire}).
 */
@Mixin(FireBlock.class)
public abstract class FireBlockMixin {
    @Inject(method = "checkBurnOut", at = @At("HEAD"))
    private void ae2$igniteTinyTnt(Level level, BlockPos pos, int chance, RandomSource random, int age,
            CallbackInfo ci) {
        var state = level.getBlockState(pos);
        // Tiny TNT burns as easily as vanilla TNT (burn odds of 100)
        if (state.getBlock() instanceof TinyTNTBlock tinyTnt && random.nextInt(chance) < 100) {
            tinyTnt.onCaughtFire(state, level, pos, null, null, ItemStack.EMPTY);
            level.removeBlock(pos, false);
        }
    }
}
