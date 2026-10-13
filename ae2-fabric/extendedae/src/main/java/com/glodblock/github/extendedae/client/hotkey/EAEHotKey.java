package com.glodblock.github.extendedae.client.hotkey;

import com.glodblock.github.extendedae.ExtendedAE;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

public class EAEHotKey {

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(ExtendedAE.id("key.extendedae.category"));
    public static final KeyMapping VIEW_PATTERN = new KeyMapping("key.extendedae.viewpattern", InputConstants.KEY_P, CATEGORY);
    public static final KeyMapping SET_AMOUNT = new KeyMapping("key.extendedae.set_amount", InputConstants.Type.MOUSE, 2, CATEGORY);

    public static boolean pressed(KeyMapping hotkey) {
        var key = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(hotkey);
        if (key.getType() != InputConstants.Type.KEYBOARD) {
            return hotkey.isDown();
        }
        return InputConstants.isKeyDown(key.getValue());
    }

    public static boolean isKeyBound(KeyMapping hotkey) {
        return !hotkey.isUnbound();
    }

}
