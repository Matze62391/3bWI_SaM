package rearth.oritech.port.neoforge.capabilities;

import java.util.function.BiFunction;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Registers Oritech's block and item capabilities into Fabric's API lookups.
 */
public final class RegisterCapabilitiesEvent {
    public <T, C, B extends BlockEntity> void registerBlockEntity(BlockCapability<T, C> capability,
            BlockEntityType<B> type, BiFunction<? super B, C, T> provider) {
        capability.registerBlockEntity(type, provider);
    }

    public <T, C> void registerItem(ItemCapability<T, C> capability, BiFunction<ItemStack, C, T> provider,
            ItemLike... items) {
        capability.registerItem(provider, items);
    }
}
