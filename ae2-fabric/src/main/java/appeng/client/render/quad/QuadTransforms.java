package appeng.client.render.quad;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

import com.mojang.math.Transformation;

import net.minecraft.client.resources.model.geometry.BakedQuad;

/**
 * Helpers to transform baked quads.
 */
public final class QuadTransforms {
    private QuadTransforms() {
    }

    /**
     * Transforms the vertex positions of a quad. Like the NeoForge helper of the same name, the face direction of the
     * quad is kept as-is.
     */
    public static BakedQuad applyTransformation(BakedQuad quad, Transformation transformation) {
        if (transformation.equals(Transformation.IDENTITY)) {
            return quad;
        }
        var matrix = transformation.getMatrix();
        var temp = new Vector4f();
        return new BakedQuad(
                transformPosition(temp, quad.position0(), matrix),
                transformPosition(temp, quad.position1(), matrix),
                transformPosition(temp, quad.position2(), matrix),
                transformPosition(temp, quad.position3(), matrix),
                quad.packedUV0(),
                quad.packedUV1(),
                quad.packedUV2(),
                quad.packedUV3(),
                quad.direction(),
                quad.materialInfo());
    }

    private static Vector3fc transformPosition(Vector4f temp, Vector3fc pos, org.joml.Matrix4fc matrix) {
        temp.set(pos.x(), pos.y(), pos.z(), 1);
        matrix.transform(temp);
        temp.div(temp.w);
        return new Vector3f(temp.x(), temp.y(), temp.z());
    }
}
