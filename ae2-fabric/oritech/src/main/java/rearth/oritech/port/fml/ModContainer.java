package rearth.oritech.port.fml;

import net.fabricmc.loader.api.FabricLoader;

import rearth.oritech.port.fml.config.ModConfig;
import rearth.oritech.port.neoforge.common.ModConfigSpec;

/**
 * Loads config specs from {@code config/<modid>-<type>.toml}.
 */
public final class ModContainer {
    private final String modId;

    public ModContainer(String modId) {
        this.modId = modId;
    }

    public String getModId() {
        return modId;
    }

    public void registerConfig(ModConfig.Type type, ModConfigSpec spec) {
        var file = FabricLoader.getInstance().getConfigDir().resolve(modId + "-" + type.extension() + ".toml");
        spec.load(file);
    }
}
