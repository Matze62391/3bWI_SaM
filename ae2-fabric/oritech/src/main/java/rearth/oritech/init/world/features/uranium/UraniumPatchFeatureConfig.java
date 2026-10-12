package rearth.oritech.init.world.features.uranium;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record UraniumPatchFeatureConfig(int number, Identifier blockId,
                                        Identifier crystalId) {

    public static final Codec<UraniumPatchFeatureConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("number").forGetter(UraniumPatchFeatureConfig::number),
            Identifier.CODEC.fieldOf("blockId").forGetter(UraniumPatchFeatureConfig::blockId),
            Identifier.CODEC.fieldOf("crystalId").forGetter(UraniumPatchFeatureConfig::crystalId)
    ).apply(instance, UraniumPatchFeatureConfig::new));

}
