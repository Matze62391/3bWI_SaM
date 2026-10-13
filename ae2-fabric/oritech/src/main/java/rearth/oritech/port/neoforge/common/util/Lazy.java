package rearth.oritech.port.neoforge.common.util;

import java.util.function.Supplier;

public final class Lazy<T> implements Supplier<T> {
    private Supplier<T> supplier;
    private T value;

    private Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public static <T> Lazy<T> of(Supplier<T> supplier) {
        return new Lazy<>(supplier);
    }

    @Override
    public synchronized T get() {
        if (supplier != null) {
            value = supplier.get();
            supplier = null;
        }
        return value;
    }
}
