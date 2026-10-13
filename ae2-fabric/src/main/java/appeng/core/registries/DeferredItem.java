package appeng.core.registries;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class DeferredItem<T extends Item> extends DeferredHolder<Item, T> implements ItemLike {
    DeferredItem(ResourceKey<Item> key) {
        super(key);
    }

    public ItemStack toStack() {
        return toStack(1);
    }

    public ItemStack toStack(int count) {
        return new ItemStack(get(), count);
    }

    @Override
    public Item asItem() {
        return get();
    }
}
