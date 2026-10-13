package rearth.oritech.port.neoforge.transfer.item;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.ItemLike;

import rearth.oritech.port.neoforge.transfer.resource.Resource;

/**
 * An immutable item with components (an item stack without a count).
 */
public final class ItemResource implements Resource {
    public static final ItemResource EMPTY = new ItemResource(ItemStack.EMPTY);

    public static final Codec<ItemResource> CODEC = ItemStack.CODEC.xmap(ItemResource::of, ItemResource::toStack);
    public static final Codec<ItemResource> OPTIONAL_CODEC = ItemStack.OPTIONAL_CODEC
            .xmap(ItemResource::of, ItemResource::toStack);
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemResource> STREAM_CODEC = ItemStack.OPTIONAL_STREAM_CODEC
            .map(ItemResource::of, ItemResource::toStack);

    private final ItemStack stack;

    private ItemResource(ItemStack stack) {
        this.stack = stack;
    }

    public static ItemResource of(ItemStack stack) {
        return stack.isEmpty() ? EMPTY : new ItemResource(stack.copyWithCount(1));
    }

    public static ItemResource of(@Nullable ItemStackTemplate template) {
        return template == null ? EMPTY : of(template.create());
    }

    public static ItemResource of(ItemLike item) {
        return of(new ItemStack(item));
    }

    public static ItemResource of(ItemLike item, DataComponentPatch patch) {
        var stack = new ItemStack(item);
        stack.applyComponents(patch);
        return of(stack);
    }

    public static ItemResource of(Holder<Item> holder) {
        return of(holder.value());
    }

    public static ItemResource of(Holder<Item> holder, DataComponentPatch patch) {
        return of(holder.value(), patch);
    }

    public static ItemResource of(ItemVariant variant) {
        return variant.isBlank() ? EMPTY : of(variant.toStack());
    }

    public ItemVariant toVariant() {
        return isEmpty() ? ItemVariant.blank() : ItemVariant.of(stack);
    }

    public Item value() {
        return stack.getItem();
    }

    public Item getItem() {
        return value();
    }

    public Holder<Item> typeHolder() {
        return stack.typeHolder();
    }

    @Override
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public boolean matches(ItemStack other) {
        return ItemStack.isSameItemSameComponents(stack, other);
    }

    public boolean matches(@Nullable ItemStackTemplate template) {
        return template != null && matches(template.create());
    }

    public boolean is(ItemLike item) {
        return stack.is(item.asItem());
    }

    public boolean is(net.minecraft.tags.TagKey<Item> tag) {
        return stack.is(tag);
    }

    public boolean test(Predicate<ItemStack> predicate) {
        return predicate.test(stack);
    }

    public boolean isComponentsPatchEmpty() {
        return stack.getComponentsPatch().isEmpty();
    }

    public ItemResource withMergedPatch(DataComponentPatch patch) {
        var copy = stack.copy();
        copy.applyComponents(patch);
        return of(copy);
    }

    public <D> ItemResource with(DataComponentType<D> type, @Nullable D data) {
        var copy = stack.copy();
        copy.set(type, data);
        return of(copy);
    }

    public <D> ItemResource with(Supplier<? extends DataComponentType<D>> type, @Nullable D data) {
        return with(type.get(), data);
    }

    public ItemResource without(DataComponentType<?> type) {
        var copy = stack.copy();
        copy.remove(type);
        return of(copy);
    }

    public ItemResource without(Supplier<? extends DataComponentType<?>> type) {
        return without(type.get());
    }

    @Nullable
    public <T> T get(DataComponentType<? extends T> type) {
        return stack.get(type);
    }

    public <T> T getOrDefault(DataComponentType<? extends T> type, T fallback) {
        return stack.getOrDefault(type, fallback);
    }

    public boolean has(DataComponentType<?> type) {
        return stack.has(type);
    }

    public DataComponentMap getComponents() {
        return stack.getComponents();
    }

    public DataComponentPatch getComponentsPatch() {
        return stack.getComponentsPatch();
    }

    public int getMaxStackSize() {
        return stack.getMaxStackSize();
    }

    public ItemStack toStack(int count) {
        return isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(count);
    }

    public ItemStack toStack() {
        return toStack(1);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ItemResource other && ItemStack.isSameItemSameComponents(stack, other.stack);
    }

    @Override
    public int hashCode() {
        return isEmpty() ? 0 : ItemStack.hashItemAndComponents(stack);
    }

    @Override
    public String toString() {
        return "ItemResource[" + stack + "]";
    }
}
