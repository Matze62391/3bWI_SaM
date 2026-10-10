package com.glodblock.github.glodium.client.render.highlight;

import com.glodblock.github.glodium.Glodium;
import com.glodblock.github.glodium.client.render.ColorData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;

/**
 * Draws the outlines of highlighted blocks, visible through other blocks.
 */
public class HighlightRender {

    public static final HighlightRender INSTANCE = new HighlightRender();

    private static final float LINE_WIDTH = 3;

    private final RenderPipeline BLOCK_HIGHLIGHT_PIPELINE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(Glodium.id(Glodium.MODID, "pipeline/block_highlight"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false)
            .build();

    private final RenderType BLOCK_HIGHLIGHT_LINE = RenderType.create(
            "glodium:block_highlight_line",
            RenderSetup.builder(BLOCK_HIGHLIGHT_PIPELINE).createRenderSetup()
    );

    public static void hook(LevelRenderContext event) {
        HighlightRender.INSTANCE.submit(event);
    }

    public void submit(LevelRenderContext event) {
        var world = Minecraft.getInstance().level;
        if (world == null) {
            return;
        }
        this.invalidate();
        var drawList = HighlightHandler.getBlockData();
        if (drawList.isEmpty()) {
            return;
        }
        var boxes = new ArrayList<HighlightHandler.HighlightData>();
        for (var block : drawList) {
            if (block.checkDim(world.dimension()) && block.allowRender()) {
                boxes.add(block);
            }
        }
        if (boxes.isEmpty()) {
            return;
        }
        Vec3 camera = event.levelState().cameraRenderState.pos;
        PoseStack poseStack = event.poseStack();
        event.submitNodeCollector().submitCustomGeometry(poseStack, BLOCK_HIGHLIGHT_LINE, (pose, buf) -> {
            for (var block : boxes) {
                drawBlockOutline(block.box().move(camera.reverse()), block.color(), pose, buf);
            }
        });
    }

    private void invalidate() {
        while (HighlightHandler.getFirst() != null) {
            var info = HighlightHandler.getFirst();
            if (System.currentTimeMillis() > info.time()) {
                HighlightHandler.expire();
            } else {
                break;
            }
        }
    }

    private void drawBlockOutline(AABB aabb, ColorData color, PoseStack.Pose pose, VertexConsumer buf) {
        var r = color.getRf();
        var g = color.getGf();
        var b = color.getBf();
        var a = color.getAf();
        var topRight = new Vec3(aabb.maxX, aabb.maxY, aabb.maxZ);
        var bottomRight = new Vec3(aabb.maxX, aabb.minY, aabb.maxZ);
        var bottomLeft = new Vec3(aabb.minX, aabb.minY, aabb.maxZ);
        var topLeft = new Vec3(aabb.minX, aabb.maxY, aabb.maxZ);
        var topRight2 = new Vec3(aabb.maxX, aabb.maxY, aabb.minZ);
        var bottomRight2 = new Vec3(aabb.maxX, aabb.minY, aabb.minZ);
        var bottomLeft2 = new Vec3(aabb.minX, aabb.minY, aabb.minZ);
        var topLeft2 = new Vec3(aabb.minX, aabb.maxY, aabb.minZ);
        renderBox(buf, pose, topLeft, bottomLeft, topRight, bottomRight, r, g, b, a);
        renderBox(buf, pose, topLeft2, bottomLeft2, topRight2, bottomRight2, r, g, b, a);
        renderLine(buf, pose, topRight, topRight2, r, g, b, a);
        renderLine(buf, pose, bottomRight, bottomRight2, r, g, b, a);
        renderLine(buf, pose, bottomLeft, bottomLeft2, r, g, b, a);
        renderLine(buf, pose, topLeft, topLeft2, r, g, b, a);
    }

    private void renderBox(VertexConsumer buf, PoseStack.Pose pose, Vec3 topLeft, Vec3 bottomLeft, Vec3 topRight, Vec3 bottomRight, float r, float g, float b, float a) {
        renderLine(buf, pose, topLeft, bottomLeft, r, g, b, a);
        renderLine(buf, pose, topLeft, topRight, r, g, b, a);
        renderLine(buf, pose, bottomRight, bottomLeft, r, g, b, a);
        renderLine(buf, pose, bottomRight, topRight, r, g, b, a);
    }

    private void renderLine(VertexConsumer buf, PoseStack.Pose pose, Vec3 from, Vec3 to, float r, float g, float b, float a) {
        var mat = pose.pose();
        var normal = new Vector3f((float) (to.x - from.x), (float) (to.y - from.y), (float) (to.z - from.z));
        if (normal.lengthSquared() < 1e-12f) {
            return;
        }
        normal.normalize();
        buf.addVertex(mat, (float) from.x, (float) from.y, (float) from.z).setColor(r, g, b, a).setNormal(normal.x, normal.y, normal.z).setLineWidth(LINE_WIDTH);
        buf.addVertex(mat, (float) to.x, (float) to.y, (float) to.z).setColor(r, g, b, a).setNormal(normal.x, normal.y, normal.z).setLineWidth(LINE_WIDTH);
    }

}
