package appeng.core.registries;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Either;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

/**
 * A holder for a registry entry that is registered by {@link DeferredRegister#register()}. Replacement for the NeoForge
 * class of the same name. Once registered, all {@link Holder} methods delegate to the registry's reference holder.
 */
public class DeferredHolder<R, T extends R> implements Holder<R>, Supplier<T> {
    private final ResourceKey<R> key;
    private Holder.Reference<R> delegate;

    protected DeferredHolder(ResourceKey<R> key) {
        this.key = Objects.requireNonNull(key);
    }

    void bind(Holder.Reference<R> reference) {
        this.delegate = reference;
    }

    private Holder.Reference<R> delegate() {
        if (delegate == null) {
            throw new IllegalStateException("Registry entry not present yet: " + key);
        }
        return delegate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T value() {
        return (T) delegate().value();
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

    public Holder<R> getDelegate() {
        return delegate();
    }

    @Override
    public boolean isBound() {
        return delegate != null && delegate.isBound();
    }

    @Override
    public boolean areComponentsBound() {
        return delegate != null && delegate.areComponentsBound();
    }

    @Override
    public DataComponentMap components() {
        return delegate().components();
    }

    @Override
    public boolean is(Identifier id) {
        return key.identifier().equals(id);
    }

    @Override
    public boolean is(ResourceKey<R> key) {
        return this.key.equals(key);
    }

    @Override
    public boolean is(Predicate<ResourceKey<R>> filter) {
        return filter.test(key);
    }

    @Override
    public boolean is(TagKey<R> tag) {
        return delegate().is(tag);
    }

    @Override
    public boolean is(Holder<R> holder) {
        return holder.is(key);
    }

    @Override
    public Stream<TagKey<R>> tags() {
        return delegate().tags();
    }

    @Override
    public Either<ResourceKey<R>, R> unwrap() {
        return Either.left(key);
    }

    @Override
    public Optional<ResourceKey<R>> unwrapKey() {
        return Optional.of(key);
    }

    @Override
    public Kind kind() {
        return Kind.REFERENCE;
    }

    @Override
    public boolean canSerializeIn(HolderOwner<R> owner) {
        return delegate().canSerializeIn(owner);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof Holder<?> h && h.kind() == Kind.REFERENCE
                && h.unwrapKey().map(key::equals).orElse(false);
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
