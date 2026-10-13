package appeng.api.model;

import java.util.function.Predicate;

/**
 * A typed key for a value in {@link ModelData}. Compared by identity. Replaces NeoForge's class of the same name.
 */
public final class ModelProperty<T> implements Predicate<T> {
    private final Predicate<T> predicate;

    public ModelProperty() {
        this(value -> true);
    }

    public ModelProperty(Predicate<T> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean test(T value) {
        return predicate.test(value);
    }
}
