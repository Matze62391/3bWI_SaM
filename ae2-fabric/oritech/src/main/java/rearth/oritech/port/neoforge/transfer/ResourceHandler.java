package rearth.oritech.port.neoforge.transfer;

import rearth.oritech.port.neoforge.transfer.resource.Resource;
import rearth.oritech.port.neoforge.transfer.transaction.TransactionContext;

/**
 * Indexed storage of resources. Amounts are items or millibuckets.
 */
public interface ResourceHandler<T extends Resource> {
    int size();

    T getResource(int index);

    long getAmountAsLong(int index);

    default int getAmountAsInt(int index) {
        return (int) Math.min(Integer.MAX_VALUE, getAmountAsLong(index));
    }

    long getCapacityAsLong(int index, T resource);

    default int getCapacityAsInt(int index, T resource) {
        return (int) Math.min(Integer.MAX_VALUE, getCapacityAsLong(index, resource));
    }

    boolean isValid(int index, T resource);

    int insert(int index, T resource, int amount, TransactionContext transaction);

    default int insert(T resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        var inserted = 0;
        // first top up indices that already hold the resource, then empty ones
        for (var pass = 0; pass < 2 && inserted < amount; pass++) {
            for (var i = 0; i < size() && inserted < amount; i++) {
                var current = getResource(i);
                var matching = pass == 0 ? current.equals(resource) : current.isEmpty();
                if (matching) {
                    inserted += insert(i, resource, amount - inserted, transaction);
                }
            }
        }
        return inserted;
    }

    int extract(int index, T resource, int amount, TransactionContext transaction);

    default int extract(T resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        var extracted = 0;
        for (var i = 0; i < size() && extracted < amount; i++) {
            if (getResource(i).equals(resource)) {
                extracted += extract(i, resource, amount - extracted, transaction);
            }
        }
        return extracted;
    }

    @SuppressWarnings("unchecked")
    static <T extends Resource> Class<ResourceHandler<T>> asClass() {
        return (Class<ResourceHandler<T>>) (Class<?>) ResourceHandler.class;
    }
}
