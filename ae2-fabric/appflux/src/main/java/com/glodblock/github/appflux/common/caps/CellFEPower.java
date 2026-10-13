package com.glodblock.github.appflux.common.caps;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import com.glodblock.github.appflux.common.me.cell.FluxCellInventory;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import team.reborn.energy.api.EnergyStorage;

/**
 * The energy stored in a flux cell item, for other mods' chargers and machines.
 */
public class CellFEPower implements EnergyStorage {

    private final FluxCellInventory inv;

    public CellFEPower(FluxCellInventory inv) {
        this.inv = inv;
    }

    @Override
    public long getAmount() {
        return this.inv.getStoredEnergy();
    }

    @Override
    public long getCapacity() {
        return this.inv.getMaxEnergy();
    }

    @Override
    public long insert(long amount, TransactionContext transaction) {
        StoragePreconditions.notNegative(amount);
        this.inv.getJournal().updateSnapshots(transaction);
        return this.inv.insert(FluxKey.of(EnergyType.FE), amount, Actionable.MODULATE, IActionSource.empty());
    }

    @Override
    public long extract(long amount, TransactionContext transaction) {
        StoragePreconditions.notNegative(amount);
        this.inv.getJournal().updateSnapshots(transaction);
        return this.inv.extract(FluxKey.of(EnergyType.FE), amount, Actionable.MODULATE, IActionSource.empty());
    }
}
