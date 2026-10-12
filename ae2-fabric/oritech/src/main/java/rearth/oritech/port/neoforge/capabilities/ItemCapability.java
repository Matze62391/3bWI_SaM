package rearth.oritech.port.neoforge.capabilities;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import rearth.oritech.port.neoforge.transfer.access.ItemAccess;

/**
 * A capability of items, backed by a Fabric {@link ItemApiLookup}. Oritech's own providers are also kept here, so
 * Oritech's code gets its handlers back without going through two adapters.
 */
public final class ItemCapability<T, C> {
    private final ItemApiLookup<Object, ContainerItemContext> lookup;
    private final java.util.function.Function<Object, T> fromFabric;
    private final java.util.function.Function<T, Object> toFabric;
    private final Map<Item, BiFunction<ItemStack, C, T>> providers = new IdentityHashMap<>();

    @SuppressWarnings("unchecked")
    <F> ItemCapability(ItemApiLookup<F, ContainerItemContext> lookup, java.util.function.Function<F, T> fromFabric,
            java.util.function.Function<T, F> toFabric) {
        this.lookup = (ItemApiLookup<Object, ContainerItemContext>) (Object) lookup;
        this.fromFabric = (java.util.function.Function<Object, T>) fromFabric;
        this.toFabric = (java.util.function.Function<T, Object>) toFabric;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public T getCapability(C context) {
        var access = (ItemAccess) context;
        var resource = access.getResource();
        if (resource.isEmpty()) {
            return null;
        }
        var stack = resource.toStack(Math.max(1, access.getAmount()));
        var provider = providers.get(resource.getItem());
        if (provider != null) {
            return provider.apply(stack, context);
        }
        var fabric = lookup.find(stack, FabricAdapters.toFabric(access));
        return fabric == null ? null : fromFabric.apply(fabric);
    }

    @SuppressWarnings("unchecked")
    void registerItem(BiFunction<ItemStack, C, T> provider, ItemLike... items) {
        for (var item : items) {
            providers.put(item.asItem(), provider);
        }
        lookup.registerForItems((stack, context) -> {
            var handler = provider.apply(stack, (C) FabricAdapters.fromFabric(context));
            return handler == null ? null : toFabric.apply(handler);
        }, items);
    }
}
