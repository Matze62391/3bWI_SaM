package rearth.oritech.port.neoforge.client.fluid;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.state.BlockState;

public final class FluidTintSources {
    private FluidTintSources() {}

    public static BlockTintSource constant(int color) {
        return new BlockTintSource() {
            @Override
            public int color(BlockState state) {
                return color;
            }
        };
    }
}
