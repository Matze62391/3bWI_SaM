package net.pedroksl.ae2addonlib.config;

/**
 * Replacement for NeoForge's ModConfig, which only provides the config types here.
 */
public final class ModConfig {
    private ModConfig() {
    }

    public enum Type {
        COMMON,
        CLIENT,
        SERVER;

        public String extension() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
