package rearth.oritech.port.neoforge.transfer.transaction;

import org.jetbrains.annotations.Nullable;

/**
 * Opens Fabric transactions. Closing without {@link #commit()} aborts the transaction.
 */
public final class Transaction implements TransactionContext, AutoCloseable {
    private final net.fabricmc.fabric.api.transfer.v1.transaction.Transaction fabric;

    private Transaction(net.fabricmc.fabric.api.transfer.v1.transaction.Transaction fabric) {
        this.fabric = fabric;
    }

    public static Transaction openRoot() {
        return new Transaction(net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter());
    }

    public static Transaction open(@Nullable TransactionContext parent) {
        if (parent != null) {
            return new Transaction(parent.fabric().openNested());
        }
        return new Transaction(net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openNested(
                net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.getCurrentUnsafe()));
    }

    @Nullable
    public static TransactionContext getCurrentOpenedTransaction() {
        var current = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.getCurrentUnsafe();
        return current == null ? null : TransactionContext.of(current);
    }

    public void commit() {
        fabric.commit();
    }

    @Override
    public void close() {
        fabric.close();
    }

    @Override
    public int depth() {
        return fabric.nestingDepth();
    }

    @Override
    public net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext fabric() {
        return fabric;
    }
}
