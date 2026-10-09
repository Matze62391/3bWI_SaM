package appeng.mixins.client;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;

import appeng.client.AppEngClient;

/**
 * Lets items that react to the mouse wheel (while a modifier key is held) consume the scroll event.
 */
@Mixin(MouseHandler.class)
public abstract class MouseScrollMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void ae2$onScroll(long handle, double xoffset, double yoffset, CallbackInfo ci) {
        if (handle != 0L && handle == minecraft.getWindow().handle() && minecraft.gui.screen() == null
                && minecraft.gui.overlay() == null && minecraft.player != null) {
            if (AppEngClient.handleMouseWheel(yoffset)) {
                ci.cancel();
            }
        }
    }
}
