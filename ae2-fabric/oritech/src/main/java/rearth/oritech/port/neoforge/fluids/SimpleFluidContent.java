package rearth.oritech.port.neoforge.fluids;

import java.util.function.Predicate;

import com.mojang.serialization.Codec;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

/**
 * An immutable fluid stack, used as a data component value.
 */
public final class SimpleFluidContent {
    public static final SimpleFluidContent EMPTY = new SimpleFluidContent(FluidStack.EMPTY);
    public static final Codec<SimpleFluidContent> CODEC = FluidStack.OPTIONAL_CODEC
            .xmap(SimpleFluidContent::copyOf, SimpleFluidContent::copy);
    public static final StreamCodec<RegistryFriendlyByteBuf, SimpleFluidContent> STREAM_CODEC = FluidStack.OPTIONAL_STREAM_CODEC
            .map(SimpleFluidContent::copyOf, SimpleFluidContent::copy);

    private final FluidStack fluidStack;

    private SimpleFluidContent(FluidStack fluidStack) {
        this.fluidStack = fluidStack;
    }

    public static SimpleFluidContent copyOf(FluidStack fluidStack) {
        return fluidStack.isEmpty() ? EMPTY : new SimpleFluidContent(fluidStack.copy());
    }

    public FluidStack copy() {
        return fluidStack.copy();
    }

    public boolean isEmpty() {
        return fluidStack.isEmpty();
    }

    public Fluid getFluid() {
        return fluidStack.getFluid();
    }

    public Holder<Fluid> getFluidHolder() {
        return fluidStack.typeHolder();
    }

    public boolean is(TagKey<Fluid> tag) {
        return fluidStack.is(tag);
    }

    public boolean is(Fluid fluid) {
        return fluidStack.is(fluid);
    }

    public boolean is(Predicate<Holder<Fluid>> predicate) {
        return fluidStack.is(predicate);
    }

    public boolean is(Holder<Fluid> holder) {
        return fluidStack.is(holder.value());
    }

    public boolean is(HolderSet<Fluid> holders) {
        return holders.contains(fluidStack.typeHolder());
    }

    public boolean is(FluidType fluidType) {
        return fluidStack.is(fluidType);
    }

    public int getAmount() {
        return fluidStack.getAmount();
    }

    public FluidType getFluidType() {
        return fluidStack.getFluidType();
    }

    public boolean matches(FluidStack other) {
        return FluidStack.matches(fluidStack, other);
    }

    public boolean isSameFluid(FluidStack other) {
        return FluidStack.isSameFluid(fluidStack, other);
    }

    public boolean isSameFluidSameComponents(FluidStack other) {
        return FluidStack.isSameFluidSameComponents(fluidStack, other);
    }

    public boolean isSameFluidSameComponents(SimpleFluidContent content) {
        return isSameFluidSameComponents(content.fluidStack);
    }

    public DataComponentMap getComponents() {
        return fluidStack.getComponents();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SimpleFluidContent other && FluidStack.matches(fluidStack, other.fluidStack);
    }

    @Override
    public int hashCode() {
        return fluidStack.getAmount() * 31 + FluidStack.hashFluidAndComponents(fluidStack);
    }
}
