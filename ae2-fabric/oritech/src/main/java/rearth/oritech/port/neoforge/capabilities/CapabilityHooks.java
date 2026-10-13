package rearth.oritech.port.neoforge.capabilities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import rearth.oritech.port.neoforge.transfer.access.ItemAccess;

/**
 * Replaces NeoForge's {@code Level#getCapability} and {@code ItemStack#getCapability} extension methods.
 */
public final class CapabilityHooks {
    private CapabilityHooks() {}

    @Nullable
    public static <T, C> T get(Level level, BlockCapability<T, C> capability, BlockPos pos, @Nullable C context) {
        return capability.getCapability(level, pos, context);
    }

    @Nullable
    public static <T, C> T get(Level level, BlockCapability<T, C> capability, BlockPos pos, @Nullable BlockState state,
            @Nullable BlockEntity blockEntity, @Nullable C context) {
        return capability.getCapability(level, pos, state, blockEntity, context);
    }

    @Nullable
    public static <T> T get(ItemStack stack, ItemCapability<T, ItemAccess> capability, ItemAccess access) {
        return capability.getCapability(access);
    }

    @Nullable
    public static <T> T get(ItemStack stack, ItemCapability<T, ItemAccess> capability) {
        return capability.getCapability(ItemAccess.forStack(stack));
    }

    /**
     * NeoForge caches capabilities and needs explicit invalidation. Fabric's API lookups query the provider on every
     * access, so there is nothing to invalidate.
     */
    public static void invalidate(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
    }

    public static <T> T get(rearth.oritech.port.neoforge.transfer.access.ItemAccess access, ItemCapability<T, rearth.oritech.port.neoforge.transfer.access.ItemAccess> capability) {
        return access.getCapability(capability);
    }
}
