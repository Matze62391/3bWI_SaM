package appeng.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric entrypoint for AE2. Creates the side-specific mod instance, which registers all content.
 * <p>
 * On NeoForge, the {@code @Mod} annotation selects the class per distribution.
 */
public final class AppEngFabricEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            createClient();
        } else {
            new AppEngServer();
        }
    }

    /**
     * Loaded reflectively so that the client class is never touched on a dedicated server.
     */
    private static void createClient() {
        try {
            Class.forName("appeng.client.AppEngClient").getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to initialize the AE2 client", e);
        }
    }
}
