package appeng.client.render.quad;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadAtlas;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;

import com.mojang.blaze3d.platform.Transparency;

/**
 * A mutable quad used by AE2 to build and transform model geometry. This replaces NeoForge's class of the same name.
 * <p>
 * Vanilla's {@link BakedQuad} does not store vertex colors or normals (NeoForge patches them in). This class keeps
 * them, and they are preserved when the quad is {@linkplain #emit(QuadEmitter) emitted} through Fabric's renderer
 * API. {@link #toBakedQuad()} drops them.
 */
public class MutableQuad {
    private static final int WHITE = 0xFFFFFFFF;

    private final Vector3f[] positions = { new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f() };
    private final long[] uvs = new long[4];
    private final int[] colors = new int[4];
    private final Vector3f[] normals = { new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f() };
    private boolean hasNormals;

    private Direction direction = Direction.DOWN;
    @Nullable
    private TextureAtlasSprite sprite;
    @Nullable
    private ChunkSectionLayer chunkLayer;
    @Nullable
    private RenderType itemRenderType;
    @Nullable
    private RenderType itemGlintRenderType;
    @Nullable
    private RenderType itemGlintSpecialRenderType;
    private int tintIndex = -1;
    @Nullable
    private Direction shadeOverride;
    private int lightEmission;
    private boolean ambientOcclusion = true;

    public MutableQuad() {
        reset();
    }

    public MutableQuad reset() {
        for (int i = 0; i < 4; i++) {
            positions[i].set(0, 0, 0);
            uvs[i] = 0;
            colors[i] = WHITE;
            normals[i].set(0, 0, 0);
        }
        hasNormals = false;
        direction = Direction.DOWN;
        sprite = null;
        chunkLayer = null;
        itemRenderType = null;
        itemGlintRenderType = null;
        itemGlintSpecialRenderType = null;
        tintIndex = -1;
        shadeOverride = null;
        lightEmission = 0;
        ambientOcclusion = true;
        return this;
    }

    // Positions

    public float x(int vertexIndex) {
        return positions[vertexIndex].x;
    }

    public float y(int vertexIndex) {
        return positions[vertexIndex].y;
    }

    public float z(int vertexIndex) {
        return positions[vertexIndex].z;
    }

    public float positionComponent(int vertexIndex, int componentIndex) {
        return positions[vertexIndex].get(componentIndex);
    }

    public Vector3f copyPosition(int vertexIndex) {
        return new Vector3f(positions[vertexIndex]);
    }

    public Vector3f copyPosition(int vertexIndex, Vector3f dest) {
        return dest.set(positions[vertexIndex]);
    }

    public MutableQuad setX(int vertexIndex, float x) {
        positions[vertexIndex].x = x;
        return this;
    }

    public MutableQuad setY(int vertexIndex, float y) {
        positions[vertexIndex].y = y;
        return this;
    }

    public MutableQuad setZ(int vertexIndex, float z) {
        positions[vertexIndex].z = z;
        return this;
    }

    public MutableQuad setPositionComponent(int vertexIndex, int componentIndex, float value) {
        positions[vertexIndex].setComponent(componentIndex, value);
        return this;
    }

    public MutableQuad setPosition(int vertexIndex, float x, float y, float z) {
        positions[vertexIndex].set(x, y, z);
        return this;
    }

    public MutableQuad setPosition(int vertexIndex, Vector3fc position) {
        positions[vertexIndex].set(position);
        return this;
    }

    /**
     * Sets the positions to a face of the unit cube, given in the coordinate space of a sprite applied to that face
     * (i.e. left/bottom/right/top as the texture would appear in a vanilla block model). {@code depth} moves the face
     * towards the center of the cube. All values are in the [0,1] range.
     */
    public MutableQuad setCubeFaceFromSpriteCoords(Direction side, float left, float bottom, float right, float top,
            float depth) {
        this.direction = side;
        // Vertex order follows vanilla: top-left, bottom-left, bottom-right, top-right (as seen from outside)
        float[][] corners = { { left, top }, { left, bottom }, { right, bottom }, { right, top } };
        for (int i = 0; i < 4; i++) {
            float u = corners[i][0];
            float v = corners[i][1];
            switch (side) {
                case NORTH -> positions[i].set(1 - u, v, depth);
                case SOUTH -> positions[i].set(u, v, 1 - depth);
                case EAST -> positions[i].set(1 - depth, v, 1 - u);
                case WEST -> positions[i].set(depth, v, u);
                case UP -> positions[i].set(u, 1 - depth, 1 - v);
                case DOWN -> positions[i].set(u, depth, v);
            }
        }
        return this;
    }

