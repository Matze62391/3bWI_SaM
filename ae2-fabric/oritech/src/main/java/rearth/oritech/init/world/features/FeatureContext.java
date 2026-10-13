package rearth.oritech.init.world.features;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;

/** The arguments of a feature placement (vanilla's FeaturePlaceContext was removed in 26.3). */
public record FeatureContext<C>(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin,
                                C config) {
}
