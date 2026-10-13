package com.glodblock.github.extendedae;

import com.glodblock.github.extendedae.client.ClientRegistryHandler;
import com.glodblock.github.extendedae.client.hooks.TagHook;
import com.glodblock.github.extendedae.client.hotkey.PatternHotKey;
import com.glodblock.github.extendedae.common.hooks.CutterHook;
import com.glodblock.github.extendedae.network.EAENetworkHandler;
import com.glodblock.github.glodium.xmod.XModManager;
import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric client entrypoint. Client entrypoints run after all {@code main} entrypoints (and thus after
 * {@code ae2:addon}), so all of ExtendedAE's content exists at this point.
 */
public class ExtendedAEClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientRegistryHandler.INSTANCE.init();
        EAENetworkHandler.INSTANCE.registerClient();
        PatternHotKey.onInit();
        TagHook.onInit();
        CutterHook.addTooltip();
        XModManager.client(ExtendedAE.MODID);
    }

}