    // Texture coordinates (in atlas space)

    public float u(int vertexIndex) {
        return UVPair.unpackU(uvs[vertexIndex]);
    }

    public float v(int vertexIndex) {
        return UVPair.unpackV(uvs[vertexIndex]);
    }

    public long packedUv(int vertexIndex) {
        return uvs[vertexIndex];
    }

    public MutableQuad setUv(int vertexIndex, float u, float v) {
        uvs[vertexIndex] = UVPair.pack(u, v);
        return this;
    }

    public MutableQuad setPackedUv(int vertexIndex, long packedUv) {
        uvs[vertexIndex] = packedUv;
        return this;
    }

    /**
     * Sets texture coordinates relative to the current sprite (in the [0,1] range).
     */
    public MutableQuad setUvFromSprite(int vertexIndex, float u, float v) {
        var sprite = requiredSprite();
        return setUv(vertexIndex, sprite.getU(u), sprite.getV(v));
    }

    /**
     * Computes texture coordinates from the vertex positions, like vanilla does for block model elements without
     * explicit UVs, and maps them into the current sprite.
     */
    public MutableQuad bakeUvsFromPosition() {
        return bakeUvsFromPosition(UVTransform.IDENTITY);
    }

    public MutableQuad bakeUvsFromPosition(UVTransform transform) {
        for (int i = 0; i < 4; i++) {
            var p = positions[i];
            long uv = switch (direction) {
                case DOWN -> UVPair.pack(p.x, 1 - p.z);
                case UP -> UVPair.pack(p.x, p.z);
                case NORTH -> UVPair.pack(1 - p.x, 1 - p.y);
                case SOUTH -> UVPair.pack(p.x, 1 - p.y);
                case WEST -> UVPair.pack(p.z, 1 - p.y);
                case EAST -> UVPair.pack(1 - p.z, 1 - p.y);
            };
            if (!transform.isIdentity()) {
                uv = transform.transformPacked(uv);
            }
            uvs[i] = uv;
        }

        var sprite = requiredSprite();
        for (int i = 0; i < 4; i++) {
            uvs[i] = UVPair.pack(sprite.getU(UVPair.unpackU(uvs[i])), sprite.getV(UVPair.unpackV(uvs[i])));
        }
        return this;
    }

    // Colors (ARGB)

    public int color(int vertexIndex) {
        return colors[vertexIndex];
    }

    public MutableQuad setColor(int packedColor) {
        for (int i = 0; i < 4; i++) {
            colors[i] = packedColor;
        }
        return this;
    }

    public MutableQuad setColor(int vertexIndex, int packedColor) {
        colors[vertexIndex] = packedColor;
        return this;
    }

