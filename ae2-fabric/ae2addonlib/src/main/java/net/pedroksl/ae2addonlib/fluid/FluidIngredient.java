package net.pedroksl.ae2addonlib.fluid;

import java.util.List;
import java.util.function.Predicate;

import com.mojang.serialization.Codec;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;

/**
 * A set of fluids, replacing NeoForge's FluidIngredient. In JSON it is a fluid id, a fluid tag ({@code #tag}) or a list
 * of fluid ids, like NeoForge's.
 */
public final class FluidIngredient implements Predicate<FluidStack> {
    public static final Codec<FluidIngredient> CODEC = RegistryCodecs.holderSet(Registries.FLUID)
            .xmap(FluidIngredient::new, FluidIngredient::fluids);

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidIngredient> STREAM_CODEC = ByteBufCodecs
            .holderSet(Registries.FLUID)
            .map(FluidIngredient::new, FluidIngredient::fluids);

    private final HolderSet<Fluid> fluids;

    public FluidIngredient(HolderSet<Fluid> fluids) {
        this.fluids = fluids;
    }

    public static FluidIngredient of(Fluid... fluids) {
        return new FluidIngredient(HolderSet.direct(Fluid::builtInRegistryHolder, fluids));
    }

    public HolderSet<Fluid> fluids() {
        return fluids;
    }

    /**
     * One stack per matching fluid, e.g. for showing the ingredient in recipe viewers.
     */
    public List<FluidStack> getStacks(long amount) {
        return fluids.stream().map(Holder::value).map(f -> new FluidStack(f, amount)).toList();
    }

    @Override
    public boolean test(FluidStack stack) {
        return !stack.isEmpty() && fluids.contains(stack.getFluid().builtInRegistryHolder());
    }

    public boolean isEmpty() {
        return fluids.size() == 0;
    }
}
