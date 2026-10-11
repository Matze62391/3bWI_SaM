package appeng.util.transfer;

import java.util.List;

import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

/**
 * Exposes several indexed storages as one, keeping the indices of each part. Inserting and extracting without an
 * index is delegated to each part in order, so that each part can apply its own stacking logic.
 */
public class CombinedIndexedStorage<T extends TransferVariant<?>> implements IndexedStorage<T> {
    private final List<IndexedStorage<T>> parts;

    public CombinedIndexedStorage(List<IndexedStorage<T>> parts) {
        this.parts = List.copyOf(parts);
    }

    @Override
    public int size() {
        int size = 0;
        for (var part : parts) {
            size += part.size();
        }
        return size;
    }

    @Override
    public T getResource(int index) {
        for (var part : parts) {
            if (index < part.size()) {
                return part.getResource(index);
            }
            index -= part.size();
        }
        throw new IndexOutOfBoundsException(index);
    }

    @Override
    public long getAmountAsLong(int index) {
        for (var part : parts) {
            if (index < part.size()) {
                return part.getAmountAsLong(index);
            }
            index -= part.size();
        }
        throw new IndexOutOfBoundsException(index);
    }

    @Override
    public long getCapacityAsLong(int index, T resource) {
        for (var part : parts) {
            if (index < part.size()) {
                return part.getCapacityAsLong(index, resource);
            }
            index -= part.size();
        }
        throw new IndexOutOfBoundsException(index);
    }

    @Override
    public boolean isValid(int index, T resource) {
        for (var part : parts) {
            if (index < part.size()) {
                return part.isValid(index, resource);
            }
            index -= part.size();
        }
        throw new IndexOutOfBoundsException(index);
    }

    @Override
    public long insert(int index, T resource, long amount, TransactionContext transaction) {
        for (var part : parts) {
            if (index < part.size()) {
                return part.insert(index, resource, amount, transaction);
            }
            index -= part.size();
        }
        throw new IndexOutOfBoundsException(index);
    }

    @Override
    public long extract(int index, T resource, long amount, TransactionContext transaction) {
        for (var part : parts) {
            if (index < part.size()) {
                return part.extract(index, resource, amount, transaction);
            }
            index -= part.size();
        }
        throw new IndexOutOfBoundsException(index);
    }

    @Override
    public long insert(T resource, long amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        long inserted = 0;
        for (var part : parts) {
            if (inserted >= amount) {
                break;
            }
            inserted += part.insert(resource, amount - inserted, transaction);
        }
        return inserted;
    }

    @Override
    public long extract(T resource, long amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        long extracted = 0;
        for (var part : parts) {
            if (extracted >= amount) {
                break;
            }
            extracted += part.extract(resource, amount - extracted, transaction);
        }
        return extracted;
    }
}
