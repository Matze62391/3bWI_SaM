package net.pedroksl.advanced_ae.xmod;

import net.fabricmc.api.ModInitializer;
import net.pedroksl.advanced_ae.xmod.appflux.AppliedFluxPlugin;

/**
 * Integrations with other AE2 addons. Runs after all addons are initialized ({@code ae2:post_addon}), so their items
 * exist.
 */
public class AAEPostAddon implements ModInitializer {
    @Override
    public void onInitialize() {
        if (Addons.APPFLUX.isLoaded()) {
            AppliedFluxPlugin.init();
        }
    }
}
