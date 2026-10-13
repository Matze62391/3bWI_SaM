package rearth.oritech.init.world.features.oil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record OilSpringFeatureConfig(int number, Identifier blockId) {

    public static final Codec<OilSpringFeatureConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("number").forGetter(OilSpringFeatureConfig::number),
            Identifier.CODEC.fieldOf("blockId").forGetter(OilSpringFeatureConfig::blockId)
    ).apply(instance, OilSpringFeatureConfig::new));

}
