package rearth.oritech.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.oritech.OritechClient;

@Mixin(MouseHandler.class)
public class RailgunMouseMixin {

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void oritech$railgunClick(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        if (OritechClient.onMouseButton(Minecraft.getInstance(), info.button(), action)) {
            ci.cancel();
        }
    }
}
