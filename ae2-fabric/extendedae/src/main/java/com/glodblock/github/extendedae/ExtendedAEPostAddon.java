package com.glodblock.github.extendedae;

import com.glodblock.github.glodium.xmod.XModManager;
import net.fabricmc.api.ModInitializer;

/**
 * Runs ExtendedAE's integrations with other AE2 addons ({@code ae2:post_addon}, after all addons are initialized, so
 * their items exist).
 */
public class ExtendedAEPostAddon implements ModInitializer {
    @Override
    public void onInitialize() {
        XModManager.common(ExtendedAE.MODID);
    }
}
