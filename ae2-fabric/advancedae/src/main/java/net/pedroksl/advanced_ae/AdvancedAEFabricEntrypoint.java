package net.pedroksl.advanced_ae;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Creates the client or server side of Advanced AE (on NeoForge, the loader picks one of the two {@code @Mod}
 * classes by distribution).
 */
public final class AdvancedAEFabricEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            createClient();
        } else {
            new AdvancedAEServer();
        }
    }

    /**
     * Loaded reflectively so that the client class is never touched on a dedicated server.
     */
    private static void createClient() {
        try {
            Class.forName("net.pedroksl.advanced_ae.client.AAEClient").getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to initialize the Advanced AE client", e);
        }
    }
}
