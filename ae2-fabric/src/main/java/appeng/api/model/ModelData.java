package appeng.api.model;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;

import org.jetbrains.annotations.Nullable;

/**
 * An immutable, typed property map that block entities hand to their dynamic models (exposed through Fabric's
 * {@code RenderDataBlockEntity}). Replaces NeoForge's class of the same name.
 */
public final class ModelData {
    public static final ModelData EMPTY = new ModelData(Map.of());

    private final Map<ModelProperty<?>, Object> properties;

    private ModelData(Map<ModelProperty<?>, Object> properties) {
        this.properties = properties;
    }

    public Set<ModelProperty<?>> getProperties() {
        return Collections.unmodifiableSet(properties.keySet());
    }

    public boolean has(ModelProperty<?> property) {
        return properties.containsKey(property);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T get(ModelProperty<T> property) {
        return (T) properties.get(property);
    }

    public Builder derive() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder(null);
    }

    public static <T> ModelData of(ModelProperty<T> property, T value) {
        return builder().with(property, value).build();
    }

    /**
     * @return The model data from Fabric's render data object, or {@link #EMPTY} if it is not model data.
     */
    public static ModelData of(@Nullable Object renderData) {
        return renderData instanceof ModelData modelData ? modelData : EMPTY;
    }

    @Override
    public String toString() {
        return "ModelData" + properties;
    }

    public static final class Builder {
        private final Map<ModelProperty<?>, Object> properties = new IdentityHashMap<>();

        private Builder(@Nullable ModelData parent) {
            if (parent != null) {
                properties.putAll(parent.properties);
            }
        }

        public <T> Builder with(ModelProperty<T> property, T value) {
            Preconditions.checkState(property.test(value), "The provided value is invalid for this property.");
            properties.put(property, value);
            return this;
        }

        public ModelData build() {
            if (properties.isEmpty()) {
                return EMPTY;
            }
            return new ModelData(new IdentityHashMap<>(properties));
        }
    }
}
