package com.glodblock.github.extendedae.xmod.modmenu;

import com.glodblock.github.extendedae.client.gui.config.EAEConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Lets Mod Menu open ExtendedAE's config screen (NeoForge generates it automatically). Only loaded when Mod Menu is
 * installed.
 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return EAEConfigScreen::new;
    }
}
