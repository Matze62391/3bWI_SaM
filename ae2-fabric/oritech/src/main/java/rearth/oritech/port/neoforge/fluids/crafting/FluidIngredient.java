package rearth.oritech.port.neoforge.fluids.crafting;

import java.util.List;
import java.util.function.Predicate;

import com.mojang.serialization.Codec;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

import rearth.oritech.port.neoforge.fluids.FluidStack;

/**
 * A set of fluids, written as a fluid id, a {@code #tag} or a list of ids.
 */
public final class FluidIngredient implements Predicate<FluidStack> {
    public static final Codec<FluidIngredient> CODEC = RegistryCodecs.holderSet(Registries.FLUID)
            .xmap(FluidIngredient::new, FluidIngredient::holderSet);
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidIngredient> STREAM_CODEC = ByteBufCodecs
            .holderSet(Registries.FLUID).map(FluidIngredient::new, FluidIngredient::holderSet);

    private final HolderSet<Fluid> fluids;

    private FluidIngredient(HolderSet<Fluid> fluids) {
        this.fluids = fluids;
    }

    public static FluidIngredient of(Fluid... fluids) {
        return new FluidIngredient(HolderSet.direct(BuiltInRegistries.FLUID::wrapAsHolder, fluids));
    }

    public static FluidIngredient tag(TagKey<Fluid> tag) {
        return new FluidIngredient(BuiltInRegistries.FLUID.getOrThrow(tag));
    }

    public HolderSet<Fluid> holderSet() {
        return fluids;
    }

    public List<Holder<Fluid>> fluids() {
        return fluids.stream().toList();
    }

    public boolean hasNoFluids() {
        return fluids.size() == 0;
    }

    @Override
    public boolean test(FluidStack stack) {
        return !stack.isEmpty() && fluids.contains(stack.typeHolder());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FluidIngredient other && fluids.equals(other.fluids);
    }

    @Override
    public int hashCode() {
        return fluids.hashCode();
    }
}
