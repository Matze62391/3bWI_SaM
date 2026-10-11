package net.pedroksl.ae2addonlib.util;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Interface that adds helper methods for an enumeration of integration mods.
 */
public interface AddonEnum {

    /**
     * Reads the entry's mod id.
     * @return The entry's mod id
     */
    String getModId();

    /**
     * Reads the entry's mod name.
     * @return The entry's mod name.
     */
    String getModName();

    /**
     * Helper method to check if an integrated mod is loaded
     * @return If the requested mod is loaded or not.
     */
    default boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded(getModId());
    }
}
