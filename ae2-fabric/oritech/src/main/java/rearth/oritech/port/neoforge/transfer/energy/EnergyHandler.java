package rearth.oritech.port.neoforge.transfer.energy;

import rearth.oritech.port.neoforge.transfer.transaction.TransactionContext;

/**
 * Stores energy (FE on NeoForge, E of Team Reborn Energy on Fabric).
 */
public interface EnergyHandler {
    long getAmountAsLong();

    default int getAmountAsInt() {
        return (int) Math.min(Integer.MAX_VALUE, getAmountAsLong());
    }

    long getCapacityAsLong();

    default int getCapacityAsInt() {
        return (int) Math.min(Integer.MAX_VALUE, getCapacityAsLong());
    }

    int insert(int amount, TransactionContext transaction);

    int extract(int amount, TransactionContext transaction);
}
