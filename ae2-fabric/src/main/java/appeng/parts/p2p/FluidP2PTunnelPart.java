package appeng.parts.p2p;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;

import appeng.api.parts.IPartItem;
import appeng.api.stacks.AEKeyType;

public class FluidP2PTunnelPart extends ResourceHandlerP2PTunnelPart<FluidP2PTunnelPart, FluidVariant> {
    public FluidP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem, FluidStorage.SIDED, AEKeyType.fluids());
    }
}
