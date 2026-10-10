package com.glodblock.github.glodium.registry.defer;

import appeng.core.registries.DeferredHolder;
import appeng.core.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Function;
import java.util.function.Supplier;

public class DeferredTileTypeRegister extends DeferredRegister<BlockEntityType<?>> {

    public DeferredTileTypeRegister(String namespace) {
        super(Registries.BLOCK_ENTITY_TYPE, namespace);
    }

    /**
     * The returned holder is a {@link DeferredTileEntityType}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public <I extends BlockEntityType<?>> DeferredHolder<BlockEntityType<?>, I> register(String name, Function<Identifier, ? extends I> factory) {
        var key = ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(getNamespace(), name));
        return (DeferredHolder) addEntry(key, (Function) factory, new DeferredTileEntityType<>(key));
    }

    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> DeferredTileEntityType<T> registerTile(String name, Supplier<BlockEntityType<T>> factory) {
        return (DeferredTileEntityType<T>) (Object) this.register(name, factory);
    }

}
