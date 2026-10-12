package rearth.oritech.port.neoforge.transfer.transaction;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext.Result;

/**
 * Keeps one snapshot per open transaction level and reverts to it when a transaction is aborted. When the outermost
 * transaction is committed, {@link #onRootCommit} is called with the state before it.
 */
public abstract class SnapshotJournal<T> implements net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext.CloseCallback {
    private final List<T> snapshots = new ArrayList<>();

    protected abstract T createSnapshot();

    protected abstract void revertToSnapshot(T snapshot);

    protected void releaseSnapshot(T snapshot) {
    }

    protected void onRootCommit(T originalState) {
    }

    public final boolean isInTransaction() {
        return !snapshots.isEmpty();
    }

    public void updateSnapshots(TransactionContext transaction) {
        updateSnapshots(transaction.fabric());
    }

    public void updateSnapshots(net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction) {
        var depth = transaction.nestingDepth();
        while (snapshots.size() <= depth) {
            snapshots.add(null);
        }
        if (snapshots.get(depth) == null) {
            snapshots.set(depth, createSnapshot());
            transaction.addCloseCallback(this);
        }
    }

    @Override
    public void onClose(net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction, Result result) {
        var depth = transaction.nestingDepth();
        var snapshot = snapshots.set(depth, null);
        if (result.wasAborted()) {
            revertToSnapshot(snapshot);
            releaseSnapshot(snapshot);
        } else if (depth > 0) {
            if (snapshots.get(depth - 1) == null) {
                snapshots.set(depth - 1, snapshot);
                transaction.getOpenTransaction(depth - 1).addCloseCallback(this);
            } else {
                releaseSnapshot(snapshot);
            }
        } else {
            transaction.addOuterCloseCallback(outerResult -> {
                if (outerResult.wasCommitted()) {
                    onRootCommit(snapshot);
                }
                releaseSnapshot(snapshot);
            });
        }
        while (!snapshots.isEmpty() && snapshots.getLast() == null) {
            snapshots.removeLast();
        }
    }
}
