package appeng.client.render.quad;

import com.mojang.math.Quadrant;

import net.minecraft.client.model.geom.builders.UVPair;

/**
 * A rotation and optional mirroring of normalized texture coordinates, applied around the center of the texture.
 */
public record UVTransform(Quadrant rotation, boolean flipU, boolean flipV) {
    public static final UVTransform IDENTITY = new UVTransform(Quadrant.R0, false, false);

    public static UVTransform of(Quadrant rotation, boolean flipU, boolean flipV) {
        if (rotation == Quadrant.R0 && !flipU && !flipV) {
            return IDENTITY;
        }
        return new UVTransform(rotation, flipU, flipV);
    }

    public boolean isIdentity() {
        return rotation == Quadrant.R0 && !flipU && !flipV;
    }

    /**
     * Transforms texture coordinates in the [0,1] range.
     */
    public long transformPacked(long packedUv) {
        float u = UVPair.unpackU(packedUv);
        float v = UVPair.unpackV(packedUv);

        // Rotate clockwise in 90° steps
        for (int i = 0; i < rotation.shift; i++) {
            float previousU = u;
            u = v;
            v = 1 - previousU;
        }

        if (flipU) {
            u = 1 - u;
        }
        if (flipV) {
            v = 1 - v;
        }
        return UVPair.pack(u, v);
    }
}
