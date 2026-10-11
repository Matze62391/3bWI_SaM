package net.pedroksl.advanced_ae.common.fluids;

import javax.annotation.ParametersAreNonnullByDefault;


import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.pedroksl.ae2addonlib.fluid.BaseFlowingFluid;
import net.pedroksl.advanced_ae.common.definitions.AAEFluids;

@ParametersAreNonnullByDefault
public abstract class QuantumInfusionFluid extends BaseFlowingFluid {
    public static final BaseFlowingFluid.Properties PROPERTIES = new BaseFlowingFluid.Properties(
                    AAEFluids.QUANTUM_INFUSION.fluidTypeHolder(),
                    AAEFluids.QUANTUM_INFUSION.sourceHolder(),
                    AAEFluids.QUANTUM_INFUSION.flowingHolder())
            .bucket(AAEFluids.QUANTUM_INFUSION::bucketItem)
            .block(AAEFluids.QUANTUM_INFUSION.blockHolder());

    protected QuantumInfusionFluid(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canConvertToSource(ServerLevel level) {
        return false;
    }

    public static class Flowing extends QuantumInfusionFluid {
        public Flowing() {
            super(PROPERTIES);
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> pBuilder) {
            super.createFluidStateDefinition(pBuilder);
            pBuilder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState pState) {
            return pState.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState pState) {
            return false;
        }
    }

    public static class Source extends QuantumInfusionFluid {
        public Source() {
            super(PROPERTIES);
        }

        @Override
        public int getAmount(FluidState pState) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState pState) {
            return true;
        }
    }
}
