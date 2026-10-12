package rearth.oritech.port.neoforge.transfer.energy;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import rearth.oritech.port.neoforge.common.util.ValueIOSerializable;
import rearth.oritech.port.neoforge.transfer.TransferPreconditions;
import rearth.oritech.port.neoforge.transfer.transaction.SnapshotJournal;
import rearth.oritech.port.neoforge.transfer.transaction.TransactionContext;

public class SimpleEnergyHandler extends SnapshotJournal<Integer> implements EnergyHandler, ValueIOSerializable {
    protected int energy;
    protected int capacity;
    protected int maxInsert;
    protected int maxExtract;

    public SimpleEnergyHandler(int capacity) {
        this(capacity, capacity);
    }

    public SimpleEnergyHandler(int capacity, int maxTransfer) {
        this(capacity, maxTransfer, maxTransfer);
    }

    public SimpleEnergyHandler(int capacity, int maxInsert, int maxExtract) {
        this(capacity, maxInsert, maxExtract, 0);
    }

    public SimpleEnergyHandler(int capacity, int maxInsert, int maxExtract, int energy) {
        this.capacity = capacity;
        this.maxInsert = maxInsert;
        this.maxExtract = maxExtract;
        this.energy = energy;
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putInt("energy", energy);
    }

    @Override
    public void deserialize(ValueInput input) {
        energy = input.getIntOr("energy", 0);
    }

    public void set(int amount) {
        var previous = energy;
        energy = amount;
        if (previous != amount) {
            onEnergyChanged(previous);
        }
    }

    protected void onEnergyChanged(int previousAmount) {
    }

    @Override
    public long getAmountAsLong() {
        return energy;
    }

    @Override
    public long getCapacityAsLong() {
        return capacity;
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        var inserted = Math.min(Math.min(amount, maxInsert), capacity - energy);
        if (inserted > 0) {
            updateSnapshots(transaction);
            energy += inserted;
            return inserted;
        }
        return 0;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        var extracted = Math.min(Math.min(amount, maxExtract), energy);
        if (extracted > 0) {
            updateSnapshots(transaction);
            energy -= extracted;
            return extracted;
        }
        return 0;
    }

    @Override
    protected Integer createSnapshot() {
        return energy;
    }

    @Override
    protected void revertToSnapshot(Integer snapshot) {
        energy = snapshot;
    }

    @Override
    protected void onRootCommit(Integer originalState) {
        if (originalState != energy) {
            onEnergyChanged(originalState);
        }
    }
}
