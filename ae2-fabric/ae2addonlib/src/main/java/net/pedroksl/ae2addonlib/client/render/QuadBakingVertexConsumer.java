package net.pedroksl.ae2addonlib.client.render;

import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;

import appeng.client.render.quad.MutableQuad;

/**
 * Builds a {@link BakedQuad} vertex by vertex (replaces NeoForge's class of the same name, based on AE2's
 * {@link MutableQuad}).
 */
public class QuadBakingVertexConsumer {
    private MutableQuad quad = new MutableQuad();
    private int vertex = -1;
    private boolean shade = true;
    private Direction direction = Direction.DOWN;

    public void setSprite(Material.Baked sprite) {
        quad.setSprite(sprite);
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
        quad.setDirection(direction);
    }

    public void setShade(boolean shade) {
        this.shade = shade;
    }

    public void setLightEmission(int lightEmission) {
        quad.setLightEmission(lightEmission);
    }

    public QuadBakingVertexConsumer addVertex(float x, float y, float z) {
        vertex++;
        quad.setPosition(vertex, x, y, z);
        quad.setColor(vertex, 255, 255, 255, 255);
        return this;
    }

    public QuadBakingVertexConsumer setColor(float r, float g, float b, float a) {
        quad.setColor(vertex, (int) (r * 255), (int) (g * 255), (int) (b * 255), (int) (a * 255));
        return this;
    }

    public QuadBakingVertexConsumer setNormal(float x, float y, float z) {
        quad.setNormal(vertex, x, y, z);
        return this;
    }

    public QuadBakingVertexConsumer setUv(float u, float v) {
        quad.setUv(vertex, u, v);
        return this;
    }

    /**
     * Bakes the quad and starts a new one.
     */
    public BakedQuad bakeQuad() {
        if (!shade) {
            quad.setShadeOverride(null);
        }
        var result = quad.toBakedQuad();
        quad = new MutableQuad();
        vertex = -1;
        shade = true;
        quad.setDirection(direction);
        return result;
    }
}
