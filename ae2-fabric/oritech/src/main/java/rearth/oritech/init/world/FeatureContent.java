package rearth.oritech.init.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import rearth.oritech.port.neoforge.registries.DeferredRegister;
import rearth.oritech.Oritech;
import rearth.oritech.init.world.features.oil.OilSpringFeature;
import rearth.oritech.init.world.features.resourcenode.ResourceNodeFeature;
import rearth.oritech.init.world.features.uranium.UraniumPatchFeature;

import java.util.function.Supplier;

public class FeatureContent {

    public static final DeferredRegister<MapCodec<? extends Feature>> FEATURES = DeferredRegister.create(Registries.FEATURE_TYPE, Oritech.MOD_ID);

    public static final Supplier<MapCodec<? extends Feature>> OIL_SPRING = FEATURES.register("oil_spring", () -> OilSpringFeature.CODEC);
    public static final Supplier<MapCodec<? extends Feature>> RESOURCE_NODE = FEATURES.register("resource_node", () -> ResourceNodeFeature.CODEC);
    public static final Supplier<MapCodec<? extends Feature>> URANIUM_PATCH = FEATURES.register("uranium_patch", () -> UraniumPatchFeature.CODEC);

}
