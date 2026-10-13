package com.glodblock.github.glodium.registry.defer;

import appeng.core.registries.DeferredHolder;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

import java.util.Objects;

/**
 * A data component type whose holder is the registered component type itself. NeoForge lets item stacks take a
 * {@code Supplier<DataComponentType>}; on Fabric the holder can be passed to {@code ItemStack.get/set} directly
 * because it is the object that is registered.
 */
public class DeferredDataComponentType<T> extends DeferredHolder<DataComponentType<?>, DataComponentType<T>> implements DataComponentType<T> {

    private DataComponentType<T> delegate;

    protected DeferredDataComponentType(ResourceKey<DataComponentType<?>> key) {
        super(key);
    }

    void setDelegate(DataComponentType<T> delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    private DataComponentType<T> delegate() {
        if (this.delegate == null) {
            throw new IllegalStateException("Data component " + getId() + " is not registered yet");
        }
        return this.delegate;
    }

    @Override
    public Codec<T> codec() {
        return delegate().codec();
    }

    @Override
    public boolean isTransient() {
        return delegate().isTransient();
    }

    @Override
    public boolean ignoreSwapAnimation() {
        return delegate().ignoreSwapAnimation();
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec() {
        return delegate().streamCodec();
    }

    @Override
    public String toString() {
        return String.valueOf(getId());
    }

}
