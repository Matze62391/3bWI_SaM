package rearth.oritech.port.fml.config;

public final class ModConfig {
    private ModConfig() {}

    public enum Type {
        COMMON,
        CLIENT,
        SERVER,
        STARTUP;

        public String extension() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
