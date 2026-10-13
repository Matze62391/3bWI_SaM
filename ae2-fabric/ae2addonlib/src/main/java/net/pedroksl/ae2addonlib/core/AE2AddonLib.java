package net.pedroksl.ae2addonlib.core;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import net.minecraft.resources.Identifier;
import net.fabricmc.api.ModInitializer;
import net.pedroksl.ae2addonlib.core.network.LibNetworkHandler;
import net.pedroksl.ae2addonlib.registry.helpers.LibComponents;
import net.pedroksl.ae2addonlib.registry.helpers.LibMenus;

/**
 * Main lib class.
 */
public class AE2AddonLib implements ModInitializer {
    /**
     * The MOD_ID of this lib
     */
    public static final String MOD_ID = "ae2addonlib";
    /**
     * A static instance of this lib's main class.
     */
    public static AE2AddonLib INSTANCE;

    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        assert INSTANCE == null;
        INSTANCE = this;

        LibMenus.INSTANCE.register();
        LibComponents.INSTANCE.register();
        LibNetworkHandler.INSTANCE.register();
    }

    /**
     * Helper method to easily create identifiers using this lib's namespace.
     * @param path The path of the desired identifier.
     * @return A constructed {@link Identifier} in this lib's namespace.
     */
    public static Identifier makeId(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

}
