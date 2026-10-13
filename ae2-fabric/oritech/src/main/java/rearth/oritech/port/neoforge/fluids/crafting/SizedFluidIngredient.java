package rearth.oritech.port.neoforge.fluids.crafting;

import java.util.Objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.material.Fluid;

import rearth.oritech.port.neoforge.fluids.FluidStack;

/** A fluid ingredient with an amount in millibuckets. */
public final class SizedFluidIngredient {
    public static final Codec<SizedFluidIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FluidIngredient.CODEC.fieldOf("ingredient").forGetter(SizedFluidIngredient::ingredient),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("amount", 1000).forGetter(SizedFluidIngredient::amount))
            .apply(instance, SizedFluidIngredient::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SizedFluidIngredient> STREAM_CODEC = StreamCodec
            .composite(
                    FluidIngredient.STREAM_CODEC, SizedFluidIngredient::ingredient,
                    ByteBufCodecs.VAR_INT, SizedFluidIngredient::amount,
                    SizedFluidIngredient::new);

    private final FluidIngredient ingredient;
    private final int amount;

    public SizedFluidIngredient(FluidIngredient ingredient, int amount) {
        this.ingredient = ingredient;
        this.amount = amount;
    }

    public static SizedFluidIngredient of(Fluid fluid, int amount) {
        return new SizedFluidIngredient(FluidIngredient.of(fluid), amount);
    }

    public FluidIngredient ingredient() {
        return ingredient;
    }

    public int amount() {
        return amount;
    }

    public boolean test(FluidStack stack) {
        return ingredient.test(stack) && stack.getAmount() >= amount;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SizedFluidIngredient other && amount == other.amount && ingredient.equals(other.ingredient);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ingredient, amount);
    }
}
