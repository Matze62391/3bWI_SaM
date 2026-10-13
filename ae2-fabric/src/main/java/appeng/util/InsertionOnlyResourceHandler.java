package appeng.util;

import appeng.util.transfer.IndexedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

public abstract class InsertionOnlyResourceHandler<T extends TransferVariant<?>> implements IndexedStorage<T> {
    private final T emptyResource;

    public InsertionOnlyResourceHandler(T emptyResource) {
        this.emptyResource = emptyResource;
    }

    @Override
    public final int size() {
        return 1; // Has to have 1 slot at least so that people pointlessly doing size() checks will still work
    }

    @Override
    public final T getResource(int index) {
        return emptyResource;
    }

    @Override
    public final long getAmountAsLong(int index) {
        return 0L; // Always appears empty
    }

    @Override
    public final long getCapacityAsLong(int index, T resource) {
        return getCapacity(resource);
    }

    protected long getCapacity(T resource) {
        return Long.MAX_VALUE;
    }

    @Override
    public final boolean isValid(int index, T resource) {
        return isValid(resource);
    }

    protected boolean isValid(T resource) {
        return true;
    }

    @Override
    public final long insert(int index, T resource, long amount, TransactionContext transaction) {
        return insert(resource, amount, transaction);
    }

    @Override
    public abstract long insert(T resource, long amount, TransactionContext transaction);

    @Override
    public final long extract(int index, T resource, long amount, TransactionContext transaction) {
        return 0;
    }
}
