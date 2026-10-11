package appeng.util.transfer;

import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;

/**
 * Argument checks for transfer operations, mirroring NeoForge's TransferPreconditions.
 */
public final class TransferPreconditions {
    private TransferPreconditions() {
    }

    public static void checkNonNegative(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Expected value to be non-negative: " + value);
        }
    }

    public static void checkNonEmpty(TransferVariant<?> resource) {
        if (resource.isBlank()) {
            throw new IllegalArgumentException("Expected resource to be non-empty: " + resource);
        }
    }

    public static void checkNonEmptyNonNegative(TransferVariant<?> resource, long value) {
        checkNonEmpty(resource);
        checkNonNegative(value);
    }
}
