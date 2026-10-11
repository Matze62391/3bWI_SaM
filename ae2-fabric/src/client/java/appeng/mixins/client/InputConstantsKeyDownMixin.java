package appeng.mixins.client;

import java.util.HashSet;
import java.util.Set;

import org.lwjgl.sdl.SDLKeyboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * {@link InputConstants#isKeyDown} indexes the SDL keyboard state without a bounds check. A key mapping bound to an
 * invalid scancode (e.g. a stale GLFW/SDL keycode in options.txt or a mod's default) then crashes the client as soon as
 * the mouse is grabbed (i.e. when joining a world). Treat such keys as "not pressed" and report the offending mappings
 * once.
 */
@Mixin(InputConstants.class)
public class InputConstantsKeyDownMixin {
    @Unique
    private static final Logger ae2_LOGGER = LoggerFactory.getLogger("ae2");
    @Unique
    private static final Set<Integer> ae2_reportedKeys = new HashSet<>();

    @Inject(method = "isKeyDown", at = @At("HEAD"), cancellable = true)
    private static void ae2_guardInvalidKey(int key, CallbackInfoReturnable<Boolean> cir) {
        var state = SDLKeyboard.SDL_GetKeyboardState();
        if (state != null && (key < 0 || key >= state.limit())) {
            cir.setReturnValue(false);
            if (ae2_reportedKeys.add(key)) {
                var names = new StringBuilder();
                var options = Minecraft.getInstance().options;
                if (options != null) {
                    for (KeyMapping mapping : options.keyMappings) {
                        var bound = KeyMappingHelper.getBoundKeyOf(mapping);
                        if (bound.getType() == InputConstants.Type.KEYBOARD && bound.getValue() == key) {
                            if (!names.isEmpty()) {
                                names.append(", ");
                            }
                            names.append(mapping.getName());
                        }
                    }
                }
                ae2_LOGGER.warn("Ignoring invalid keyboard key code {} (bound by: {}). Rebind it in Options > Controls.",
                        key, names.isEmpty() ? "unknown" : names);
            }
        }
    }
}
