package rearth.oritech.port.neoforge.fluids;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.material.Fluid;

/**
 * An immutable description of a fluid stack (fluid, amount in millibuckets, components), e.g. for recipe results.
 */
public record FluidStackTemplate(Holder<Fluid> fluid, int amount, DataComponentPatch components) {
    public static final Codec<FluidStackTemplate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FluidStack.FLUID_NON_EMPTY_CODEC.fieldOf("id").forGetter(FluidStackTemplate::fluid),
            ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(FluidStackTemplate::amount),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                    .forGetter(FluidStackTemplate::components))
            .apply(instance, FluidStackTemplate::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStackTemplate> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderRegistry(net.minecraft.core.registries.Registries.FLUID), FluidStackTemplate::fluid,
            ByteBufCodecs.VAR_INT, FluidStackTemplate::amount,
            DataComponentPatch.STREAM_CODEC, FluidStackTemplate::components,
            FluidStackTemplate::new);

    public FluidStackTemplate(Holder<Fluid> fluid, int amount) {
        this(fluid, amount, DataComponentPatch.EMPTY);
    }

    public FluidStackTemplate(Fluid fluid, int amount, DataComponentPatch components) {
        this(fluid.builtInRegistryHolder(), amount, components);
    }

    public FluidStackTemplate(Fluid fluid, int amount) {
        this(fluid.builtInRegistryHolder(), amount);
    }

    public static FluidStackTemplate fromNonEmptyStack(FluidStack stack) {
        return new FluidStackTemplate(stack.typeHolder(), stack.getAmount(), stack.getComponentsPatch());
    }

    public FluidStackTemplate withAmount(int amount) {
        return new FluidStackTemplate(fluid, amount, components);
    }

    public FluidStack create() {
        return new FluidStack(fluid, amount, components);
    }

    public FluidStack apply(DataComponentPatch additionalPatch) {
        return apply(amount, additionalPatch);
    }

    public FluidStack apply(int amount, DataComponentPatch additionalPatch) {
        var stack = new FluidStack(fluid, amount, components);
        stack.applyComponents(additionalPatch);
        return stack;
    }

    public Holder<Fluid> typeHolder() {
        return fluid;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T get(DataComponentType<? extends T> type) {
        return (T) net.minecraft.core.component.PatchedDataComponentMap.fromPatch(net.minecraft.core.component.DataComponentMap.EMPTY, components).get(type);
    }

    public static Codec<FluidStackTemplate> fixedAmountCodec(int amount) {
        return FluidStack.fixedAmountCodec(amount).xmap(FluidStackTemplate::fromNonEmptyStack, FluidStackTemplate::create);
    }
}
