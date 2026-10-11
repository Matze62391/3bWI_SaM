package appeng.mixins.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.WeatherEffectRenderer;

import appeng.client.renderer.spatialstorage.SpatialStorageEnvironment;

/**
 * The spatial storage dimension has no weather.
 */
@Mixin(WeatherEffectRenderer.class)
public abstract class SpatialStorageWeatherMixin {
    @Inject(method = {
            "render(Lnet/minecraft/client/renderer/state/level/WeatherRenderState;Lcom/mojang/renderpearl/api/commands/RenderPass;)V",
            "renderOit" }, at = @At("HEAD"), cancellable = true)
    private void ae2$skipSpatialWeather(CallbackInfo ci) {
        if (SpatialStorageEnvironment.isActive()) {
            ci.cancel();
        }
    }
}
