package com.glodblock.github.glodium.registry.defer;

import appeng.core.registries.DeferredHolder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class DeferredTileEntityType<T extends BlockEntity> extends DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> {

    protected DeferredTileEntityType(ResourceKey<BlockEntityType<?>> key) {
        super(key);
    }

}
