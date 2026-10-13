package com.glodblock.github.appflux.common.me.strategy;

import appeng.api.behaviors.StackExportStrategy;
import appeng.api.behaviors.StackTransferContext;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageHelper;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.util.AFUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import team.reborn.energy.api.EnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class FEStackExportStrategy implements StackExportStrategy {
    private final BlockApiCache<EnergyStorage, Direction> apiCache;
    private final Direction side;

    public FEStackExportStrategy(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        this.apiCache = BlockApiCache.create(EnergyStorage.SIDED, level, fromPos);
        this.side = fromSide;
    }

    @Override
    public long transfer(StackTransferContext context, AEKey what, long amount) {
        if (!(what instanceof FluxKey)) {
            return 0;
        }
        var storage = this.apiCache.find(this.side);
        if (storage == null) {
            return 0;
        }

        var inv = context.getInternalStorage();

        var toAdd = StorageHelper.poweredExtraction(context.getEnergySource(), inv.getInventory(), what, amount, context.getActionSource(), Actionable.SIMULATE);
        long realExt = 0;

        try (var trans = Transaction.openOuter()) {
            toAdd = storage.insert(AFUtil.clampLong(toAdd), trans);
            if (toAdd > 0) {
                realExt = (StorageHelper.poweredExtraction(context.getEnergySource(), inv.getInventory(), what, toAdd, context.getActionSource(), Actionable.MODULATE));
            }
        }

        if (realExt > 0) {
            try (var trans = Transaction.openOuter()) {
                storage.insert(realExt, trans);
                trans.commit();
                return realExt;
            }
        }
        return 0;
    }

    @Override
    public long push(AEKey what, long amount, Actionable mode) {
        if (!(what instanceof FluxKey)) {
            return 0;
        }
        var storage = this.apiCache.find(this.side);
        if (storage == null) {
            return 0;
        }

        try (var trans = Transaction.openOuter()) {
            var added = storage.insert(AFUtil.clampLong(amount), trans);
            if (!mode.isSimulate()) {
                trans.commit();
            }
            return added;
        }
    }
}
