package appeng.mixins.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.CloudRenderer;

import appeng.client.renderer.spatialstorage.SpatialStorageEnvironment;

/**
 * The spatial storage dimension has no clouds.
 */
@Mixin(CloudRenderer.class)
public abstract class SpatialStorageCloudsMixin {
    @Inject(method = {
            "render(Lnet/minecraft/client/CloudStatus;Lcom/mojang/renderpearl/api/commands/RenderPass;)V",
            "renderOit" }, at = @At("HEAD"), cancellable = true)
    private void ae2$skipSpatialClouds(CallbackInfo ci) {
        if (SpatialStorageEnvironment.isActive()) {
            ci.cancel();
        }
    }
}
