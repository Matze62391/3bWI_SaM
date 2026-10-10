package net.pedroksl.ae2addonlib.fluid;

import java.util.Objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;

/**
 * A fluid with an amount, replacing NeoForge's FluidStack.
 * <p/>
 * Amounts are in droplets (81000 per bucket), like everywhere else on Fabric and in AE2's {@link AEFluidKey}. The JSON
 * {@link #CODEC} reads and writes millibuckets (1000 per bucket) though, so recipe files stay the same as on NeoForge.
 */
public final class FluidStack {
    /**
     * Droplets per millibucket.
     */
    public static final int DROPLETS_PER_MB = (int) (FluidConstants.BUCKET / 1000);

    public static final FluidStack EMPTY = new FluidStack(FluidVariant.blank(), 0);

    private static final Codec<Fluid> FLUID_CODEC = BuiltInRegistries.FLUID.byNameCodec();

    /**
     * Reads and writes the amount in millibuckets.
     */
    public static final Codec<FluidStack> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                    FLUID_CODEC.fieldOf("id").forGetter(FluidStack::getFluid),
                    Codec.INT.optionalFieldOf("amount", 1000).forGetter(s -> s.getAmount() / DROPLETS_PER_MB),
                    DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                            .forGetter(s -> s.variant.getComponentsPatch()))
            .apply(builder, (Fluid fluid, Integer mb, DataComponentPatch components) -> new FluidStack(
                    FluidVariant.of(fluid, components), (long) mb * DROPLETS_PER_MB)));

    public static final Codec<FluidStack> OPTIONAL_CODEC = ExtraCodecs.optionalEmptyMap(CODEC)
            .xmap(o -> o.orElse(EMPTY), s -> s.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(s));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> OPTIONAL_STREAM_CODEC = StreamCodec.of(
            (buf, stack) -> {
                buf.writeBoolean(!stack.isEmpty());
                if (!stack.isEmpty()) {
                    FluidVariant.PACKET_CODEC.encode(buf, stack.variant);
                    buf.writeVarLong(stack.amount);
                }
            },
            buf -> buf.readBoolean() ? new FluidStack(FluidVariant.PACKET_CODEC.decode(buf), buf.readVarLong())
                    : EMPTY);

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> STREAM_CODEC = OPTIONAL_STREAM_CODEC;

    private final FluidVariant variant;
    private long amount;

    public FluidStack(FluidVariant variant, long amount) {
        this.variant = Objects.requireNonNull(variant);
        this.amount = variant.isBlank() ? 0 : amount;
    }

    public FluidStack(Fluid fluid, long amount) {
        this(fluid == Fluids.EMPTY ? FluidVariant.blank() : FluidVariant.of(fluid), amount);
    }

    public FluidStack(Holder<Fluid> fluid, long amount) {
        this(fluid.value(), amount);
    }

    public static FluidStack of(@Nullable AEFluidKey key, long amount) {
        return key == null ? EMPTY : new FluidStack(key.toVariant(), amount);
    }

    @Nullable
    public static GenericStack toGenericStack(FluidStack stack) {
        return stack.isEmpty() ? null : new GenericStack(AEFluidKey.of(stack.variant), stack.amount);
    }

    @Nullable
    public AEFluidKey toKey() {
        return isEmpty() ? null : AEFluidKey.of(variant);
    }

    public FluidVariant getVariant() {
        return variant;
    }

    public Fluid getFluid() {
        return isEmpty() ? Fluids.EMPTY : variant.getFluid();
    }

    public Holder<Fluid> getFluidHolder() {
        return getFluid().builtInRegistryHolder();
    }

    public boolean is(Fluid fluid) {
        return getFluid() == fluid;
    }

    public int getAmount() {
        return (int) Math.min(Integer.MAX_VALUE, amount);
    }

    public long getAmountLong() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public void grow(long amount) {
        this.amount += amount;
    }

    public void shrink(long amount) {
        this.amount -= amount;
    }

    public boolean isEmpty() {
        return variant.isBlank() || amount <= 0;
    }

    public FluidStack copy() {
        return new FluidStack(variant, amount);
    }

    public FluidStack copyWithAmount(long amount) {
        return new FluidStack(variant, amount);
    }

    public Component getHoverName() {
        return FluidVariantAttributes.getName(variant);
    }

    public static boolean isSameFluid(FluidStack a, FluidStack b) {
        return a.getFluid() == b.getFluid();
    }

    public static boolean isSameFluidSameComponents(FluidStack a, FluidStack b) {
        return a.variant.equals(b.variant);
    }

    public static boolean matches(FluidStack a, FluidStack b) {
        return isSameFluidSameComponents(a, b) && a.amount == b.amount;
    }

    @Override
    public String toString() {
        return amount + " " + variant;
    }
}
