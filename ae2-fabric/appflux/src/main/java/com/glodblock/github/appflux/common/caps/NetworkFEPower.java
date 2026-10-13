package com.glodblock.github.appflux.common.caps;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import com.glodblock.github.appflux.util.AFUtil;
import com.glodblock.github.appflux.util.DeltaEnergyJournal;
import com.glodblock.github.appflux.util.IOSignal;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import team.reborn.energy.api.EnergyStorage;

/**
 * The energy stored in an ME network, exposed by a flux accessor.
 */
public class NetworkFEPower implements EnergyStorage {

    private final IStorageService storage;
    private final IActionSource source;
    private final IOSignal signal;
    private final DeltaEnergyJournal journal;

    public NetworkFEPower(IStorageService storage, IActionSource source, IOSignal signal) {
        this.storage = storage;
        this.source = source;
        this.signal = signal;
        this.journal = new DeltaEnergyJournal(
                (amount, simulate) -> this.storage.getInventory().extract(FluxKey.of(EnergyType.FE), amount, AFUtil.ofSim(simulate), this.source),
                (amount, simulate) -> this.storage.getInventory().insert(FluxKey.of(EnergyType.FE), amount, AFUtil.ofSim(simulate), this.source)
        );
    }

    @Override
    public boolean supportsInsertion() {
        return this.signal.isInput();
    }

    @Override
    public boolean supportsExtraction() {
        return this.signal.isOutput();
    }

    @Override
    public long insert(long amount, TransactionContext transaction) {
        if (this.signal.isInput()) {
            StoragePreconditions.notNegative(amount);
            this.journal.updateSnapshots(transaction);
            return this.journal.onInsert(amount);
        } else {
            return 0;
        }
    }

    @Override
    public long extract(long amount, TransactionContext transaction) {
        if (this.signal.isOutput()) {
            StoragePreconditions.notNegative(amount);
            this.journal.updateSnapshots(transaction);
            return this.journal.onExtract(amount);
        } else {
            return 0;
        }
    }

    @Override
    public long getAmount() {
        return this.storage.getCachedInventory().get(FluxKey.of(EnergyType.FE));
    }

    @Override
    public long getCapacity() {
        var space = this.storage.getInventory().insert(FluxKey.of(EnergyType.FE), Long.MAX_VALUE - 1, Actionable.SIMULATE, this.source);
        return space + this.getAmount();
    }
}
