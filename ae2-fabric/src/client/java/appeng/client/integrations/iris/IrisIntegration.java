package appeng.client.integrations.iris;

import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;
import net.irisshaders.iris.api.v0.IrisShadowProgram;

import appeng.client.render.AERenderPipelines;

/**
 * Tells Iris which shader pack program to use for AE2's own render pipelines. Without this, Iris logs an error and
 * may render them incorrectly while a shader pack is active. Only loaded when Iris is installed.
 */
public final class IrisIntegration {
    private IrisIntegration() {
    }

    public static void register() {
        var iris = IrisApi.getInstance();
        iris.assignPipeline(AERenderPipelines.STORAGE_CELL_LEDS, IrisProgram.BASIC);
        iris.assignPipeline(AERenderPipelines.AREA_OVERLAY_FACE, IrisProgram.BASIC);
        iris.assignPipeline(AERenderPipelines.LINES_BEHIND_BLOCK, IrisProgram.LINES);
        iris.assignPipeline(AERenderPipelines.AREA_OVERLAY_LINE, IrisProgram.LINES);
        iris.assignPipeline(AERenderPipelines.AREA_OVERLAY_LINE_OCCLUDED, IrisProgram.LINES);
        iris.assignPipeline(AERenderPipelines.SPATIAL_SKYBOX, IrisProgram.SKY_BASIC);
        iris.assignPipeline(AERenderPipelines.SPATIAL_SKYBOX_SPARKLES, IrisProgram.SKY_BASIC);
        iris.assignPipeline(AERenderPipelines.LIGHTNING_FX, IrisProgram.PARTICLES_TRANSLUCENT);

        // Some shader packs also render block entities (and with them the drive LEDs) into the shadow map
        iris.assignPipelineShadow(AERenderPipelines.STORAGE_CELL_LEDS, IrisShadowProgram.SHADOW);
    }
}
