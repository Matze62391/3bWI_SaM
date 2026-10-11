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
    /**
     * Entrypoint ({@link ModInitializer}) for addons that use AE2's content while they initialize, for example to
     * register upgrades for AE2's items. Fabric doesn't order the {@code main} entrypoints of different mods, so these
     * addons are initialized by AE2 once all of its content is registered.
     */
    public static final String ADDON_ENTRYPOINT = "ae2:addon";

    /**
     * Entrypoint ({@link ModInitializer}) that is called after all {@link #ADDON_ENTRYPOINT addons} were initialized.
     * Addons use it for integrations with other addons, e.g. to register upgrades for another addon's items, because
     * the addons themselves are initialized in no particular order.
     */
    public static final String POST_ADDON_ENTRYPOINT = "ae2:post_addon";

    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            createClient();
        } else {
            new AppEngServer();
        }

        FabricLoader.getInstance().invokeEntrypoints(ADDON_ENTRYPOINT, ModInitializer.class,
                ModInitializer::onInitialize);
        FabricLoader.getInstance().invokeEntrypoints(POST_ADDON_ENTRYPOINT, ModInitializer.class,
                ModInitializer::onInitialize);
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
