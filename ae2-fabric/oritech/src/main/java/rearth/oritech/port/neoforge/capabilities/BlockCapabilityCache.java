package rearth.oritech.port.neoforge.capabilities;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/**
 * Caches the lookup of a capability at one position, on top of Fabric's {@link BlockApiCache}.
 */
public final class BlockCapabilityCache<T, C> {
    private final BlockCapability<T, C> capability;
    private final BlockApiCache<Object, Direction> cache;
    private final BlockPos pos;
    @Nullable
    private final C context;

    private BlockCapabilityCache(BlockCapability<T, C> capability, ServerLevel level, BlockPos pos, @Nullable C context) {
        this.capability = capability;
        this.cache = BlockApiCache.create(capability.lookup(), level, pos);
        this.pos = pos;
        this.context = context;
    }

    public static <T, C> BlockCapabilityCache<T, C> create(BlockCapability<T, C> capability, ServerLevel level,
            BlockPos pos, @Nullable C context) {
        return new BlockCapabilityCache<>(capability, level, pos, context);
    }

    public static <T, C> BlockCapabilityCache<T, C> create(BlockCapability<T, C> capability, ServerLevel level,
            BlockPos pos, @Nullable C context, java.util.function.BooleanSupplier isValid, Runnable invalidationListener) {
        return new BlockCapabilityCache<>(capability, level, pos, context);
    }

    @Nullable
    public T getCapability() {
        return capability.fromFabric(cache.find((Direction) context));
    }

    public ServerLevel level() {
        return (ServerLevel) cache.getLevel();
    }

    public BlockPos pos() {
        return pos;
    }

    @Nullable
    public C context() {
        return context;
    }
}
