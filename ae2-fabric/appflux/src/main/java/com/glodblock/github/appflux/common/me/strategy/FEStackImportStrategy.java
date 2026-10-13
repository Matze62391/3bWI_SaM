package com.glodblock.github.appflux.common.me.strategy;

import appeng.api.behaviors.StackImportStrategy;
import appeng.api.behaviors.StackTransferContext;
import appeng.api.config.Actionable;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import com.glodblock.github.appflux.common.me.key.type.FluxKeyType;
import com.glodblock.github.appflux.util.AFUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import team.reborn.energy.api.EnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class FEStackImportStrategy implements StackImportStrategy {

    private final BlockApiCache<EnergyStorage, Direction> apiCache;
    private final Direction side;

    public FEStackImportStrategy(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        this.apiCache = BlockApiCache.create(EnergyStorage.SIDED, level, fromPos);
        this.side = fromSide;
    }

    @Override
    public boolean transfer(StackTransferContext context) {
        if (!context.isKeyTypeEnabled(FluxKeyType.TYPE)) {
            return false;
        }

        var adjacentHandler = this.apiCache.find(this.side);
        var resource = FluxKey.of(EnergyType.FE);

        if (adjacentHandler == null) {
            return false;
        }
        if (!context.isInFilter(resource)) {
            return false;
        }

        var remainingTransferAmount = context.getOperationsRemaining() * (long) FluxKeyType.TYPE.getAmountPerOperation();
        var inv = context.getInternalStorage();
        var toExt = inv.getInventory().insert(resource, remainingTransferAmount, Actionable.SIMULATE, context.getActionSource());
        long realAdd = 0;

        try (var trans = Transaction.openOuter()) {
            toExt = adjacentHandler.extract(AFUtil.clampLong(toExt), trans);
            if (toExt > 0) {
                realAdd = (inv.getInventory().insert(resource, toExt, Actionable.MODULATE, context.getActionSource()));
            }
        }

        if (realAdd > 0) {
            try (var trans = Transaction.openOuter()) {
                adjacentHandler.extract(realAdd, trans);
                trans.commit();
            }
            var opsUsed = Math.max(1, realAdd / FluxKeyType.TYPE.getAmountPerOperation());
            context.reduceOperationsRemaining(opsUsed);
        }
        return false;
    }
}
