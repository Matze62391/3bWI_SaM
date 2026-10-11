package com.glodblock.github.appflux.common.me.energy;

import appeng.api.config.Actionable;
import appeng.api.config.PowerUnit;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import com.glodblock.github.appflux.config.AFConfig;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.Direction;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import team.reborn.energy.api.EnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class UniversalEnergyHandler {

    private static final ArrayList<Pair<BlockApiLookup<?, Direction>, Handler<?>>> HANDLERS = new ArrayList<>();
    private static final Handler<EnergyStorage> DEFAULT = (accepter, storage, source) -> {
        if (!accepter.supportsInsertion()) {
            return 0;
        }
        try (var transaction = Transaction.openOuter()) {
            var toAdd = Math.min(accepter.getCapacity() - accepter.getAmount(), AFConfig.getFluxAccessorIO());
            if (toAdd > 0) {
                var drained = storage.getInventory().extract(FluxKey.of(EnergyType.FE), toAdd, Actionable.MODULATE, source);
                if (drained > 0) {
                    var actuallyDrained = accepter.insert(drained, transaction);
                    var differ = drained - actuallyDrained;
                    if (differ > 0) {
                        storage.getInventory().insert(FluxKey.of(EnergyType.FE), differ, Actionable.MODULATE, source);
                    }
                    transaction.commit();
                    return actuallyDrained;
                }
            }
        }
        return 0;
    };

    public static <T> void addHandler(BlockApiLookup<T, Direction> cap, Handler<T> handler) {
        HANDLERS.add(Pair.of(cap, handler));
    }

    // -1 means this side has no valid energy accepter
    @SuppressWarnings("unchecked")
    public static <T> long send(@NotNull EnergyCapCache cache, Direction side, @NotNull IStorageService storage, @NotNull IActionSource source) {
        for (var entry : HANDLERS) {
            T cap = cache.getEnergyCap((BlockApiLookup<T, Direction>) entry.left(), side);
            if (cap != null) {
                return ((Handler<T>) entry.right()).send(cap, storage, source);
            }
        }
        var cap = cache.getEnergyCap(EnergyStorage.SIDED, side);
        if (cap != null) {
            return DEFAULT.send(cap, storage, source);
        }
        return -1;
    }

    public static void chargeNetwork(@NotNull IEnergyService energy, @NotNull IStorageService storage, @NotNull IActionSource source) {
        var toAdd = Math.floor(Integer.MAX_VALUE - energy.injectPower(Integer.MAX_VALUE, Actionable.SIMULATE));
        var toDrain = storage.getInventory().extract(FluxKey.of(EnergyType.FE), (long) PowerUnit.AE.convertTo(PowerUnit.TR, toAdd), Actionable.MODULATE, source);
        energy.injectPower(toDrain, Actionable.MODULATE);
    }

    public interface Handler<T> {

        long send(@NotNull T cap, @NotNull IStorageService storage, @NotNull IActionSource source);

    }

}
