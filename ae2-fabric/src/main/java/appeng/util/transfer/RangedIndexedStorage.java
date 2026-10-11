package appeng.util.transfer;

import com.google.common.base.Preconditions;

import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

/**
 * Exposes a range of indices of another indexed storage.
 */
public class RangedIndexedStorage<T extends TransferVariant<?>> implements IndexedStorage<T> {
    private final IndexedStorage<T> delegate;
    private final int start;
    private final int end;

    public RangedIndexedStorage(IndexedStorage<T> delegate, int startInclusive, int endExclusive) {
        Preconditions.checkArgument(startInclusive >= 0 && startInclusive <= endExclusive);
        this.delegate = delegate;
        this.start = startInclusive;
        this.end = endExclusive;
    }

    private int map(int index) {
        if (index < 0 || index >= end - start) {
            throw new IndexOutOfBoundsException(index);
        }
        return start + index;
    }

    @Override
    public int size() {
        return end - start;
    }

    @Override
    public T getResource(int index) {
        return delegate.getResource(map(index));
    }

    @Override
    public long getAmountAsLong(int index) {
        return delegate.getAmountAsLong(map(index));
    }

    @Override
    public long getCapacityAsLong(int index, T resource) {
        return delegate.getCapacityAsLong(map(index), resource);
    }

    @Override
    public boolean isValid(int index, T resource) {
        return delegate.isValid(map(index), resource);
    }

    @Override
    public long insert(int index, T resource, long amount, TransactionContext transaction) {
        return delegate.insert(map(index), resource, amount, transaction);
    }

    @Override
    public long extract(int index, T resource, long amount, TransactionContext transaction) {
        return delegate.extract(map(index), resource, amount, transaction);
    }

    @Override
    public long insert(T resource, long amount, TransactionContext transaction) {
        return insertStacking(resource, amount, transaction);
    }
}
