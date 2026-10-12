package rearth.oritech.init.world.features.resourcenode;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import rearth.oritech.init.world.features.FeatureContext;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import rearth.oritech.Oritech;
import rearth.oritech.config.OritechConfig;

import java.util.List;

public class ResourceNodeFeature implements Feature {
    public static final MapCodec<ResourceNodeFeature> CODEC = MapCodec.assumeMapUnsafe(ResourceNodeFeatureConfig.CODEC).xmap(ResourceNodeFeature::new, feature -> feature.config);

    private final ResourceNodeFeatureConfig config;

    public ResourceNodeFeature(ResourceNodeFeatureConfig config) {
        this.config = config;
    }

    @Override
    public MapCodec<ResourceNodeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        return placeFeature(new FeatureContext<>(level, chunkGenerator, random, origin, config));
    }



        private boolean placeFeature(FeatureContext<ResourceNodeFeatureConfig> context) {

        var level = context.level();
        var origin = context.origin();

        if (level.isClientSide()) return false;

        var solidBlockFound = false;
        var testPos = rearth.oritech.util.BlockPosHelper.of(origin);
        var deepNodePos = testPos;
        var boulderPos = testPos;

        for (int y = origin.getY(); y > level.getMinY(); y--) {
            var downPos = testPos.below();
            var testState = level.getBlockState(downPos);
            if (testState.is(Blocks.BEDROCK)) {
                deepNodePos = testPos;
                break;
            } else if (testState.isRedstoneConductor(level, downPos) && !solidBlockFound) {
                boulderPos = testPos = downPos;
                solidBlockFound = true;
            } else {
                testPos = downPos;
            }
        }

        // edge case: if no solid block was found, or the boulder is too close to the deep node, don't generate
        if (!solidBlockFound || boulderPos.getY() < (deepNodePos.getY() + 10))
            return false;

        if (OritechConfig.easyFindFeatures.get())
            placeSurfaceBoulder(boulderPos, context);
        placeBedrockNode(deepNodePos, context);
        Oritech.LOGGER.debug("placing resource node at " + boulderPos + " with deep " + deepNodePos);
        return true;

    }

    private BlockState getRandomBlockFromList(List<Identifier> list, RandomSource random) {
        return BuiltInRegistries.BLOCK.get(getRandomFromList(list, random)).get().value().defaultBlockState();
    }

    private Identifier getRandomFromList(List<Identifier> list, RandomSource random) {
        return list.get(random.nextInt(list.size()));
    }

    private void placeBedrockNode(BlockPos startPos, FeatureContext<ResourceNodeFeatureConfig> context) {

        var level = context.level();
        var random = context.random();
        var ores = context.config().nodeOres();

        var radius = context.config().nodeSize();
        var overlayBlock = BuiltInRegistries.BLOCK.get(context.config().overlayBlock()).get().value().defaultBlockState();
        var overlayHeight = context.config().overlayHeight();

        var noise = new PerlinNoise(random);

        // the bottom of the "bowl" should start below the top layer of bedrock
        BlockPos centerPos = startPos.above(radius - 2);

        for (BlockPos pos : rearth.oritech.util.BlockPosHelper.withinManhattan(centerPos, radius, radius, radius)) {
            // skip anything outside the radius, or outside the vertical cutoff
            if (Math.sqrt(pos.distSqr(centerPos)) + noise.get(pos.getX(), pos.getY(), pos.getZ()) > radius
                    || pos.getY() >= startPos.getY() + overlayHeight + 3 + noise.get(pos.getX(), pos.getY() + 2, pos.getZ()))
                continue;
            // randomly replace some blocks below bedrock level with resource nodes
            if (pos.getY() <= startPos.getY() + 1 && random.nextDouble() <= context.config().nodeOreChance()) {
                level.setBlock(pos, getRandomBlockFromList(ores, random), 0x10);
                // set blocks between bedrock and bedrock + overlayHeight to overlayBlock
            } else if (pos.getY() > startPos.getY() + 1 && pos.getY() <= startPos.getY() + overlayHeight + 1) {
                level.setBlock(pos, overlayBlock, 0x10);
                // set anything between overlay and vertical cutoff to air
            } else if (pos.getY() > startPos.getY() + 1) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 0x10);
            }
        }
    }

    private void placeSurfaceBoulder(BlockPos startPos, FeatureContext<ResourceNodeFeatureConfig> context) {

        var level = context.level();
        var random = context.random();
        var radius = context.config().boulderRadius();
        var movedCenter = startPos.relative(Axis.getRandom(random), random.nextIntBetweenInclusive(0, radius - 1));
        var ores = context.config().boulderOres();
        var noise = new PerlinNoise(random);

        for (BlockPos pos : rearth.oritech.util.BlockPosHelper.withinManhattan(movedCenter, radius, radius, radius)) {
            if (Math.sqrt(pos.distSqr(movedCenter)) > radius + noise.get(pos.getX(), pos.getY(), pos.getZ()))
                continue;
            level.setBlock(pos, getRandomBlockFromList(ores, random), 0x10);
        }
    }
}
