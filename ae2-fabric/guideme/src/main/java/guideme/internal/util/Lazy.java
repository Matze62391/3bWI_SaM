package guideme.internal.util;

import java.util.Objects;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

/**
 * A value that is computed on first access and can be invalidated to be recomputed on the next access. Replaces the
 * NeoForge class of the same name.
 */
public final class Lazy<T> implements Supplier<T> {
    private final Supplier<T> factory;
    @Nullable
    private volatile T value;

    private Lazy(Supplier<T> factory) {
        this.factory = Objects.requireNonNull(factory);
    }

    public static <T> Lazy<T> of(Supplier<T> factory) {
        return new Lazy<>(factory);
    }

    @Override
    public T get() {
        var result = value;
        if (result == null) {
            synchronized (this) {
                result = value;
                if (result == null) {
                    result = factory.get();
                    value = result;
                }
            }
        }
        return result;
    }

    public void invalidate() {
        value = null;
    }
}
