package appeng.client.integrations.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import appeng.client.gui.config.AEConfigScreen;

/**
 * Lets Mod Menu open AE2's config screen. Only loaded when Mod Menu is installed.
 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return AEConfigScreen::new;
    }
}
