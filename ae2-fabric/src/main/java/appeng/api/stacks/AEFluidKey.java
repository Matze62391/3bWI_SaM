package appeng.api.stacks;

import java.util.List;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;

import appeng.api.storage.AEKeyFilter;
import appeng.core.AELog;

public final class AEFluidKey extends AEKey {
    public static final MapCodec<AEFluidKey> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    BuiltInRegistries.FLUID.holderByNameCodec().validate(
                            holder -> holder.is(Fluids.EMPTY.builtInRegistryHolder())
                                    ? DataResult.error(() -> "Fluid must not be minecraft:empty")
                                    : DataResult.success(holder))
                            .fieldOf("id").forGetter(key -> key.variant.typeHolder()),
                    DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                            .forGetter(key -> key.variant.getComponentsPatch()))
                    .apply(instance, (fluidHolder,
                            dataComponentPatch) -> new AEFluidKey(
                                    FluidVariant.of(fluidHolder.value(), dataComponentPatch))));
    public static final Codec<AEFluidKey> CODEC = MAP_CODEC.codec();

    /**
     * Fluid amounts use Fabric's units (droplets), i.e. 81000 per bucket.
     */
    public static final int AMOUNT_BUCKET = (int) FluidConstants.BUCKET;
    public static final int AMOUNT_BLOCK = (int) FluidConstants.BLOCK;

    private final FluidVariant variant;
    private final int hashCode;

    private AEFluidKey(FluidVariant variant) {
        Preconditions.checkArgument(!variant.isBlank(), "variant was blank");
        this.variant = variant;
        this.hashCode = variant.hashCode();
    }

    public static AEFluidKey of(Fluid fluid) {
        return of(FluidVariant.of(fluid));
    }

    public static AEFluidKey of(Fluid fluid, DataComponentPatch components) {
        return of(FluidVariant.of(fluid, components));
    }

    @Nullable
    public static AEFluidKey of(FluidVariant variant) {
        if (variant.isBlank()) {
            return null;
        }
        return new AEFluidKey(variant);
    }

    public static boolean matches(AEKey what, FluidVariant fluid) {
        return what instanceof AEFluidKey fluidKey && fluidKey.matches(fluid);
    }

    public static boolean is(AEKey what) {
        return what instanceof AEFluidKey;
    }

    public static AEKeyFilter filter() {
        return AEFluidKey::is;
    }

    public boolean matches(FluidVariant variant) {
        return this.variant.equals(variant);
    }

    @Override
    public AEKeyType getType() {
        return AEKeyType.fluids();
    }

    @Override
    public AEFluidKey dropSecondary() {
        return of(FluidVariant.of(getFluid()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AEFluidKey aeFluidKey = (AEFluidKey) o;
        // The hash code comparison is a fast-fail cheap check
        return hashCode == aeFluidKey.hashCode && variant.equals(aeFluidKey.variant);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    public static AEFluidKey fromTag(ValueInput input) {
        try {
            return input.read(MAP_CODEC).orElseThrow();
        } catch (Exception e) {
            AELog.debug("Tried to load an invalid fluid key from NBT: %s", input, e);
            return null;
        }
    }

    @Override
    public void toTag(ValueOutput output) {
        output.store(MAP_CODEC, this);
    }

    @Override
    public Object getPrimaryKey() {
        return getFluid();
    }

    @Override
    public Identifier getId() {
        return BuiltInRegistries.FLUID.getKey(getFluid());
    }

    @Override
    public void addDrops(long amount, List<ItemStack> drops, Level level, BlockPos pos) {
        // Fluids are voided
    }

    @Override
    protected Component computeDisplayName() {
        return FluidVariantAttributes.getName(variant);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isTagged(TagKey<?> tag) {
        // This will just return false for incorrectly cast tags
        return variant.getFluid().is((TagKey<Fluid>) tag);
    }

    @Override
    public <T> @Nullable T get(DataComponentType<T> type) {
        return variant.getComponents().get(type);
    }

    @Override
    public boolean hasComponents() {
        return variant.hasComponents();
    }

    public FluidVariant toVariant() {
        return variant;
    }

    public Fluid getFluid() {
        return variant.getFluid();
    }

    @Override
    public void writeToPacket(RegistryFriendlyByteBuf data) {
        FluidVariant.PACKET_CODEC.encode(data, variant);
    }

    public static AEFluidKey fromPacket(RegistryFriendlyByteBuf data) {
        var variant = FluidVariant.PACKET_CODEC.decode(data);
        return new AEFluidKey(variant);
    }

    public static boolean is(@Nullable GenericStack stack) {
        return stack != null && stack.what() instanceof AEFluidKey;
    }

    @Override
    public String toString() {
        var id = BuiltInRegistries.FLUID.getKey(getFluid());
        String idString = id != BuiltInRegistries.FLUID.getDefaultKey() ? id.toString()
                : getFluid().getClass().getName() + "(unregistered)";
        return variant.getComponentsPatch().isEmpty() ? idString : idString + " (+components)";
    }
}
