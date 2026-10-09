package appeng.parts.automation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;

import appeng.api.behaviors.StackExportStrategy;
import appeng.api.behaviors.StackTransferContext;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageHelper;

public class StorageExportStrategy<T, S> implements StackExportStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(StorageExportStrategy.class);
    private final BlockApiCache<T, Direction> cache;
    private final Direction side;
    private final HandlerStrategy<T, S> handlerStrategy;

    public StorageExportStrategy(BlockApiLookup<T, Direction> capability,
            HandlerStrategy<T, S> handlerStrategy,
            ServerLevel level,
            BlockPos fromPos,
            Direction fromSide) {
        this.handlerStrategy = handlerStrategy;
        this.cache = BlockApiCache.create(capability, level, fromPos);
        this.side = fromSide;
    }

    @Override
    public long transfer(StackTransferContext context, AEKey what, long amount) {
        if (!handlerStrategy.isSupported(what)) {
            return 0;
        }

        var adjacentStorage = cache.find(side);
        if (adjacentStorage == null) {
            return 0;
        }

        var inv = context.getInternalStorage();

        var extracted = StorageHelper.poweredExtraction(
                context.getEnergySource(),
                inv.getInventory(),
                what,
                amount,
                context.getActionSource(),
                Actionable.SIMULATE);

        long wasInserted = handlerStrategy.insert(adjacentStorage, what, extracted, Actionable.SIMULATE);

        if (wasInserted > 0) {
            extracted = StorageHelper.poweredExtraction(
                    context.getEnergySource(),
                    inv.getInventory(),
                    what,
                    wasInserted,
                    context.getActionSource(),
                    Actionable.MODULATE);

            wasInserted = handlerStrategy.insert(adjacentStorage, what, extracted, Actionable.MODULATE);

            if (wasInserted < extracted) {
                // Be nice and try to give the overflow back
                long leftover = extracted - wasInserted;
                leftover -= inv.getInventory().insert(what, leftover, Actionable.MODULATE, context.getActionSource());
                if (leftover > 0) {
                    LOG.error("Storage export: adjacent block unexpectedly refused insert, voided {}x{}", leftover,
                            what);
                }
            }
        }

        return wasInserted;
    }

    @Override
    public long push(AEKey what, long amount, Actionable mode) {
        if (!handlerStrategy.isSupported(what)) {
            return 0;
        }

        var adjacentStorage = cache.find(side);
        if (adjacentStorage == null) {
            return 0;
        }

        return handlerStrategy.insert(adjacentStorage, what, amount, mode);
    }

    public static StackExportStrategy createItem(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        return new StorageExportStrategy<>(
                ItemStorage.SIDED,
                HandlerStrategy.ITEMS,
                level,
                fromPos,
                fromSide);
    }

    public static StackExportStrategy createFluid(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        return new StorageExportStrategy<>(
                FluidStorage.SIDED,
                HandlerStrategy.FLUIDS,
                level,
                fromPos,
                fromSide);
    }
}
