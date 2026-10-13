package rearth.oritech.port.neoforge.common.crafting;

import java.util.Objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/** An ingredient with a count. */
public final class SizedIngredient {
    public static final Codec<SizedIngredient> NESTED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(SizedIngredient::ingredient),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(SizedIngredient::count))
            .apply(instance, SizedIngredient::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SizedIngredient> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, SizedIngredient::ingredient,
            ByteBufCodecs.VAR_INT, SizedIngredient::count,
            SizedIngredient::new);

    private final Ingredient ingredient;
    private final int count;

    public SizedIngredient(Ingredient ingredient, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be positive");
        }
        this.ingredient = ingredient;
        this.count = count;
    }

    public static SizedIngredient of(ItemLike item, int count) {
        return new SizedIngredient(Ingredient.of(item), count);
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    public int count() {
        return count;
    }

    public boolean test(ItemStack stack) {
        return ingredient.test(stack) && stack.getCount() >= count;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SizedIngredient other && count == other.count && ingredient.equals(other.ingredient);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ingredient, count);
    }

    @Override
    public String toString() {
        return count + "x " + ingredient;
    }
}
