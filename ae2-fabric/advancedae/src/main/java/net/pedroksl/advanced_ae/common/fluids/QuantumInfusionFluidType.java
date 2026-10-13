package net.pedroksl.advanced_ae.common.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.FluidState;
import net.pedroksl.ae2addonlib.fluid.FluidType.SoundAction;
import net.pedroksl.ae2addonlib.fluid.FluidType;
import net.pedroksl.ae2addonlib.util.WaterBasedFluidType;

public class QuantumInfusionFluidType extends FluidType implements WaterBasedFluidType {
    public QuantumInfusionFluidType() {
        super(Properties.create()
                .density(300)
                .viscosity(1000)
                .sound(SoundAction.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundAction.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY));
    }

    @Override
    public boolean canConvertToSource(FluidState state, LevelReader reader, BlockPos pos) {
        return false;
    }

    @Override
    public int getTintColor() {
        return 0xFF7362D3;
    }
}
