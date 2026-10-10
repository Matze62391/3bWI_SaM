package com.glodblock.github.glodium.registry.defer;

import appeng.core.registries.DeferredHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceKey;

public class DeferredDataComponentType<T> extends DeferredHolder<DataComponentType<?>, DataComponentType<T>> {

    protected DeferredDataComponentType(ResourceKey<DataComponentType<?>> key) {
        super(key);
    }

}
