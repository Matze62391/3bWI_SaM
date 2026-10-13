package rearth.oritech.client.ui.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import rearth.oritech.client.renderers.util.RenderHelpers;

/**
 * Renders block models and block entities into the GUI picture-in-picture target.
 */
public class BlockPreviewPipRenderer extends PictureInPictureRenderer<BlockPreviewRenderState> {
    private BlockPreviewRenderState cachedState;
    private Object cachedModel;

    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    private net.minecraft.client.renderer.block.BlockModelResolver modelResolver;

    @Override
    public Class<BlockPreviewRenderState> getRenderStateClass() {
        return BlockPreviewRenderState.class;
    }

    @Override
    protected boolean textureIsReadyToBlit(BlockPreviewRenderState state) {
        var previous = cachedState;
        return state.cacheTexture() && previous != null
                && state.blocks().equals(previous.blocks())
                && state.rotationX() == previous.rotationX() && state.rotationY() == previous.rotationY()
                && state.centerX() == previous.centerX() && state.centerY() == previous.centerY()
                && state.centerZ() == previous.centerZ() && state.scale() == previous.scale()
                && cachedModel == Minecraft.getInstance().getModelManager().getBlockModelSet();
    }

    @Override
    protected void renderToTexture(BlockPreviewRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodes) {
        var minecraft = Minecraft.getInstance();
        var modelState = new BlockModelRenderState();

        minecraft.gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);

        // GUI coordinates point down and the PIP base transform flips depth.
        poseStack.scale(1f, -1f, -1f);
        poseStack.rotate(Axis.XP.rotationDegrees(state.rotationX()));
        poseStack.rotate(Axis.YP.rotationDegrees(state.rotationY()));
        poseStack.translate(-state.centerX(), -state.centerY(), -state.centerZ());

        for (var entry : state.blocks()) {
            poseStack.pushPose();
            // Block models occupy [0, 1]; offsets describe block centers.
            poseStack.translate(
                    entry.offset().getX() - 0.5f,
                    entry.offset().getY() - 0.5f,
                    entry.offset().getZ() - 0.5f
            );
            if (entry.scale() != 1f) {
                poseStack.translate(0.5f, 0.5f, 0.5f);
                poseStack.scale(entry.scale(), entry.scale(), entry.scale());
                poseStack.translate(-0.5f, -0.5f, -0.5f);
            }

            if (modelResolver == null) modelResolver = new net.minecraft.client.renderer.block.BlockModelResolver(minecraft.getModelManager());
            modelResolver.update(modelState, entry.state(), DISPLAY_CONTEXT);
            modelState.submit(poseStack, submitNodes, RenderHelpers.FULL_BRIGHT, entry.overlayCoords(), 0);

            if (entry.entity() != null) {
                var dispatcher = minecraft.getBlockEntityRenderDispatcher();
                var renderer = dispatcher.getRenderer(entry.entity());
                var blockEntityState = renderer == null ? null : dispatcher
                        .tryExtractRenderState(entry.entity(), state.partialTick(), null, renderer.shouldRenderOffScreen());
                if (blockEntityState != null) {
                    var cameraState = minecraft.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
                    minecraft.getBlockEntityRenderDispatcher()
                            .submit(blockEntityState, poseStack, submitNodes, cameraState);
                }
            }
            poseStack.popPose();
        }

        cachedState = state.cacheTexture() ? state : null;
        cachedModel = minecraft.getModelManager().getBlockModelSet();
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "oritech_block_preview";
    }
}
