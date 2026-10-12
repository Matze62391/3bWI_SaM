package rearth.oritech.port.neoforge.capabilities;

import java.util.function.BiFunction;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A capability of blocks, backed by a Fabric {@link BlockApiLookup} with an adapter between Oritech's handler type
 * {@code T} and the Fabric API type {@code F}.
 */
public final class BlockCapability<T, C> {
    private final BlockApiLookup<Object, Direction> lookup;
    private final java.util.function.Function<Object, T> fromFabric;
    private final java.util.function.Function<T, Object> toFabric;

    @SuppressWarnings("unchecked")
    <F> BlockCapability(BlockApiLookup<F, Direction> lookup, java.util.function.Function<F, T> fromFabric,
            java.util.function.Function<T, F> toFabric) {
        this.lookup = (BlockApiLookup<Object, Direction>) (Object) lookup;
        this.fromFabric = (java.util.function.Function<Object, T>) fromFabric;
        this.toFabric = (java.util.function.Function<T, Object>) toFabric;
    }

    BlockApiLookup<Object, Direction> lookup() {
        return lookup;
    }

    T fromFabric(@Nullable Object fabric) {
        return fabric == null ? null : fromFabric.apply(fabric);
    }

    @Nullable
    Object toFabric(@Nullable T handler) {
        return handler == null ? null : toFabric.apply(handler);
    }

    @Nullable
    public T getCapability(Level level, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity blockEntity, @Nullable C context) {
        return fromFabric(lookup.find(level, pos, state, blockEntity, (Direction) context));
    }

    @Nullable
    public T getCapability(Level level, BlockPos pos, @Nullable C context) {
        return fromFabric(lookup.find(level, pos, (Direction) context));
    }

    <B extends BlockEntity> void registerBlockEntity(BlockEntityType<B> type, BiFunction<? super B, C, T> provider) {
        lookup.registerForBlockEntity((be, side) -> toFabric(provider.apply(be, castContext(side))), type);
    }

    @SuppressWarnings("unchecked")
    private C castContext(@Nullable Direction side) {
        return (C) side;
    }
}
