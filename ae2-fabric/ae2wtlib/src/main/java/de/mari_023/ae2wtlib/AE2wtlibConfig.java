package de.mari_023.ae2wtlib;

import net.fabricmc.loader.api.FabricLoader;

import appeng.core.config.ConfigSpec;

import de.mari_023.ae2wtlib.api.AE2wtlibAPI;

/**
 * ae2wtlib's common config, stored in {@code config/ae2wtlib.json} (NeoForge: {@code ae2wtlib.toml}).
 */
public record AE2wtlibConfig(ConfigSpec.DoubleValue magnetCardRangeValue) {
    public static final AE2wtlibConfig CONFIG;
    public static final ConfigSpec SPEC;

    static {
        var builder = new ConfigSpec.Builder();
        CONFIG = new AE2wtlibConfig(builder.comment("Range of the magnet card in blocks")
                .defineInRange("magnet_card_range", 16.0, 0.0, 64.0));
        SPEC = builder.build();
    }

    public static void load() {
        SPEC.load(FabricLoader.getInstance().getConfigDir().resolve(AE2wtlibAPI.MOD_NAME + ".json"));
    }

    public double magnetCardRange() {
        return magnetCardRangeValue().get();
    }
}