    public MutableQuad setColor(int vertexIndex, int r, int g, int b, int a) {
        colors[vertexIndex] = (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
        return this;
    }

    // Normals

    public float normalX(int vertexIndex) {
        return normals[vertexIndex].x;
    }

    public float normalY(int vertexIndex) {
        return normals[vertexIndex].y;
    }

    public float normalZ(int vertexIndex) {
        return normals[vertexIndex].z;
    }

    public MutableQuad setNormal(int vertexIndex, float x, float y, float z) {
        normals[vertexIndex].set(x, y, z);
        hasNormals = true;
        return this;
    }

    public MutableQuad setNormal(int vertexIndex, Vector3fc normal) {
        return setNormal(vertexIndex, normal.x(), normal.y(), normal.z());
    }

    // Material

    public int tintIndex() {
        return tintIndex;
    }

    public MutableQuad setTintIndex(int tintIndex) {
        this.tintIndex = tintIndex;
        return this;
    }

    public Direction direction() {
        return direction;
    }

    public MutableQuad setDirection(Direction direction) {
        this.direction = direction;
        return this;
    }

    @Nullable
    public TextureAtlasSprite sprite() {
        return sprite;
    }

    public TextureAtlasSprite requiredSprite() {
        if (sprite == null) {
            throw new IllegalStateException("No sprite has been set on this quad");
        }
        return sprite;
    }

    @Nullable
    public ChunkSectionLayer chunkLayer() {
        return chunkLayer;
    }

    public MutableQuad setChunkLayer(ChunkSectionLayer chunkLayer) {
        this.chunkLayer = chunkLayer;
        return this;
    }

    public MutableQuad setSprite(Material.Baked material) {
        var transparency = material.forceTranslucent() ? Transparency.TRANSLUCENT : material.sprite().transparency();
        return setSprite(material, transparency);
    }

    public MutableQuad setSprite(Material.Baked material, Transparency transparency) {
        boolean translucent = transparency.hasTranslucent();
        if (material.sprite().atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)) {
            itemRenderType = translucent ? Sheets.translucentBlockItemSheet() : Sheets.cutoutBlockItemSheet();
            itemGlintRenderType = translucent ? Sheets.translucentBlockItemGlintSheet()
                    : Sheets.cutoutBlockItemGlintSheet();
            itemGlintSpecialRenderType = translucent ? Sheets.translucentBlockItemGlintSpecialSheet()
                    : Sheets.cutoutBlockItemGlintSpecialSheet();
        } else {
            itemRenderType = translucent ? Sheets.translucentItemSheet() : Sheets.cutoutItemSheet();
            itemGlintRenderType = translucent ? Sheets.translucentItemGlintSheet() : Sheets.cutoutItemGlintSheet();
            itemGlintSpecialRenderType = translucent ? Sheets.translucentItemGlintSpecialSheet()
                    : Sheets.cutoutItemGlintSpecialSheet();
        }
        this.sprite = material.sprite();
        this.chunkLayer = ChunkSectionLayer.byTransparency(transparency);
        return this;
    }

    @Nullable
    public Direction shadeOverride() {
        return shadeOverride;
    }

    public MutableQuad setShadeOverride(@Nullable Direction shadeOverride) {
        this.shadeOverride = shadeOverride;
        return this;
    }

    public int lightEmission() {
        return lightEmission;
    }

    public MutableQuad setLightEmission(int lightEmission) {
        this.lightEmission = lightEmission;
        return this;
    }

    public boolean hasAmbientOcclusion() {
        return ambientOcclusion;
    }

    public MutableQuad setAmbientOcclusion(boolean ambientOcclusion) {
        this.ambientOcclusion = ambientOcclusion;
        return this;
    }

    // Conversion

    public MutableQuad setFrom(BakedQuad quad) {
        for (int i = 0; i < 4; i++) {
            positions[i].set(quad.position(i));
            uvs[i] = quad.packedUV(i);
            colors[i] = WHITE;
            normals[i].set(0, 0, 0);
        }
        hasNormals = false;
        direction = quad.direction();
        var info = quad.materialInfo();
        sprite = info.sprite();
        chunkLayer = info.layer();
        itemRenderType = info.itemRenderType();
        itemGlintRenderType = info.itemGlintRenderType();
        itemGlintSpecialRenderType = info.itemGlintSpecialRenderType();
        tintIndex = info.tintIndex();
        shadeOverride = info.shadeDirectionOverride();
        lightEmission = info.lightEmission();
        ambientOcclusion = true;
        return this;
    }

    /**
     * Copies a quad produced by Fabric's renderer API (i.e. emitted by another block model), including its vertex
     * colors and normals.
     */
    public MutableQuad setFrom(QuadView quad) {
        var atlas = quad.atlas();
        var spriteFinder = Minecraft.getInstance().getAtlasManager()
                .getAtlasOrThrow((atlas != null ? atlas : QuadAtlas.BLOCK).getId())
                .spriteFinder();
        hasNormals = true;
        int minLight = 15;
        for (int i = 0; i < 4; i++) {
            positions[i].set(quad.x(i), quad.y(i), quad.z(i));
            uvs[i] = UVPair.pack(quad.u(i), quad.v(i));
            colors[i] = quad.color(i);
            if (quad.hasNormal(i)) {
                normals[i].set(quad.normalX(i), quad.normalY(i), quad.normalZ(i));
            } else {
                hasNormals = false;
                normals[i].set(0, 0, 0);
            }
            var lightmap = quad.lightmap(i);
            minLight = Math.min(minLight,
                    Math.min(LightCoordsUtil.block(lightmap), LightCoordsUtil.sky(lightmap)));
        }
        if (!hasNormals) {
            for (int i = 0; i < 4; i++) {
                normals[i].set(0, 0, 0);
            }
        }
        direction = quad.lightFace();
        sprite = spriteFinder.find(quad);
        chunkLayer = quad.chunkLayer();
        itemRenderType = quad.itemRenderType();
        itemGlintRenderType = quad.itemGlintRenderType();
        itemGlintSpecialRenderType = quad.itemGlintSpecialRenderType();
        tintIndex = quad.tintIndex();
        shadeOverride = quad.shadeDirectionOverride();
        lightEmission = quad.emissive() ? 15 : minLight;
        ambientOcclusion = quad.ambientOcclusion() != TriState.FALSE;
        return this;
    }

