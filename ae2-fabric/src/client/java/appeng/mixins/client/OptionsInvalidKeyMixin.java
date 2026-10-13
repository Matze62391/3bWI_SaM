package appeng.mixins.client;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;

/**
 * Before 26.3 "unbound" was keyboard key -1 (GLFW_KEY_UNKNOWN); now it is 0 (SDL_SCANCODE_UNKNOWN). Older versions of
 * the AE2 addons saved "key.keyboard.-1" to options.txt, which would crash {@link KeyMapping#setAll()}. Migrate any
 * negative keyboard binding back to "unbound" when the options are loaded.
 */
@Mixin(Options.class)
public class OptionsInvalidKeyMixin {
    @Shadow
    @Final
    public KeyMapping[] keyMappings;

    @Inject(method = "load", at = @At("TAIL"))
    private void ae2_migrateNegativeKeys(CallbackInfo ci) {
        boolean changed = false;
        for (var mapping : keyMappings) {
            var bound = KeyMappingHelper.getBoundKeyOf(mapping);
            if (bound.getType() == InputConstants.Type.KEYBOARD && bound.getValue() < 0) {
                mapping.setKey(InputConstants.UNKNOWN);
                changed = true;
            }
        }
        if (changed) {
            KeyMapping.resetMapping();
        }
    }
}
