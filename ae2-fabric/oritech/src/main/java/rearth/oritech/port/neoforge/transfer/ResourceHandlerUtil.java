package rearth.oritech.port.neoforge.transfer;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import rearth.oritech.port.neoforge.transfer.resource.Resource;
import rearth.oritech.port.neoforge.transfer.resource.ResourceStack;
import rearth.oritech.port.neoforge.transfer.transaction.Transaction;
import rearth.oritech.port.neoforge.transfer.transaction.TransactionContext;

public final class ResourceHandlerUtil {
    private ResourceHandlerUtil() {}

    public static boolean isEmpty(Resource resource, int amount) {
        return resource.isEmpty() || amount <= 0;
    }

    public static boolean isEmpty(ResourceHandler<? extends Resource> handler) {
        for (var i = 0; i < handler.size(); i++) {
            if (!handler.getResource(i).isEmpty() && handler.getAmountAsLong(i) > 0) {
                return false;
            }
        }
        return true;
    }

    public static <T extends Resource> boolean isFull(ResourceHandler<T> handler) {
        for (var i = 0; i < handler.size(); i++) {
            var resource = handler.getResource(i);
            if (resource.isEmpty() || handler.getAmountAsLong(i) < handler.getCapacityAsLong(i, resource)) {
                return false;
            }
        }
        return true;
    }

    public static <T extends Resource> boolean contains(ResourceHandler<T> handler, T resource) {
        return indexOf(handler, resource) >= 0;
    }

    public static <T extends Resource> int indexOf(ResourceHandler<T> handler, T resource) {
        for (var i = 0; i < handler.size(); i++) {
            if (handler.getResource(i).equals(resource)) {
                return i;
            }
        }
        return -1;
    }

    public static <T extends Resource> int getRedstoneSignalFromResourceHandler(ResourceHandler<T> handler) {
        if (handler.size() == 0) {
            return 0;
        }
        var fill = 0.0;
        for (var i = 0; i < handler.size(); i++) {
            var resource = handler.getResource(i);
            var capacity = handler.getCapacityAsLong(i, resource);
            if (capacity > 0) {
                fill += (double) handler.getAmountAsLong(i) / capacity;
            }
        }
        fill /= handler.size();
        return (int) Math.floor(fill * 14) + (fill > 0 ? 1 : 0);
    }

    @Nullable
    public static <T extends Resource> T findExtractableResource(ResourceHandler<T> handler, Predicate<T> filter,
            @Nullable TransactionContext transaction) {
        for (var i = 0; i < handler.size(); i++) {
            var resource = handler.getResource(i);
            if (resource.isEmpty() || !filter.test(resource)) {
                continue;
            }
            try (var tx = Transaction.open(transaction)) {
                if (handler.extract(i, resource, 1, tx) > 0) {
                    return resource;
                }
            }
        }
        return null;
    }

    public static <T extends Resource> int move(@Nullable ResourceHandler<T> from, @Nullable ResourceHandler<T> to,
            Predicate<T> filter, int amount, @Nullable TransactionContext transaction) {
        if (from == null || to == null || amount <= 0) {
            return 0;
        }
        var moved = 0;
        try (var tx = Transaction.open(transaction)) {
            for (var i = 0; i < from.size() && moved < amount; i++) {
                var resource = from.getResource(i);
                if (resource.isEmpty() || !filter.test(resource)) {
                    continue;
                }
                moved += moveIndex(from, i, to, resource, amount - moved, tx);
            }
            tx.commit();
        }
        return moved;
    }

    public static <T extends Resource> int moveStacking(@Nullable ResourceHandler<T> from, @Nullable ResourceHandler<T> to,
            Predicate<T> filter, int amount, @Nullable TransactionContext transaction) {
        return move(from, to, filter, amount, transaction);
    }

    @Nullable
    public static <T extends Resource> ResourceStack<T> moveFirst(@Nullable ResourceHandler<T> from,
            @Nullable ResourceHandler<T> to, Predicate<T> filter, int amount, @Nullable TransactionContext transaction) {
        if (from == null || to == null || amount <= 0) {
            return null;
        }
        for (var i = 0; i < from.size(); i++) {
            var resource = from.getResource(i);
            if (resource.isEmpty() || !filter.test(resource)) {
                continue;
            }
            try (var tx = Transaction.open(transaction)) {
                var moved = moveIndex(from, i, to, resource, amount, tx);
                if (moved > 0) {
                    tx.commit();
                    return new ResourceStack<>(resource, moved);
                }
            }
        }
        return null;
    }

    @Nullable
    public static <T extends Resource> ResourceStack<T> moveFirstStacking(@Nullable ResourceHandler<T> from,
            @Nullable ResourceHandler<T> to, Predicate<T> filter, int amount, @Nullable TransactionContext transaction) {
        return moveFirst(from, to, filter, amount, transaction);
    }

    public static <T extends Resource> int insertStacking(ResourceHandler<T> handler, T resource, int amount,
            TransactionContext transaction) {
        return handler.insert(resource, amount, transaction);
    }

    @Nullable
    public static <T extends Resource> ResourceStack<T> extractFirst(ResourceHandler<T> handler, Predicate<T> filter,
            int amount, TransactionContext transaction) {
        for (var i = 0; i < handler.size(); i++) {
            var resource = handler.getResource(i);
            if (resource.isEmpty() || !filter.test(resource)) {
                continue;
            }
            var extracted = handler.extract(i, resource, amount, transaction);
            if (extracted > 0) {
                return new ResourceStack<>(resource, extracted);
            }
        }
        return null;
    }

    private static <T extends Resource> int moveIndex(ResourceHandler<T> from, int index, ResourceHandler<T> to, T resource,
            int amount, TransactionContext transaction) {
        int accepted;
        try (var tx = Transaction.open(transaction)) {
            var extracted = from.extract(index, resource, amount, tx);
            if (extracted <= 0) {
                return 0;
            }
            accepted = to.insert(resource, extracted, tx);
            if (accepted == extracted) {
                tx.commit();
                return accepted;
            }
        }
        if (accepted <= 0) {
            return 0;
        }
        // the target took less: move exactly that much
        try (var tx = Transaction.open(transaction)) {
            var extracted = from.extract(index, resource, accepted, tx);
            var inserted = to.insert(resource, extracted, tx);
            if (inserted == extracted) {
                tx.commit();
                return inserted;
            }
        }
        return 0;
    }
}
