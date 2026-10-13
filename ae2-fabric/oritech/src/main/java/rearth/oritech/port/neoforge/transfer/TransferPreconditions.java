package rearth.oritech.port.neoforge.transfer;

import rearth.oritech.port.neoforge.transfer.resource.Resource;

public final class TransferPreconditions {
    private TransferPreconditions() {}

    public static void checkNonEmpty(Resource resource) {
        if (resource.isEmpty()) {
            throw new IllegalArgumentException("Expected resource to be non-empty: " + resource);
        }
    }

    public static void checkNonNegative(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Expected value to be non-negative: " + value);
        }
    }

    public static void checkNonEmptyNonNegative(Resource resource, int value) {
        checkNonEmpty(resource);
        checkNonNegative(value);
    }
}
