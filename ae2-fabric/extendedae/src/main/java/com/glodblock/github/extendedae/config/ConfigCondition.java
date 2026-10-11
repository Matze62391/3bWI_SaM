package com.glodblock.github.extendedae.config;

import com.glodblock.github.extendedae.ExtendedAE;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.resources.RegistryOps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

/**
 * A recipe condition that checks a config option ({@code fabric:load_conditions} with
 * {@code "condition": "extendedae:config"}).
 */
public record ConfigCondition(String id) implements ResourceCondition {

    private final static Object2ReferenceMap<String, BooleanSupplier> CONFIG_MAP = new Object2ReferenceOpenHashMap<>();
    public final static MapCodec<ConfigCondition> CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder.group(
                    Codec.STRING.fieldOf("config_id").forGetter(ir -> ir.id)
            ).apply(builder, ConfigCondition::new)
    );
    public static final ResourceConditionType<ConfigCondition> TYPE = ResourceConditionType.create(
            ExtendedAE.id("config"), CODEC);

    static {
        CONFIG_MAP.put(IDs.ASSEMBLER_CIRCUIT, () -> EAEConfig.allowAssemblerCircuits);
    }

    public ConfigCondition {
        if (!CONFIG_MAP.containsKey(id)) {
            throw new IllegalArgumentException("Unregistered ID: " + id);
        }
    }

    public static void register() {
        ResourceConditions.register(TYPE);
    }

    @Override
    public ResourceConditionType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean test(RegistryOps.@Nullable RegistryInfoLookup registryInfo) {
        return CONFIG_MAP.get(this.id).getAsBoolean();
    }

    @Override
    public @NotNull String toString() {
        return "extendedae_config(\"" + this.id + "\")";
    }

    public static class IDs {

        public static final String ASSEMBLER_CIRCUIT = "assembler_circuit";

    }

}
