package com.glodblock.github.appflux.common.me.energy;

import appeng.api.AECapabilities;
import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Caches the energy APIs of the blocks next to a flux accessor or an interface with an induction card. Energy isn't
 * sent to blocks that are part of the same grid.
 */
public class EnergyCapCache {

    private final Map<BlockApiLookup<?, Direction>, BlockApiCache<?, Direction>[]> cache = new IdentityHashMap<>();
    private final Map<Direction, BlockApiCache<IInWorldGridNodeHost, Void>> girdCache = new IdentityHashMap<>();
    private final ServerLevel world;
    private final BlockPos pos;
    private final Supplier<IGrid> self;

    public EnergyCapCache(ServerLevel world, BlockPos pos, Supplier<IGrid> gridSupplier) {
        this.world = world;
        this.pos = pos;
        this.self = gridSupplier;
    }

    @Nullable
    public <T> T getEnergyCap(BlockApiLookup<T, Direction> cap, Direction side) {
        if (this.checkGrid(side)) {
            return this.getEnergyCacheInternal(cap, side).find(side.getOpposite());
        }
        return null;
    }

    private boolean checkGrid(Direction side) {
        var gird = this.getGridCache(side).find(null);
        if (gird == null) {
            return true;
        }
        var thisGrid = this.self.get();
        if (thisGrid != null) {
            var thatGrid = gird.getGridNode(side.getOpposite());
            if (thatGrid == null) {
                return true;
            }
            return thatGrid.getGrid() != thisGrid;
        }
        return true;
    }

    private BlockApiCache<IInWorldGridNodeHost, Void> getGridCache(Direction side) {
        return this.girdCache.computeIfAbsent(side, face -> BlockApiCache.create(AECapabilities.IN_WORLD_GRID_NODE_HOST, this.world, this.pos.relative(face)));
    }

    @SuppressWarnings("unchecked")
    private <T> BlockApiCache<T, Direction> getEnergyCacheInternal(BlockApiLookup<T, Direction> cap, Direction side) {
        var capArr = this.cache.computeIfAbsent(cap, k -> new BlockApiCache[6]);
        int index = side.get3DDataValue();
        if (capArr[index] == null) {
            capArr[index] = BlockApiCache.create(cap, this.world, this.pos.relative(side));
        }
        return (BlockApiCache<T, Direction>) capArr[index];
    }

}
