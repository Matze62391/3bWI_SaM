package net.pedroksl.ae2addonlib.fluid;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.world.item.ItemStack;

/**
 * Helpers for fluid containers such as buckets (replaces parts of NeoForge's FluidUtil).
 */
public final class FluidContainers {
    private FluidContainers() {
    }

    /**
     * Returns the first fluid contained in the given item, or {@link FluidStack#EMPTY}.
     */
    public static FluidStack getFirstStackContained(ItemStack stack) {
        if (stack.isEmpty()) {
            return FluidStack.EMPTY;
        }
        var storage = ContainerItemContext.withConstant(stack).find(FluidStorage.ITEM);
        if (storage == null) {
            return FluidStack.EMPTY;
        }
        var content = StorageUtil.findExtractableContent(storage, null);
        return content == null ? FluidStack.EMPTY : new FluidStack(content.resource(), content.amount());
    }
}
