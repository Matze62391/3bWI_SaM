package appeng.client.model;

import org.joml.Matrix4fc;

import com.mojang.math.Transformation;

import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.core.Direction;

/**
 * A model state that applies an additional transformation after the transformation of a base model state.
 */
public final class ComposedModelState implements ModelState {
    private final ModelState base;
    private final Transformation transformation;

    public ComposedModelState(ModelState base, Transformation transformation) {
        this.base = base;
        this.transformation = base.transformation().compose(transformation);
    }

    @Override
    public Transformation transformation() {
        return transformation;
    }

    @Override
    public Matrix4fc faceTransformation(Direction face) {
        return base.faceTransformation(face);
    }

    @Override
    public Matrix4fc inverseFaceTransformation(Direction face) {
        return base.inverseFaceTransformation(face);
    }
}
