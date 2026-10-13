package com.glodblock.github.appflux;

import com.glodblock.github.appflux.client.AFClientRegistryHandler;
import com.glodblock.github.appflux.network.AFNetworkHandler;
import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric client entrypoint. Runs after all {@code main} entrypoints, so Applied Flux's content exists.
 */
public class AppFluxClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AFClientRegistryHandler.INSTANCE.init();
        AFNetworkHandler.INSTANCE.registerClient();
    }

}
