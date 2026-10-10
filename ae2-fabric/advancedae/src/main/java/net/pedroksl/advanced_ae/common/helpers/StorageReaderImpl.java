package net.pedroksl.advanced_ae.common.helpers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;

import appeng.api.AECapabilities;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import appeng.parts.automation.HandlerStrategy;

public class StorageReaderImpl<T, S> implements StorageReader {

    private final BlockApiCache<T, Direction> cache;
    private final BlockApiCache<MEStorage, Direction> meCache;
    private final Direction fromSide;
    private final HandlerStrategy<T, S> conversion;

    public StorageReaderImpl(
            BlockApiLookup<T, Direction> capability,
            HandlerStrategy<T, S> conversion,
            ServerLevel level,
            BlockPos fromPos,
            Direction fromSide) {
        this.cache = BlockApiCache.create(capability, level, fromPos);
        this.meCache = BlockApiCache.create(AECapabilities.ME_STORAGE, level, fromPos);
        this.fromSide = fromSide;
        this.conversion = conversion;
    }

    public long getCurrentStock(AEKey what) {
        if (what.getType() != conversion.getKeyType()) {
            return 0;
        }

        // Try internal capability first
        var meHandler = meCache.find(fromSide);
        if (meHandler != null) {
            var keys = meHandler.getAvailableStacks();
            if (keys.get(what) > 0) {
                return keys.get(what);
            }
        }

        var adjacentHandler = cache.find(fromSide);
        if (adjacentHandler == null) {
            return 0;
        }

        var adjacentStorage = conversion.getFacade(adjacentHandler);
        var amount = adjacentStorage.getAvailableStacks().get(what);
        return Math.max(0, amount);
    }

    public static StorageReader item(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        return new StorageReaderImpl<>(ItemStorage.SIDED, HandlerStrategy.ITEMS, level, fromPos, fromSide);
    }

    public static StorageReader fluid(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        return new StorageReaderImpl<>(FluidStorage.SIDED, HandlerStrategy.FLUIDS, level, fromPos, fromSide);
    }
}
