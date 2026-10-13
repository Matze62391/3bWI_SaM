package net.pedroksl.advanced_ae.xmod.modmenu;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.pedroksl.advanced_ae.common.definitions.AAEConfig;
import net.pedroksl.ae2addonlib.client.config.ModConfigScreen;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Lets Mod Menu open Advanced AE's config screen (NeoForge generates it automatically). Only loaded when Mod Menu is
 * installed.
 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new ModConfigScreen(
                parent,
                Component.literal("Advanced AE"),
                List.of(
                        new ModConfigScreen.Section(Component.literal("Client"), AAEConfig.instance().getClientSpec()),
                        new ModConfigScreen.Section(
                                Component.literal("Common (only applies to worlds hosted by this game)"),
                                AAEConfig.instance().getCommonSpec())));
    }
}
