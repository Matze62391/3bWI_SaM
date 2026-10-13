package rearth.oritech.port.neoforge.transfer.fluid;

import java.util.function.Predicate;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import rearth.oritech.port.neoforge.fluids.FluidStack;
import rearth.oritech.port.neoforge.fluids.FluidStackTemplate;
import rearth.oritech.port.neoforge.fluids.FluidType;
import rearth.oritech.port.neoforge.transfer.resource.Resource;

/**
 * An immutable fluid with components.
 */
public final class FluidResource implements Resource {
    public static final FluidResource EMPTY = new FluidResource(Fluids.EMPTY, DataComponentPatch.EMPTY);

    public static final Codec<FluidResource> CODEC = FluidStack.fixedAmountCodec(1)
            .xmap(FluidResource::of, resource -> resource.toStack(1));
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidResource> STREAM_CODEC = FluidStack.OPTIONAL_STREAM_CODEC
            .map(FluidResource::of, resource -> resource.toStack(1));

    private final Fluid fluid;
    private final DataComponentPatch patch;
    private final FluidStack stack;

    private FluidResource(Fluid fluid, DataComponentPatch patch) {
        this.fluid = fluid;
        this.patch = patch;
        this.stack = fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, 1, patch);
    }

    public static FluidResource of(FluidStack stack) {
        return stack.isEmpty() ? EMPTY : new FluidResource(stack.getFluid(), stack.getComponentsPatch());
    }

    public static FluidResource of(@Nullable FluidStackTemplate template) {
        return template == null ? EMPTY : new FluidResource(template.fluid().value(), template.components());
    }

    public static FluidResource of(Fluid fluid) {
        return of(fluid, DataComponentPatch.EMPTY);
    }

    public static FluidResource of(Fluid fluid, DataComponentPatch patch) {
        return fluid == Fluids.EMPTY ? EMPTY : new FluidResource(fluid, patch);
    }

    public static FluidResource of(Holder<Fluid> fluid) {
        return of(fluid.value());
    }

    public static FluidResource of(Holder<Fluid> holder, DataComponentPatch patch) {
        return of(holder.value(), patch);
    }

    public static FluidResource of(FluidVariant variant) {
        return variant.isBlank() ? EMPTY : of(variant.getFluid(), variant.getComponentsPatch());
    }

    public FluidVariant toVariant() {
        return isEmpty() ? FluidVariant.blank() : FluidVariant.of(fluid, patch);
    }

    public Fluid value() {
        return fluid;
    }

    public Fluid getFluid() {
        return fluid;
    }

    public Holder<Fluid> typeHolder() {
        return fluid.builtInRegistryHolder();
    }

    public FluidType getFluidType() {
        return FluidType.of(fluid);
    }

    @Override
    public boolean isEmpty() {
        return fluid == Fluids.EMPTY;
    }

    public FluidResource withMergedPatch(DataComponentPatch additional) {
        var copy = stack.copy();
        copy.applyComponents(additional);
        return of(copy);
    }

    public <D> FluidResource with(DataComponentType<D> type, @Nullable D data) {
        var copy = stack.copy();
        copy.set(type, data);
        return of(copy);
    }

    public <D> FluidResource with(Supplier<? extends DataComponentType<D>> type, @Nullable D data) {
        return with(type.get(), data);
    }

    public FluidResource without(DataComponentType<?> type) {
        var copy = stack.copy();
        copy.remove(type);
        return of(copy);
    }

    public FluidResource without(Supplier<? extends DataComponentType<?>> type) {
        return without(type.get());
    }

    public DataComponentMap getComponents() {
        return stack.getComponents();
    }

    public DataComponentPatch getComponentsPatch() {
        return patch;
    }

    public FluidStack toStack(int amount) {
        return isEmpty() ? FluidStack.EMPTY : new FluidStack(fluid, amount, patch);
    }

    public boolean isComponentsPatchEmpty() {
        return patch.isEmpty();
    }

    public boolean is(FluidType fluidType) {
        return getFluidType() == fluidType;
    }

    public boolean is(Fluid other) {
        return fluid.isSame(other);
    }

    public boolean is(net.minecraft.tags.TagKey<Fluid> tag) {
        return typeHolder().is(tag);
    }

    public boolean matches(FluidStack other) {
        return FluidStack.isSameFluidSameComponents(stack, other);
    }

    public boolean matches(@Nullable FluidStackTemplate template) {
        return template != null && FluidStack.isSameFluidSameComponents(stack, template);
    }

    public boolean test(Predicate<FluidStack> predicate) {
        return predicate.test(stack);
    }

    public Component getHoverName() {
        return stack.getHoverName();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FluidResource other && fluid == other.fluid && patch.equals(other.patch);
    }

    @Override
    public int hashCode() {
        return 31 * fluid.hashCode() + patch.hashCode();
    }

    @Override
    public String toString() {
        return "FluidResource[" + stack + "]";
    }
}
