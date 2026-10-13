package com.glodblock.github.appflux.common.me.strategy;

import appeng.api.behaviors.ExternalStorageStrategy;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.core.localization.GuiText;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import com.glodblock.github.appflux.common.me.key.type.FluxKeyType;
import com.glodblock.github.appflux.util.AFUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import team.reborn.energy.api.EnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class FEExternalStorageStrategy implements ExternalStorageStrategy {

    private final BlockApiCache<EnergyStorage, Direction> apiCache;
    private final Direction side;
    private final Direction fromSide;

    public FEExternalStorageStrategy(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        this.apiCache = BlockApiCache.create(EnergyStorage.SIDED, level, fromPos);
        this.side = fromSide;
        this.fromSide = fromSide;
    }

    @Override
    public @Nullable MEStorage createWrapper(boolean extractableOnly, Runnable callback) {
        var storage = this.apiCache.find(this.side);
        if (storage == null) {
            return null;
        }
        return new FEStorageWrapper(storage, this.fromSide, callback);
    }

    private record FEStorageWrapper(@Nullable EnergyStorage storage, Direction side, Runnable callback) implements MEStorage {

        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            if (FluxKey.of(EnergyType.FE).equals(what)) {
                if (this.storage != null) {
                    try (var trans = Transaction.openOuter()) {
                        long in = this.storage.insert(AFUtil.clampLong(amount), trans);
                        if (in > 0 && mode == Actionable.MODULATE) {
                            this.callback.run();
                            trans.commit();
                        }
                        return in;
                    }
                }
            }
            return 0;
        }

        @Override
        public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
            if (FluxKey.of(EnergyType.FE).equals(what)) {
                if (this.storage != null) {
                    try (var trans = Transaction.openOuter()) {
                        long out = this.storage.extract(AFUtil.clampLong(amount), trans);
                        if (out > 0 && mode == Actionable.MODULATE) {
                            this.callback.run();
                            trans.commit();
                        }
                        return out;
                    }
                }
            }
            return 0;
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            if (this.storage != null) {
                long stored = this.storage.getAmount();
                if (stored > 0) {
                    out.add(FluxKey.of(EnergyType.FE), stored);
                }
            }
        }

        @Override
        public Component getDescription() {
            return GuiText.ExternalStorage.text(FluxKeyType.TYPE.getDescription());
        }

    }
}
