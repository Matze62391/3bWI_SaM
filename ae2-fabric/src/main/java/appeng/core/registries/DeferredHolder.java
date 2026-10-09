package appeng.core.registries;

import java.util.Objects;
import java.util.function.Supplier;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

/**
 * A reference to a registry entry that is registered by {@link DeferredRegister#register()}. Replacement for the
 * NeoForge class of the same name.
 * <p>
 * Unlike NeoForge's class, this does not implement {@link Holder} itself (it is sealed in vanilla), use
 * {@link #getDelegate()} to get the registry's holder once the entry has been registered.
 */
public class DeferredHolder<R, T extends R> implements Supplier<T> {
    private final ResourceKey<R> key;
    private Holder.Reference<R> delegate;

    protected DeferredHolder(ResourceKey<R> key) {
        this.key = Objects.requireNonNull(key);
    }

    void bind(Holder.Reference<R> reference) {
        this.delegate = reference;
    }

    /**
     * @return The holder of the registered entry.
     */
    public Holder<R> getDelegate() {
        if (delegate == null) {
            throw new IllegalStateException("Registry entry not present yet: " + key);
        }
        return delegate;
    }

    @SuppressWarnings("unchecked")
    public T value() {
        return (T) getDelegate().value();
    }

    @Override
    public T get() {
        return value();
    }

    public Identifier getId() {
        return key.identifier();
    }

    public ResourceKey<R> getKey() {
        return key;
    }

    public boolean isBound() {
        return delegate != null && delegate.isBound();
    }

    public boolean is(TagKey<R> tag) {
        return getDelegate().is(tag);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof DeferredHolder<?, ?> other && key.equals(other.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "DeferredHolder{" + key + "}";
    }

    @SuppressWarnings("unchecked")
    static <R> Registry<R> lookupRegistry(ResourceKey<? extends Registry<R>> registryKey) {
        var registry = net.minecraft.core.registries.BuiltInRegistries.REGISTRY.getValue(registryKey.identifier());
        if (registry == null) {
            throw new IllegalStateException("Unknown registry " + registryKey);
        }
        return (Registry<R>) registry;
    }
}
