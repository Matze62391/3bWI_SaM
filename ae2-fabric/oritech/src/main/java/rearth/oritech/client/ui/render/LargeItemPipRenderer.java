package rearth.oritech.client.ui.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;

public class LargeItemPipRenderer extends PictureInPictureRenderer<LargeItemRenderState> {

    @Override
    public Class<LargeItemRenderState> getRenderStateClass() {
        return LargeItemRenderState.class;
    }

    @Override
    protected void renderToTexture(LargeItemRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        var minecraft = Minecraft.getInstance();
        var itemRenderState = new TrackingItemStackRenderState();
        minecraft.getItemModelResolver().updateForTopItem(
                itemRenderState,
                renderState.stack(),
                ItemDisplayContext.GUI,
                minecraft.level,
                minecraft.player,
                0
        );

        poseStack.scale(1.0F, -1.0F, -1.0F);

        if (itemRenderState.usesBlockLight()) {
            minecraft.gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);
        } else {
            minecraft.gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_FLAT);
        }

        itemRenderState.submit(poseStack, submitNodeCollector, 15728880, OverlayTexture.NO_OVERLAY, 0);
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "oritech_large_item";
    }
}
