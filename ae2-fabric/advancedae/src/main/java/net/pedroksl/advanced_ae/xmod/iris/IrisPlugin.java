package net.pedroksl.advanced_ae.xmod.iris;

import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;
import net.irisshaders.iris.api.v0.IrisShadowProgram;
import net.pedroksl.advanced_ae.client.renderer.QuantumArmorRenderer;
import net.pedroksl.advanced_ae.xmod.Addons;

public class IrisPlugin {

    public static boolean isShaderPackInUse() {
        try {
            return Addons.IRIS.isLoaded() && IrisApi.getInstance().isShaderPackInUse();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tells Iris which shader pack programs to use for GeckoLib's glowing layer pipeline, which the quantum armor's
     * helmet and chestplate use for their glowmasks. Iris does not know this pipeline and otherwise picks a program by
     * guessing, which can render the glowing parts as stretched shards with some shader packs and GPUs.
     */
    public static void assignPipelines() {
        if (!Addons.IRIS.isLoaded()) {
            return;
        }
        var iris = IrisApi.getInstance();
        var pipeline = QuantumArmorRenderer.emissivePipeline();
        iris.assignPipeline(pipeline, IrisProgram.EMISSIVE_ENTITIES);
        iris.assignPipelineShadow(pipeline, IrisShadowProgram.SHADOW_ENTITIES);
    }
}
