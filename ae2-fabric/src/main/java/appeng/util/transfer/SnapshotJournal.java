package appeng.util.transfer;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

/**
 * Implements the semantics of NeoForge's SnapshotJournal on top of Fabric's {@link SnapshotParticipant}. AE2's
 * transactional inventories are written against these semantics: the state from before the outermost transaction is
 * passed to {@link #onRootCommit} once that transaction has been committed.
 */
public abstract class SnapshotJournal<T> {
    private final Participant participant = new Participant();

    /**
     * @return a new object containing a copy of this journal's current state.
     */
    protected abstract T createSnapshot();

    /**
     * Rolls back to a state previously created by {@link #createSnapshot}.
     */
    protected abstract void revertToSnapshot(T snapshot);

    /**
     * Signals that the snapshot will not be used anymore, and is safe to cache for future calls to
     * {@link #createSnapshot}, or discard entirely.
     */
    protected void releaseSnapshot(T snapshot) {
    }

    /**
     * Called after the outermost transaction was committed, with the state from before that transaction.
     */
    protected void onRootCommit(T originalState) {
    }

    /**
     * Must be called before this journal changes its state as part of the given transaction.
     */
    public void updateSnapshots(TransactionContext transaction) {
        participant.updateSnapshots(transaction);
    }

    private final class Participant extends SnapshotParticipant<T> {
        @Nullable
        private T originalState;
        private boolean capturingRootSnapshot;

        @Override
        protected T createSnapshot() {
            return SnapshotJournal.this.createSnapshot();
        }

        @Override
        protected void readSnapshot(T snapshot) {
            revertToSnapshot(snapshot);
        }

        @Override
        public void onClose(TransactionContext transaction, Transaction.Result result) {
            // When the outermost transaction is committed, SnapshotParticipant releases the original snapshot
            // right before scheduling onFinalCommit. Keep it around to pass it to onRootCommit.
            capturingRootSnapshot = !result.wasAborted() && transaction.nestingDepth() == 0;
            try {
                super.onClose(transaction, result);
            } finally {
                capturingRootSnapshot = false;
            }
        }

        @Override
        protected void releaseSnapshot(T snapshot) {
            if (capturingRootSnapshot && originalState == null) {
                originalState = snapshot;
            } else {
                SnapshotJournal.this.releaseSnapshot(snapshot);
            }
        }

        @Override
        protected void onFinalCommit() {
            var original = originalState;
            originalState = null;
            if (original != null) {
                onRootCommit(original);
                SnapshotJournal.this.releaseSnapshot(original);
            }
        }
    }
}