    /**
     * Copies all data of another mutable quad into this one.
     */
    public MutableQuad setFrom(MutableQuad quad) {
        for (int i = 0; i < 4; i++) {
            positions[i].set(quad.positions[i]);
            uvs[i] = quad.uvs[i];
            colors[i] = quad.colors[i];
            normals[i].set(quad.normals[i]);
        }
        hasNormals = quad.hasNormals;
        direction = quad.direction;
        sprite = quad.sprite;
        chunkLayer = quad.chunkLayer;
        itemRenderType = quad.itemRenderType;
        itemGlintRenderType = quad.itemGlintRenderType;
        itemGlintSpecialRenderType = quad.itemGlintSpecialRenderType;
        tintIndex = quad.tintIndex;
        shadeOverride = quad.shadeOverride;
        lightEmission = quad.lightEmission;
        ambientOcclusion = quad.ambientOcclusion;
        return this;
    }

    public MutableQuad copy() {
        return new MutableQuad().setFrom(this);
    }

    /**
     * Creates a vanilla baked quad. Vertex colors, normals and the ambient occlusion flag are not supported by vanilla
     * quads and are lost.
     */
    public BakedQuad toBakedQuad() {
        var sprite = requiredSprite();
        if (chunkLayer == null || itemRenderType == null || itemGlintRenderType == null
                || itemGlintSpecialRenderType == null) {
            throw new IllegalStateException("No material has been set on this quad");
        }
        var materialInfo = new BakedQuad.MaterialInfo(sprite, chunkLayer, itemRenderType, itemGlintRenderType,
                itemGlintSpecialRenderType, tintIndex, shadeOverride, lightEmission);
        return new BakedQuad(
                new Vector3f(positions[0]),
                new Vector3f(positions[1]),
                new Vector3f(positions[2]),
                new Vector3f(positions[3]),
                uvs[0], uvs[1], uvs[2], uvs[3],
                direction,
                materialInfo);
    }

    /**
     * Emits this quad, including vertex colors and normals, using Fabric's renderer API.
     */
    public void emit(QuadEmitter emitter) {
        emit(emitter, null);
    }

    /**
     * Emits this quad with the given cull face, including vertex colors and normals.
     */
    public void emit(QuadEmitter emitter, @Nullable Direction cullFace) {
        emitter.fromBakedQuad(toBakedQuad());
        emitter.cullFace(cullFace);
        for (int i = 0; i < 4; i++) {
            emitter.color(i, colors[i]);
            if (hasNormals) {
                emitter.normal(i, normals[i]);
            }
        }
        if (!ambientOcclusion) {
            emitter.ambientOcclusion(TriState.FALSE);
        }
        emitter.emit();
    }

    /**
     * Transforms the positions (and normals) of this quad and updates the direction accordingly.
     */
    public MutableQuad transform(Matrix4fc matrix) {
        for (int i = 0; i < 4; i++) {
            matrix.transformPosition(positions[i]);
            if (hasNormals) {
                matrix.transformDirection(normals[i]).normalize();
            }
        }
        var normal = computeFaceNormal();
        direction = Direction.getApproximateNearest(normal.x, normal.y, normal.z);
        return this;
    }

    private Vector3f computeFaceNormal() {
        var a = new Vector3f(positions[2]).sub(positions[0]);
        var b = new Vector3f(positions[3]).sub(positions[1]);
        return a.cross(b).normalize();
    }
}
