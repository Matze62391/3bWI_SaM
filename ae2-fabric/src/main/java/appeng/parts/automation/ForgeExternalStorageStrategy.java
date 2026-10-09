package appeng.parts.automation;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;

import appeng.api.behaviors.ExternalStorageStrategy;
import appeng.api.storage.MEStorage;

public class ForgeExternalStorageStrategy<T, S> implements ExternalStorageStrategy {
    private final BlockApiCache<T, Direction> cache;
    private final Direction side;
    private final HandlerStrategy<T, S> conversion;

    public ForgeExternalStorageStrategy(BlockApiLookup<T, Direction> capability,
            HandlerStrategy<T, S> conversion,
            ServerLevel level,
            BlockPos fromPos,
            Direction fromSide) {
        this.cache = BlockApiCache.create(capability, level, fromPos);
        this.side = fromSide;
        this.conversion = conversion;
    }

    @Nullable
    @Override
    public MEStorage createWrapper(boolean extractableOnly, Runnable injectOrExtractCallback) {
        var storage = cache.find(side);
        if (storage == null) {
            return null;
        }

        var result = conversion.getFacade(storage);
        result.setChangeListener(injectOrExtractCallback);
        result.setExtractableOnly(extractableOnly);
        return result;
    }

    public static ExternalStorageStrategy createItem(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        return new ForgeExternalStorageStrategy<>(
                ItemStorage.SIDED,
                HandlerStrategy.ITEMS,
                level,
                fromPos,
                fromSide);
    }

    public static ExternalStorageStrategy createFluid(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        return new ForgeExternalStorageStrategy<>(
                FluidStorage.SIDED,
                HandlerStrategy.FLUIDS,
                level,
                fromPos,
                fromSide);
    }
}
