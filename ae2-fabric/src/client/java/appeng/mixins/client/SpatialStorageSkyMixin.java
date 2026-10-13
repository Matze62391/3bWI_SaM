package appeng.mixins.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;

import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;

import appeng.client.renderer.spatialstorage.SpatialStorageEnvironment;

@Mixin(SkyRenderer.class)
public abstract class SpatialStorageSkyMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void ae2$renderSpatialSky(GpuBufferSlice skyFog, SkyRenderState state, CallbackInfo ci) {
        if (SpatialStorageEnvironment.isActive()
                && SpatialStorageEnvironment.getSkyRenderer().renderSky(RenderSystem.getModelViewMatrixCopy())) {
            ci.cancel();
        }
    }
}
