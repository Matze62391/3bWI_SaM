package appeng.helpers.externalstorage;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.stacks.AEKeyType;
import appeng.helpers.ResourceConversion;

/**
 * Exposes a {@link GenericInternalInventory} as the platforms external fluid storage interface.
 */
public class GenericStackFluidHandler extends GenericStackInvHandler<FluidVariant> {
    public GenericStackFluidHandler(GenericInternalInventory inv) {
        super(ResourceConversion.FLUID, AEKeyType.fluids(), inv);
    }
}
