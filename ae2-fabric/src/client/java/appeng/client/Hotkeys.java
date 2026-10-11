package appeng.client;

import java.util.HashMap;
import java.util.function.Consumer;

import com.mojang.blaze3d.platform.InputConstants;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.KeyMapping;

import appeng.core.AppEng;
import appeng.hotkeys.HotkeyActions;

/**
 * client side component of {@link HotkeyActions}
 */
public class Hotkeys {

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(AppEng.makeId("category"));

    private static final HashMap<String, Hotkey> HOTKEYS = new HashMap<>();

    @Nullable
    private static Consumer<KeyMapping> keyMappingRegistrar;

    private static Hotkey createHotkey(String id) {
        return new Hotkey(id, new KeyMapping("key.ae2." + id, InputConstants.UNKNOWN.getValue(), CATEGORY));
    }

    private static void registerHotkey(Hotkey hotkey) {
        HOTKEYS.put(hotkey.name(), hotkey);
        // Addons (ae2:addon entrypoint) register their hotkeys after AE2 finalized its own
        if (keyMappingRegistrar != null) {
            keyMappingRegistrar.accept(hotkey.mapping());
        }
    }

    public static void finalizeRegistration(Consumer<KeyMapping> register) {
        for (var value : HOTKEYS.values()) {
            register.accept(value.mapping());
        }
        keyMappingRegistrar = register;
    }

    public static void registerHotkey(String id) {
        registerHotkey(createHotkey(id));
    }

    public static void checkHotkeys() {
        HOTKEYS.forEach((name, hotkey) -> hotkey.check());
    }

    @Nullable
    public static Hotkey getHotkeyMapping(@Nullable String id) {
        return HOTKEYS.get(id);
    }
}
