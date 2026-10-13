package rearth.oritech.client.renderers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import rearth.oritech.port.neoforge.client.model.pipeline.VertexConsumerWrapper;
import rearth.oritech.client.init.OritechClientConfig;

/** Glowing interference bands on the actual fluid surface, including slopes and waterfalls. */
public final class StrangeMatterFluidRenderer implements FluidRenderHandler {

    public static final StrangeMatterFluidRenderer INSTANCE = new StrangeMatterFluidRenderer();

    private StrangeMatterFluidRenderer() {
    }

    @Override
    public void renderFluid(FluidRenderer fluidRenderer, BlockPos pos, BlockAndTintGetter level,
                            FluidRenderer.Output output, BlockState blockState, FluidState fluidState) {
        if (!OritechClientConfig.renderFullEffects()) {
            FluidRenderHandler.super.renderFluid(fluidRenderer, pos, level, output, blockState, fluidState);
            return;
        }
        var model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluidState);
        var sprite = model.stillMaterial().sprite();
        // Tint and mirror the existing animated texture on one mesh. Extra horizontal quads cannot
        // follow vanilla's averaged corner heights and cut through flowing surfaces.
        FluidRenderHandler.super.renderFluid(fluidRenderer, pos, level, layer -> new VertexConsumerWrapper(output.getBuilder(layer)) {
            @Override
            public void addVertex(float x, float y, float z, int color, float u, float v,
                                  int overlay, int light, float normalX, float normalY, float normalZ) {
                // World coordinates keep the bands continuous across blocks and chunk boundaries.
                double worldX = (pos.getX() & ~15) + x;
                double worldY = (pos.getY() & ~15) + y;
                double worldZ = (pos.getZ() & ~15) + z;
                float band = (float) (0.5 + 0.5 * Math.sin(worldY * 2 + (worldX + worldZ) * 0.8));
                int red = (int) (90 + 130 * band);
                int green = (int) (210 - 140 * band);
                int tint = (color & 0xFF000000) | (red << 16) | (green << 8) | 255;
                // Reverse the texture's flow for an uncanny counter-current, with violet/cyan bands.
                parent.addVertex(x, y, z, tint, sprite.getU0() + sprite.getU1() - u,
                    sprite.getV0() + sprite.getV1() - v, overlay, LightCoordsUtil.FULL_BRIGHT,
                    normalX, normalY, normalZ);
            }
        }, blockState, fluidState);
    }
}
