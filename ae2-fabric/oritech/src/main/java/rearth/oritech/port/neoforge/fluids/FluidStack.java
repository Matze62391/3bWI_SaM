package rearth.oritech.port.neoforge.fluids;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * A mutable amount (in millibuckets) of a fluid with data components, like an {@code ItemStack} for fluids.
 */
public final class FluidStack {
    public static final FluidStack EMPTY = new FluidStack(Fluids.EMPTY, 0);

    public static final Codec<Holder<Fluid>> FLUID_NON_EMPTY_CODEC = BuiltInRegistries.FLUID.holderByNameCodec()
            .validate(fluid -> fluid.value() == Fluids.EMPTY
                    ? com.mojang.serialization.DataResult.error(() -> "Fluid must not be minecraft:empty")
                    : com.mojang.serialization.DataResult.success(fluid));

    public static final MapCodec<FluidStack> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            FLUID_NON_EMPTY_CODEC.fieldOf("id").forGetter(FluidStack::typeHolder),
            ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(FluidStack::getAmount),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                    .forGetter(FluidStack::getComponentsPatch))
            .apply(instance, FluidStack::new));

    public static final Codec<FluidStack> CODEC = MAP_CODEC.codec();

    public static final Codec<FluidStack> OPTIONAL_CODEC = ExtraCodecs.optionalEmptyMap(CODEC)
            .xmap(optional -> optional.orElse(EMPTY), stack -> stack.isEmpty() ? Optional.empty() : Optional.of(stack));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> OPTIONAL_STREAM_CODEC = new StreamCodec<>() {
        @Override
        public FluidStack decode(RegistryFriendlyByteBuf buf) {
            var amount = buf.readVarInt();
            if (amount <= 0) {
                return EMPTY;
            }
            var fluid = ByteBufCodecs.holderRegistry(net.minecraft.core.registries.Registries.FLUID).decode(buf);
            var patch = DataComponentPatch.STREAM_CODEC.decode(buf);
            return new FluidStack(fluid, amount, patch);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, FluidStack stack) {
            if (stack.isEmpty()) {
                buf.writeVarInt(0);
                return;
            }
            buf.writeVarInt(stack.getAmount());
            ByteBufCodecs.holderRegistry(net.minecraft.core.registries.Registries.FLUID).encode(buf, stack.typeHolder());
            DataComponentPatch.STREAM_CODEC.encode(buf, stack.getComponentsPatch());
        }
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> STREAM_CODEC = OPTIONAL_STREAM_CODEC;

    private final Fluid fluid;
    private int amount;
    private final PatchedDataComponentMap components;

    public FluidStack(Fluid fluid, int amount, DataComponentPatch patch) {
        this.fluid = fluid;
        this.amount = amount;
        this.components = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, patch);
    }

    public FluidStack(Fluid fluid, int amount) {
        this(fluid, amount, DataComponentPatch.EMPTY);
    }

    public FluidStack(Holder<Fluid> fluid, int amount) {
        this(fluid.value(), amount);
    }

    public FluidStack(Holder<Fluid> fluid, int amount, DataComponentPatch patch) {
        this(fluid.value(), amount, patch);
    }

    public static Codec<FluidStack> fixedAmountCodec(int amount) {
        return RecordCodecBuilder.create(instance -> instance.group(
                FLUID_NON_EMPTY_CODEC.fieldOf("id").forGetter(FluidStack::typeHolder),
                DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                        .forGetter(FluidStack::getComponentsPatch))
                .apply(instance, (fluid, patch) -> new FluidStack(fluid, amount, patch)));
    }

    public static MapCodec<FluidStack> lenientOptionalFieldOf(String fieldName) {
        return OPTIONAL_CODEC.lenientOptionalFieldOf(fieldName, EMPTY);
    }

    public boolean isEmpty() {
        return this == EMPTY || fluid == Fluids.EMPTY || amount <= 0;
    }

    public Fluid getFluid() {
        return isEmpty() ? Fluids.EMPTY : fluid;
    }

    public Holder<Fluid> typeHolder() {
        return getFluid().builtInRegistryHolder();
    }

    public boolean is(Fluid other) {
        return getFluid().isSame(other);
    }

    public boolean is(net.minecraft.tags.TagKey<Fluid> tag) {
        return typeHolder().is(tag);
    }

    public boolean is(Predicate<Holder<Fluid>> predicate) {
        return predicate.test(typeHolder());
    }

    public boolean is(FluidType type) {
        return getFluidType() == type;
    }

    public FluidType getFluidType() {
        return FluidType.of(getFluid());
    }

    public int amount() {
        return getAmount();
    }

    public int getAmount() {
        return isEmpty() ? 0 : amount;
    }

    public void setAmount(int amount) {
        if (this == EMPTY) {
            throw new IllegalStateException("Can't modify the empty stack");
        }
        this.amount = amount;
    }

    public void limitSize(int amount) {
        if (!isEmpty() && getAmount() > amount) {
            setAmount(amount);
        }
    }

    public void grow(int addedAmount) {
        setAmount(this.amount + addedAmount);
    }

    public void shrink(int removedAmount) {
        grow(-removedAmount);
    }

    public FluidStack split(int amount) {
        var taken = Math.min(amount, getAmount());
        var result = copyWithAmount(taken);
        if (this != EMPTY) {
            shrink(taken);
        }
        return result;
    }

    public FluidStack copyAndClear() {
        if (isEmpty()) {
            return EMPTY;
        }
        var copy = copy();
        setAmount(0);
        return copy;
    }

    public FluidStack copy() {
        if (isEmpty()) {
            return EMPTY;
        }
        return new FluidStack(fluid, amount, components.asPatch());
    }

    public FluidStack copyWithAmount(int amount) {
        if (isEmpty() || amount <= 0) {
            return EMPTY;
        }
        return new FluidStack(fluid, amount, components.asPatch());
    }

    public FluidStack transmuteCopy(Fluid newFluid) {
        return transmuteCopy(newFluid, getAmount());
    }

    public FluidStack transmuteCopy(Fluid newFluid, int newAmount) {
        return isEmpty() ? EMPTY : new FluidStack(newFluid, newAmount, components.asPatch());
    }

    public DataComponentMap getComponents() {
        return components;
    }

    public DataComponentPatch getComponentsPatch() {
        return isEmpty() ? DataComponentPatch.EMPTY : components.asPatch();
    }

    public DataComponentMap immutableComponents() {
        return components.toImmutableMap();
    }

    public boolean isComponentsPatchEmpty() {
        return getComponentsPatch().isEmpty();
    }

    public boolean hasNonDefault(DataComponentType<?> type) {
        return components.has(type);
    }

    @Nullable
    public <T> T get(DataComponentType<? extends T> type) {
        return components.get(type);
    }

    public <T> T getOrDefault(DataComponentType<? extends T> type, T fallback) {
        return components.getOrDefault(type, fallback);
    }

    public boolean has(DataComponentType<?> type) {
        return components.has(type);
    }

    @Nullable
    public <T> T set(DataComponentType<T> type, @Nullable T value) {
        return components.set(type, value);
    }

    @Nullable
    public <T> T set(TypedDataComponent<T> value) {
        return components.set(value.type(), value.value());
    }

    @Nullable
    public <T> T remove(DataComponentType<? extends T> type) {
        return components.remove(type);
    }

    public void applyComponents(DataComponentPatch patch) {
        components.applyPatch(patch);
    }

    public void applyComponents(DataComponentMap map) {
        components.setAll(map);
    }

    public FluidVariant toVariant() {
        return isEmpty() ? FluidVariant.blank() : FluidVariant.of(fluid, getComponentsPatch());
    }

    public static FluidStack fromVariant(FluidVariant variant, int amount) {
        return variant.isBlank() || amount <= 0 ? EMPTY : new FluidStack(variant.getFluid(), amount, variant.getComponentsPatch());
    }

    public Component getHoverName() {
        return isEmpty() ? Component.empty() : FluidVariantAttributes.getName(toVariant());
    }

    public String getDescriptionId() {
        return getFluidType().getDescriptionId();
    }

    public List<Component> getTooltipLines(Item.TooltipContext context, @Nullable Player player, TooltipFlag flag) {
        var lines = new ArrayList<Component>();
        lines.add(getHoverName());
        return lines;
    }

    public static boolean matches(FluidStack first, FluidStack second) {
        return first == second || first.getAmount() == second.getAmount() && isSameFluidSameComponents(first, second);
    }

    public static boolean matches(FluidStack a, @Nullable FluidStackTemplate b) {
        return b != null && a.getAmount() == b.amount() && isSameFluidSameComponents(a, b);
    }

    public static boolean isSameFluid(FluidStack first, FluidStack second) {
        return first.getFluid() == second.getFluid();
    }

    public static boolean isSameFluidSameComponents(FluidStack first, FluidStack second) {
        if (!isSameFluid(first, second)) {
            return false;
        }
        return first.isEmpty() && second.isEmpty() || Objects.equals(first.getComponentsPatch(), second.getComponentsPatch());
    }

    public static boolean isSameFluid(FluidStack a, @Nullable FluidStackTemplate b) {
        return b != null && a.getFluid() == b.fluid().value();
    }

    public static boolean isSameFluidSameComponents(FluidStack a, @Nullable FluidStackTemplate b) {
        return isSameFluid(a, b) && Objects.equals(a.getComponentsPatch(), b.components());
    }

    public static int hashFluidAndComponents(@Nullable FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        return 31 * stack.getFluid().hashCode() + stack.getComponentsPatch().hashCode();
    }

    @Override
    public String toString() {
        return getAmount() + " " + BuiltInRegistries.FLUID.getKey(getFluid());
    }
}
