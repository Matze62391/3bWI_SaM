package rearth.oritech.port.neoforge.transfer.transaction;

/**
 * A transaction that is open, backed by a Fabric transaction.
 */
public interface TransactionContext {
    int depth();

    net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext fabric();

    /**
     * Wraps a transaction that Fabric API (or another mod) opened.
     */
    static TransactionContext of(net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext fabric) {
        return new TransactionContext() {
            @Override
            public int depth() {
                return fabric.nestingDepth();
            }

            @Override
            public net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext fabric() {
                return fabric;
            }
        };
    }
}
