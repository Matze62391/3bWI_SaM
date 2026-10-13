package appeng.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block state model whose parts depend on the level (usually on the render data of a block entity). Replaces
 * NeoForge's interface of the same name and bridges it to Fabric's renderer API, which calls
 * {@link #emitQuads(QuadEmitter, BlockAndTintGetter, BlockPos, BlockState, RandomSource, Predicate)} with the level
 * context.
 */
public interface DynamicBlockStateModel extends BlockStateModel {
    /**
     * Collects the parts to render for the block at the given position.
     */
    void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
            List<BlockStateModelPart> parts);

    @Override
    default void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
        collectParts(BlockAndTintGetter.EMPTY, BlockPos.ZERO, Blocks.AIR.defaultBlockState(), random, parts);
    }

    /**
     * @return The material flags of this model at the given position.
     */
    default @BakedQuad.MaterialFlags int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return materialFlags();
    }

    @Override
    default @BakedQuad.MaterialFlags int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state,
            RandomSource random) {
        return materialFlags(level, pos, state);
    }

    @Override
    default Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return particleMaterial();
    }

    @Override
    default void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state,
            RandomSource random, Predicate<@Nullable Direction> cullTest) {
        var parts = new ArrayList<BlockStateModelPart>();
        collectParts(level, pos, state, random, parts);
        for (var part : parts) {
            part.emitQuads(emitter, cullTest);
        }
    }

    /**
     * Collects the parts of any block state model, passing the level context to models that support it.
     */
    static void collectParts(BlockStateModel model, BlockAndTintGetter level, BlockPos pos, BlockState state,
            RandomSource random, List<BlockStateModelPart> parts) {
        if (model instanceof DynamicBlockStateModel dynamicModel) {
            dynamicModel.collectParts(level, pos, state, random, parts);
        } else {
            model.collectParts(random, parts);
        }
    }
}
