package appeng.client.hooks;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import appeng.api.implementations.items.IFacadeItem;
import appeng.api.parts.IFacadePart;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.client.render.AERenderTypes;
import appeng.core.AEConfig;
import appeng.core.definitions.AEParts;
import appeng.items.parts.FacadeItem;
import appeng.parts.BusCollisionHelper;
import appeng.parts.PartPlacement;

public class RenderBlockOutlineHook {
    private static final float PREVIEW_LINE_WIDTH = 7F;
    private static final float HIGH_CONTRAST_SECONDARY_LINE_WIDTH = 7F;

    private RenderBlockOutlineHook() {
    }

    /**
     * The custom outline renderers that were determined during extraction of the current frame.
     */
    private static final List<CustomBlockOutlineRenderer> customRenderers = new ArrayList<>();

    public static void install() {
        LevelExtractionEvents.AFTER_BLOCK_OUTLINE_EXTRACTION.register(RenderBlockOutlineHook::handleEvent);
        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, outlineRenderState) -> {
            boolean renderVanillaOutline = true;
            for (var renderer : customRenderers) {
                if (renderer.render(outlineRenderState, context.submitNodeCollector(), context.poseStack(),
                        context.levelState())) {
                    renderVanillaOutline = false;
                }
            }
            return renderVanillaOutline;
        });
    }

    @FunctionalInterface
    interface CustomBlockOutlineRenderer {
        /**
         * @return true to suppress rendering of the vanilla block outline.
         */
        boolean render(BlockOutlineRenderState blockOutlineRenderState,
                SubmitNodeCollector collector,
                PoseStack poseStack,
                LevelRenderState levelRenderState);
    }

    /*
     * Changes block outline rendering such that it renders only for individual parts, not for the entire part host.
     */
    private static void handleEvent(LevelExtractionContext context, @org.jetbrains.annotations.Nullable HitResult hit) {
        customRenderers.clear();

        var player = Minecraft.getInstance().player;
        var outlineRenderState = context.levelState().blockOutlineRenderState;
        if (player == null || outlineRenderState == null || !(hit instanceof BlockHitResult blockHitResult)
                || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        var camera = context.camera();
        var blockPos = blockHitResult.getBlockPos();
        var itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (AEConfig.instance().isPlacementPreviewEnabled()) {
            if (!itemInHand.isEmpty() && itemInHand.getItem() instanceof IPartItem<?> partItem) {
                var part = partItem.createPart();
                var placement = PartPlacement.getPartPlacement(player,
                        player.level(),
                        itemInHand,
                        blockPos,
                        blockHitResult.getDirection(),
                        blockHitResult.getLocation());
                if (placement != null) {
                    var cameraRelativePos = new Vec3(
                            placement.pos().getX() - camera.position().x,
                            placement.pos().getY() - camera.position().y,
                            placement.pos().getZ() - camera.position().z);
                    customRenderers.add(new PartPlacementPreviewRenderer(placement, part, cameraRelativePos));
                }
            }
        }

        // Hit test against all attached parts to highlight the part that is relevant
        var pos = blockPos;
        if (context.level().getBlockEntity(pos) instanceof IPartHost partHost) {
            var cameraRelativePos = new Vec3(
                    blockPos.getX() - camera.position().x,
                    blockPos.getY() - camera.position().y,
                    blockPos.getZ() - camera.position().z);

            // Rendering a preview of what is currently in hand has priority
            // If the item in hand is a facade and a block is hit, attempt facade placement
            if (AEConfig.instance().isPlacementPreviewEnabled()) {
                if (itemInHand.getItem() instanceof IFacadeItem facadeItem) {
                    var side = blockHitResult.getDirection();
                    var facade = facadeItem.createPartFromItemStack(itemInHand, side);
                    if (facade != null && FacadeItem.canPlaceFacade(partHost, facade)) {
                        // Maybe a bit hacky, but if there's no part on the side to support the facade
                        // We would render a cable anchor implicitly
                        boolean renderAnchor = partHost.getPart(side) == null;
                        customRenderers.add(
                                new FacadePlacementPreviewRenderer(side, facade, renderAnchor, cameraRelativePos));
                    }
                }
            }

            var selectedPart = partHost.selectPartWorld(blockHitResult.getLocation());
            boolean highContrast = outlineRenderState.highContrast();
            float lineWidth = Minecraft.getInstance().gameRenderer
                    .gameRenderState().windowRenderState.appropriateLineWidth;
            if (selectedPart.facade != null) {
                customRenderers.add(
                        new FacadeOutlineRenderer(selectedPart.facade, selectedPart.side, cameraRelativePos,
                                highContrast, lineWidth));
                return;
            }
            if (selectedPart.part != null) {
                customRenderers.add(new PartOutlineRenderer(selectedPart.part, selectedPart.side, cameraRelativePos,
                        highContrast, lineWidth));
                return;
            }
        }
    }

    record PartPlacementPreviewRenderer(PartPlacement.Placement placement,
            IPart part, Vec3 cameraRelativePos) implements CustomBlockOutlineRenderer {
        @Override
        public boolean render(BlockOutlineRenderState blockOutlineRenderState,
                SubmitNodeCollector collector,
                PoseStack poseStack,
                LevelRenderState levelRenderState) {
            // Render without depth test to also have a preview for parts inside blocks.
            renderPart(poseStack, collector, blockOutlineRenderState.isTranslucent(), cameraRelativePos, part,
                    placement.side(), PREVIEW_LINE_WIDTH, false,
                    true, true);
            renderPart(poseStack, collector, blockOutlineRenderState.isTranslucent(), cameraRelativePos, part,
                    placement.side(), PREVIEW_LINE_WIDTH, false,
                    true, false);
            return false;
        }
    }

    record FacadePlacementPreviewRenderer(Direction side, IFacadePart facade,
            boolean renderAnchor,
            Vec3 cameraRelativePos) implements CustomBlockOutlineRenderer {
        @Override
        public boolean render(BlockOutlineRenderState blockOutlineRenderState,
                SubmitNodeCollector collector,
                PoseStack poseStack,
                LevelRenderState levelRenderState) {
            // Use same rendering inside blocks as part preview.
            showFacadePlacementPreview(poseStack, cameraRelativePos, collector,
                    blockOutlineRenderState.isTranslucent(), true);
            showFacadePlacementPreview(poseStack, cameraRelativePos, collector,
                    blockOutlineRenderState.isTranslucent(), false);
            return false;
        }

        private void showFacadePlacementPreview(PoseStack poseStack,
                Vec3 cameraRelativePos,
                SubmitNodeCollector collector,
                boolean afterTerrain,
                boolean insideBlock) {
            if (renderAnchor) {
                var cableAnchor = AEParts.CABLE_ANCHOR.get().createPart();
                renderPart(poseStack, collector, afterTerrain, cameraRelativePos, cableAnchor, side, PREVIEW_LINE_WIDTH,
                        false, true,
                        insideBlock);
            }

            renderFacade(poseStack, collector, afterTerrain, cameraRelativePos, facade, side, PREVIEW_LINE_WIDTH, false,
                    true,
                    insideBlock);
        }
    }

    record FacadeOutlineRenderer(IFacadePart facade, Direction side,
            Vec3 cameraRelativePos,
            boolean highContrast,
            float lineWidth) implements CustomBlockOutlineRenderer {
        @Override
        public boolean render(BlockOutlineRenderState blockOutlineRenderState,
                SubmitNodeCollector collector,
                PoseStack poseStack,
                LevelRenderState levelRenderState) {
            renderFacade(poseStack, collector, blockOutlineRenderState.isTranslucent(), cameraRelativePos, facade, side,
                    lineWidth, highContrast, false,
                    false);

            return true;
        }
    }

    record PartOutlineRenderer(IPart part, Direction side,
            Vec3 cameraRelativePos,
            boolean highContrast,
            float lineWidth) implements CustomBlockOutlineRenderer {
        @Override
        public boolean render(BlockOutlineRenderState blockOutlineRenderState,
                SubmitNodeCollector collector,
                PoseStack poseStack,
                LevelRenderState levelRenderState) {
            renderPart(poseStack, collector, blockOutlineRenderState.isTranslucent(), cameraRelativePos, part, side,
                    lineWidth, highContrast, false, false);
            return true;
        }
    }

    private static void renderPart(PoseStack poseStack,
            SubmitNodeCollector collector,
            boolean afterTerrain,
            Vec3 cameraRelativePos,
            IPart part,
            Direction side,
            float lineWidth,
            boolean highContrast,
            boolean preview,
            boolean insideBlock) {
        var boxes = new ArrayList<AABB>();
        var helper = new BusCollisionHelper(boxes, side, true);
        part.getBoxes(helper);
        renderBoxes(poseStack, collector, afterTerrain, cameraRelativePos, boxes, lineWidth, highContrast, preview,
                insideBlock);
    }

    private static void renderFacade(PoseStack poseStack,
            SubmitNodeCollector collector,
            boolean afterTerrain,
            Vec3 cameraRelativePos,
            IFacadePart facade,
            Direction side,
            float lineWidth,
            boolean highContrast,
            boolean preview,
            boolean insideBlock) {
        var boxes = new ArrayList<AABB>();
        var helper = new BusCollisionHelper(boxes, side, true);
        facade.getBoxes(helper, false);
        renderBoxes(poseStack, collector, afterTerrain, cameraRelativePos, boxes, lineWidth, highContrast, preview,
                insideBlock);
    }

    private static void renderBoxes(PoseStack poseStack,
            SubmitNodeCollector collector,
            boolean afterTerrain,
            Vec3 cameraRelativePos,
            List<AABB> boxes,
            float lineWidth,
            boolean highContrast,
            boolean preview,
            boolean insideBlock) {
        if (preview) {
            RenderType renderType = insideBlock ? AERenderTypes.LINES_BEHIND_BLOCK : getOutlineRenderType(false);
            int color = ARGB.white(insideBlock ? 0.2f : 0.6f);
            renderBoxes(poseStack, collector, afterTerrain, cameraRelativePos, boxes, renderType, color, lineWidth);
        } else {
            if (highContrast) {
                renderBoxes(poseStack, collector, afterTerrain, cameraRelativePos, boxes,
                        RenderTypes.secondaryBlockOutline(),
                        CommonColors.BLACK, HIGH_CONTRAST_SECONDARY_LINE_WIDTH);
            }
            int color = highContrast ? CommonColors.HIGH_CONTRAST_DIAMOND : ARGB.black(0.4f);
            renderBoxes(poseStack, collector, afterTerrain, cameraRelativePos, boxes,
                    getOutlineRenderType(highContrast), color, lineWidth);
        }
    }

    /**
     * Picks the render type for outlines the same way as vanilla's block outline rendering. When improved transparency
     * is enabled, translucent outlines are rendered using OIT, which requires render types that support it.
     */
    private static RenderType getOutlineRenderType(boolean highContrast) {
        if (highContrast) {
            return RenderTypes.linesDepthBias();
        } else if (Minecraft.getInstance().gameRenderer.useImprovedTransparency()) {
            return RenderTypes.linesTranslucentNoDepthWrite();
        } else {
            return RenderTypes.linesTranslucent();
        }
    }

    private static void renderBoxes(PoseStack poseStack,
            SubmitNodeCollector collector,
            boolean afterTerrain,
            Vec3 cameraRelativePos,
            List<AABB> boxes,
            RenderType renderType,
            int color,
            float lineWidth) {
        poseStack.pushPose();
        poseStack.translate(cameraRelativePos.x, cameraRelativePos.y, cameraRelativePos.z);
        for (var box : boxes) {
            collector.submitShapeOutline(poseStack, Shapes.create(box), renderType, color, lineWidth, afterTerrain);
        }
        poseStack.popPose();
    }
}
