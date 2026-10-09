package appeng.util;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;

import appeng.api.stacks.GenericStack;

/**
 * Allows generalized extraction from item-based containers such as buckets or tanks.
 */
public final class GenericContainerHelper {
    private GenericContainerHelper() {
    }

    @Nullable
    public static GenericStack getContainedFluidStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        var storage = ContainerItemContext.withConstant(stack).find(FluidStorage.ITEM);
        var content = StorageUtil.findStoredResource(storage);
        if (content == null) {
            return null;
        }
        long amount = 0;
        for (var view : storage) {
            if (!view.isResourceBlank() && view.getResource().equals(content)) {
                amount += view.getAmount();
            }
        }
        return GenericStack.from(content, amount);
    }

}
