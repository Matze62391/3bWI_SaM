package rearth.oritech.port.neoforge.registries;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Either;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

/**
 * A holder for an entry registered through {@link DeferredRegister}. It resolves to the registry's own holder once
 * the entry is registered; entries of registries that only exist on NeoForge are kept in {@link #direct}.
 */
public class DeferredHolder<R, T extends R> implements Supplier<T> {
    protected final ResourceKey<R> key;
    @Nullable
    private Holder<R> holder;
    @Nullable
    T direct;

    protected DeferredHolder(ResourceKey<R> key) {
        this.key = key;
    }

    public static <R, T extends R> DeferredHolder<R, T> create(ResourceKey<? extends Registry<R>> registryKey,
            Identifier id) {
        return new DeferredHolder<>(ResourceKey.create(registryKey, id));
    }

    public static <R, T extends R> DeferredHolder<R, T> create(ResourceKey<R> key) {
        return new DeferredHolder<>(key);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private Holder<R> delegate() {
        if (holder == null && direct == null) {
            var registry = (Registry<R>) BuiltInRegistries.REGISTRY.getValue(key.registry());
            if (registry != null) {
                holder = registry.get(key).orElse(null);
            }
        }
        return holder;
    }

    private Holder<R> delegateOrThrow() {
        var delegate = delegate();
        if (delegate == null) {
            throw new IllegalStateException("Registry entry not present: " + key);
        }
        return delegate;
    }

    @SuppressWarnings("unchecked")
    public R value() {
        if (direct != null) {
            return direct;
        }
        return delegateOrThrow().value();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get() {
        return (T) value();
    }

    /** The registry's holder of this entry. */
    public Holder<R> holder() {
        if (direct != null) {
            return Holder.direct(direct);
        }
        return delegateOrThrow();
    }

    @Nullable
    public Holder<R> getDelegate() {
        return delegate();
    }

    public Identifier getId() {
        return key.identifier();
    }

    public ResourceKey<R> getKey() {
        return key;
    }

    public boolean isBound() {
        if (direct != null) {
            return true;
        }
        var delegate = delegate();
        return delegate != null && delegate.isBound();
    }

    public boolean areComponentsBound() {
        var delegate = delegate();
        return delegate == null || delegate.areComponentsBound();
    }

    public boolean is(Identifier id) {
        return key.identifier().equals(id);
    }

    public boolean is(ResourceKey<R> resourceKey) {
        return key.equals(resourceKey);
    }

    public boolean is(Predicate<ResourceKey<R>> predicate) {
        return predicate.test(key);
    }

    public boolean is(TagKey<R> tagKey) {
        var delegate = delegate();
        return delegate != null && delegate.is(tagKey);
    }

    public boolean is(Holder<R> other) {
        return other.is(key);
    }

    public Stream<TagKey<R>> tags() {
        var delegate = delegate();
        return delegate != null ? delegate.tags() : Stream.empty();
    }

    public DataComponentMap components() {
        var delegate = delegate();
        return delegate != null ? delegate.components() : DataComponentMap.EMPTY;
    }

    public Either<ResourceKey<R>, R> unwrap() {
        return Either.left(key);
    }

    public Optional<ResourceKey<R>> unwrapKey() {
        return Optional.of(key);
    }


    public boolean canSerializeIn(HolderOwner<R> owner) {
        var delegate = delegate();
        return delegate != null && delegate.canSerializeIn(owner);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DeferredHolder<?, ?> other) {
            return key.equals(other.key);
        }
        return obj instanceof Holder<?> other && other.unwrapKey().map(key::equals).orElse(false);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key);
    }

    @Override
    public String toString() {
        return "DeferredHolder{" + key + "}";
    }
}
