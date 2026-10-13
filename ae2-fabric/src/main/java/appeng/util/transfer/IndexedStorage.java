package appeng.util.transfer;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.google.common.primitives.Ints;

import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

/**
 * An index-based (slotted) storage. This mirrors the shape of NeoForge's ResourceHandler, which AE2's inventories are
 * implemented against, and exposes it as a Fabric {@link SlottedStorage}.
 */
public interface IndexedStorage<T extends TransferVariant<?>> extends SlottedStorage<T> {
    /**
     * @return the current number of indices in this storage.
     */
    int size();

    /**
     * @return the resource at the given index, which may be blank.
     */
    T getResource(int index);

    long getAmountAsLong(int index);

    default int getAmountAsInt(int index) {
        return Ints.saturatedCast(getAmountAsLong(index));
    }

    /**
     * @param resource The resource to get the capacity for. May be blank to get the general capacity at the index.
     */
    long getCapacityAsLong(int index, T resource);

    default int getCapacityAsInt(int index, T resource) {
        return Ints.saturatedCast(getCapacityAsLong(index, resource));
    }

    boolean isValid(int index, T resource);

    long insert(int index, T resource, long amount, TransactionContext transaction);

    long extract(int index, T resource, long amount, TransactionContext transaction);

    @Override
    default long insert(T resource, long amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        long inserted = 0;
        int size = size();
        for (int index = 0; index < size && inserted < amount; index++) {
            inserted += insert(index, resource, amount - inserted, transaction);
        }
        return inserted;
    }

    @Override
    default long extract(T resource, long amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        long extracted = 0;
        int size = size();
        for (int index = 0; index < size && extracted < amount; index++) {
            extracted += extract(index, resource, amount - extracted, transaction);
        }
        return extracted;
    }

    /**
     * Inserts into indices that already contain a resource first, and only then into empty indices.
     */
    default long insertStacking(T resource, long amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        long inserted = 0;
        int size = size();
        for (int index = 0; index < size && inserted < amount; index++) {
            if (!getResource(index).isBlank()) {
                inserted += insert(index, resource, amount - inserted, transaction);
            }
        }
        for (int index = 0; index < size && inserted < amount; index++) {
            if (getResource(index).isBlank()) {
                inserted += insert(index, resource, amount - inserted, transaction);
            }
        }
        return inserted;
    }

    @Override
    default int getSlotCount() {
        return size();
    }

    @Override
    default SingleSlotStorage<T> getSlot(int slot) {
        return new IndexSlot<>(this, slot);
    }

    @Override
    default Iterator<StorageView<T>> iterator() {
        return new Iterator<>() {
            private int index;

            @Override
            public boolean hasNext() {
                return index < size();
            }

            @Override
            public StorageView<T> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return getSlot(index++);
            }
        };
    }

    record IndexSlot<T extends TransferVariant<?>>(IndexedStorage<T> storage,
            int index) implements SingleSlotStorage<T> {
        @Override
        public long insert(T resource, long maxAmount, TransactionContext transaction) {
            return storage.insert(index, resource, maxAmount, transaction);
        }

        @Override
        public long extract(T resource, long maxAmount, TransactionContext transaction) {
            return storage.extract(index, resource, maxAmount, transaction);
        }

        @Override
        public boolean isResourceBlank() {
            return storage.getResource(index).isBlank();
        }

        @Override
        public T getResource() {
            return storage.getResource(index);
        }

        @Override
        public long getAmount() {
            return storage.getAmountAsLong(index);
        }

        @Override
        public long getCapacity() {
            return storage.getCapacityAsLong(index, storage.getResource(index));
        }
    }
}
