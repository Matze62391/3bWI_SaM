package com.glodblock.github.appflux.config;

import com.glodblock.github.appflux.AppFlux;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Applied Flux's config, stored in {@code config/appflux.json} (NeoForge: {@code appflux-common.toml}). Same options,
 * comments are written as {@code //} lines.
 */
public class AFConfig {

    private static int fluxPerByte = 1024 * 1024;
    private static long fluxAccessorIO = 0;
    private static boolean selfCharge = false;
    private static boolean allowImportBus = false;

    public static int getFluxPerByte() {
        return fluxPerByte;
    }

    public static long getFluxAccessorIO() {
        return fluxAccessorIO <= 0 ? Long.MAX_VALUE : fluxAccessorIO;
    }

    public static boolean selfCharge() {
        return selfCharge;
    }

    public static boolean allowImport() {
        return allowImportBus;
    }

    public static boolean miSupport() {
        return false;
    }

    public static void load() {
        var file = FabricLoader.getInstance().getConfigDir().resolve(AppFlux.MODID + ".json");
        if (Files.exists(file)) {
            try {
                var reader = new JsonReader(new StringReader(Files.readString(file, StandardCharsets.UTF_8)));
                reader.setStrictness(Strictness.LENIENT);
                var root = JsonParser.parseReader(reader).getAsJsonObject();
                fluxPerByte = Math.clamp(getLong(root, "flux_cell", "amount", fluxPerByte), 1, Integer.MAX_VALUE);
                fluxAccessorIO = Math.clamp(getLong(root, "flux_accessor", "io_limit", fluxAccessorIO), 0, Integer.MAX_VALUE);
                selfCharge = getBool(root, "flux_accessor", "enable", selfCharge);
                allowImportBus = getBool(root, "misc", "enable", allowImportBus);
            } catch (IOException | RuntimeException e) {
                AppFlux.LOGGER.error("Failed to read config file {}, using defaults", file, e);
            }
        }
        var out = """
                {
                  "flux_cell": {
                    // Energy (E) that can be stored per byte.
                    // Default: 1048576, Range: 1 ~ 2147483647
                    "amount": %d
                  },
                  "flux_accessor": {
                    // The I/O limit of Flux Accessor. 0 means no limitation.
                    // Default: 0, Range: 0 ~ 2147483647
                    "io_limit": %d,
                    // Allow Flux Accessor to charge ME network with stored energy.
                    // Default: false
                    "enable": %b
                  },
                  "misc": {
                    // Allow ME Import Bus to import energy like items/fluids.
                    // Default: false
                    "enable": %b
                  }
                }
                """.formatted(fluxPerByte, fluxAccessorIO, selfCharge, allowImportBus);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            AppFlux.LOGGER.error("Failed to write config file {}", file, e);
        }
    }

    private static long getLong(JsonObject root, String section, String key, long def) {
        return root.get(section) instanceof JsonObject obj && obj.has(key) ? obj.get(key).getAsLong() : def;
    }

    private static boolean getBool(JsonObject root, String section, String key, boolean def) {
        return root.get(section) instanceof JsonObject obj && obj.has(key) ? obj.get(key).getAsBoolean() : def;
    }

}
