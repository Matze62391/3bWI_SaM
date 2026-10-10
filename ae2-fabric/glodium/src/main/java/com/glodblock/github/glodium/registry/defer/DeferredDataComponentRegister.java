package com.glodblock.github.glodium.registry.defer;

import appeng.core.registries.DeferredHolder;
import appeng.core.registries.DeferredRegister;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.function.Function;
import java.util.function.Supplier;

public class DeferredDataComponentRegister extends DeferredRegister<DataComponentType<?>> {

    public DeferredDataComponentRegister(String namespace) {
        super(Registries.DATA_COMPONENT_TYPE, namespace);
    }

    /**
     * The returned holder is a {@link DeferredDataComponentType}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public <I extends DataComponentType<?>> DeferredHolder<DataComponentType<?>, I> register(String name, Function<Identifier, ? extends I> factory) {
        var key = ResourceKey.create(Registries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(getNamespace(), name));
        return (DeferredHolder) addEntry(key, (Function) factory, new DeferredDataComponentType<>(key));
    }

    @SuppressWarnings("unchecked")
    public <T> DeferredDataComponentType<T> registerComponent(String name, Supplier<DataComponentType<T>> factory) {
        return (DeferredDataComponentType<T>) (Object) this.register(name, factory);
    }

}
