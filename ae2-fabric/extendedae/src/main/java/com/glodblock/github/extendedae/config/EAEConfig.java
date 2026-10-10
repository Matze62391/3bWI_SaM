package com.glodblock.github.extendedae.config;

import appeng.api.stacks.AEKeyType;
import com.glodblock.github.extendedae.ExtendedAE;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import it.unimi.dsi.fastutil.ints.IntImmutableList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ExtendedAE's config, stored in {@code config/extendedae.json} (NeoForge: {@code extendedae-common.toml}). The
 * options and their sections are the same as on NeoForge. Comments are written as {@code //} lines.
 */
public class EAEConfig {

    private static final IntList defaultModifierMultiplier = new IntImmutableList(new int[]{2, 3, 5, 7});

    private static final Map<String, Option<?>> OPTIONS = new LinkedHashMap<>();

    private static final Option<Integer> EX_BUS_SPEED = intOption(
            "device.extended_io_bus_multiplier", "ME Extend Import/Export Bus speed multiplier", 8, 2, 128);
    private static final Option<Double> INFINITY_CELL_ENERGY = doubleOption(
            "item.infinity_cell_energy_cost", "ME Infinity Cell idle energy cost (unit: AE/t)", 8.0, 0.1, 64.0);
    private static final Option<Double> WIRELESS_CONNECTOR_RANGE = doubleOption(
            "device.wireless_connector_max_range", "The max range between two wireless connector", 1000.0, 10.0, 10000.0);
    private static final Option<Double> WIRELESS_CONNECTOR_POWER_MULTIPLIER = doubleOption(
            "device.wireless_connector_power_multiplier", "Power usage multiplier for wireless connectors", 1.0, 0.0, 100.0);
    private static final Option<List<Integer>> PATTERN_MODIFIER_NUMBER = listOption(
            "item.pattern_modifier_multipliers", "Pattern modifier multipliers", List.copyOf(defaultModifierMultiplier),
            JsonPrimitive::new, e -> {
                int value = e.getAsInt();
                if (value <= 0) {
                    throw new IllegalArgumentException("Multipliers must be positive");
                }
                return value;
            });
    private static final Option<List<String>> PACKABLE_AE_DEVICE = listOption(
            "item.me_packing_tape_whitelist", "The AE device/part that can be packed by ME Packing Tape", List.of(
                    "extendedae:ex_interface_part",
                    "extendedae:ex_pattern_provider_part",
                    "extendedae:ex_interface",
                    "extendedae:ex_pattern_provider",
                    "extendedae:ex_drive",
                    "extendedae:oversize_interface",
                    "extendedae:oversize_interface_part",
                    "ae2:cable_interface",
                    "ae2:cable_pattern_provider",
                    "ae2:interface",
                    "ae2:pattern_provider",
                    "ae2:drive"
            ), JsonPrimitive::new, JsonElement::getAsString);
    private static final Option<Boolean> INSCRIBER_RENDER = boolOption(
            "client.disable_inscriber_item_render", "Disable Extended Inscriber's item render, it only works in client side", false);
    private static final Option<Integer> OVERSIZE_MULTIPLIER = intOption(
            "device.oversize_interface_multiplier", "Size multiplier of oversize interface", 16, 2, 4096);
    private static final Option<List<String>> CUSTOM_OVERSIZE_MULTIPLIER = listOption(
            "device.custom_oversize_interface_multiplier", "Set multiplier for specific AEKeyType in oversize interface",
            List.of("appbot:mana 2", "appflux:flux 4"), JsonPrimitive::new, JsonElement::getAsString);
    private static final Option<Boolean> CRYSTAL_INSCRIBER = boolOption(
            "device.enable_crystal_assembler_inscribe_processors", "Allow Crystal Assembler to do processor inscriber recipes", true);
    private static final Option<Integer> ASSEMBLER_MATRIX_SIZE = intOption(
            "device.assembler_matrix_max_size", "The max size of Assembler Matrix", 6, 3, 16);
    private static final Option<Boolean> DEBUG_MODE = boolOption(
            "misc.debug_mode", "Enable debug logging.", false);

    public static int busSpeed;
    public static double infCellCost;
    public static double wirelessMaxRange;
    public static double wirelessPowerMultiplier;
    public static List<Identifier> tapeWhitelist;
    public static boolean disableInscriberRender;
    private static int oversizeMultiplier;
    private static Map<Identifier, Integer> customOversizeMultiplier;
    private static List<? extends Integer> modifierMultiplier;
    public static boolean allowAssemblerCircuits;
    public static int assemblerMatrixSize;
    public static boolean debugMode;

    public static int getPatternModifierNumber(int index) {
        if (index >= modifierMultiplier.size()) {
            return defaultModifierMultiplier.getInt(index);
        } else {
            return modifierMultiplier.get(index);
        }
    }

    public static int getOversizeMultiplier(AEKeyType type) {
        return customOversizeMultiplier.getOrDefault(type.getId(), oversizeMultiplier);
    }

    /**
     * Loads the config file (creating it with the default values if it is missing) and updates the cached values.
     */
    public static void load() {
        var file = FabricLoader.getInstance().getConfigDir().resolve(ExtendedAE.MODID + ".json");
        read(file);
        write(file);
        onLoad();
    }

    private static void onLoad() {
        busSpeed = EX_BUS_SPEED.get();
        infCellCost = INFINITY_CELL_ENERGY.get();
        wirelessMaxRange = WIRELESS_CONNECTOR_RANGE.get();
        wirelessPowerMultiplier = WIRELESS_CONNECTOR_POWER_MULTIPLIER.get();
        tapeWhitelist = PACKABLE_AE_DEVICE.get().stream().map(Identifier::parse).collect(Collectors.toList());
        disableInscriberRender = INSCRIBER_RENDER.get();
        oversizeMultiplier = OVERSIZE_MULTIPLIER.get();
        customOversizeMultiplier = new Object2IntOpenHashMap<>();
        CUSTOM_OVERSIZE_MULTIPLIER.get().stream()
                .map(EAEConfig::parseOversizeMultiplier)
                .filter(Objects::nonNull)
                .forEach(p -> customOversizeMultiplier.put(p.getKey(), p.getValue()));
        modifierMultiplier = PATTERN_MODIFIER_NUMBER.get();
        allowAssemblerCircuits = CRYSTAL_INSCRIBER.get();
        assemblerMatrixSize = ASSEMBLER_MATRIX_SIZE.get();
        debugMode = DEBUG_MODE.get();
    }

    private static Pair<Identifier, Integer> parseOversizeMultiplier(String s) {
        try {
            var pair = s.split(" ");
            return Pair.of(Identifier.parse(pair[0]), Integer.decode(pair[1]));
        } catch (Throwable t) {
            return null;
        }
    }

    private static void read(Path file) {
        if (!Files.exists(file)) {
            return;
        }
        try {
            var reader = new JsonReader(new StringReader(Files.readString(file, StandardCharsets.UTF_8)));
            reader.setStrictness(Strictness.LENIENT);
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            for (var option : OPTIONS.values()) {
                var parts = option.name.split("\\.");
                JsonObject section = root;
                for (int i = 0; i < parts.length - 1 && section != null; i++) {
                    section = section.get(parts[i]) instanceof JsonObject obj ? obj : null;
                }
                var key = parts[parts.length - 1];
                if (section != null && section.has(key)) {
                    try {
                        option.read(section.get(key));
                    } catch (RuntimeException e) {
                        ExtendedAE.LOGGER.warn("Invalid value for {} in {}, using the default", option.name, file);
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            ExtendedAE.LOGGER.error("Failed to read config file {}, using defaults", file, e);
        }
    }

    private static void write(Path file) {
        var gson = new GsonBuilder().create();
        var sections = new LinkedHashMap<String, List<Option<?>>>();
        for (var option : OPTIONS.values()) {
            sections.computeIfAbsent(option.name.substring(0, option.name.indexOf('.')), k -> new ArrayList<>()).add(option);
        }
        var out = new StringBuilder("{\n");
        var sectionIt = sections.entrySet().iterator();
        while (sectionIt.hasNext()) {
            var section = sectionIt.next();
            out.append("  \"").append(section.getKey()).append("\": {\n");
            var it = section.getValue().iterator();
            while (it.hasNext()) {
                var option = it.next();
                out.append("    // ").append(option.comment).append('\n');
                out.append("    // Default: ").append(gson.toJson(option.writeDefault())).append('\n');
                out.append("    \"").append(option.name.substring(option.name.indexOf('.') + 1)).append("\": ")
                        .append(gson.toJson(option.writeValue()));
                out.append(it.hasNext() ? ",\n" : "\n");
            }
            out.append("  }").append(sectionIt.hasNext() ? ",\n" : "\n");
        }
        out.append("}\n");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            ExtendedAE.LOGGER.error("Failed to write config file {}", file, e);
        }
    }

    private static Option<Integer> intOption(String name, String comment, int def, int min, int max) {
        return add(new Option<>(name, comment, def, JsonPrimitive::new, e -> Math.clamp(e.getAsInt(), min, max)));
    }

    private static Option<Double> doubleOption(String name, String comment, double def, double min, double max) {
        return add(new Option<>(name, comment, def, JsonPrimitive::new, e -> Math.clamp(e.getAsDouble(), min, max)));
    }

    private static Option<Boolean> boolOption(String name, String comment, boolean def) {
        return add(new Option<>(name, comment, def, JsonPrimitive::new, JsonElement::getAsBoolean));
    }

    private static <T> Option<List<T>> listOption(String name, String comment, List<T> def,
                                                   Function<T, JsonElement> writer, Function<JsonElement, T> reader) {
        return add(new Option<>(name, comment, def, list -> {
            var array = new JsonArray();
            list.forEach(v -> array.add(writer.apply(v)));
            return array;
        }, e -> {
            var list = new ArrayList<T>();
            for (var element : e.getAsJsonArray()) {
                list.add(reader.apply(element));
            }
            return List.copyOf(list);
        }));
    }

    private static <T> Option<T> add(Option<T> option) {
        OPTIONS.put(option.name, option);
        return option;
    }

    private static final class Option<T> {
        private final String name;
        private final String comment;
        private final T defaultValue;
        private final Function<T, JsonElement> writer;
        private final Function<JsonElement, T> reader;
        private T value;

        private Option(String name, String comment, T defaultValue, Function<T, JsonElement> writer,
                       Function<JsonElement, T> reader) {
            this.name = name;
            this.comment = comment;
            this.defaultValue = defaultValue;
            this.value = defaultValue;
            this.writer = writer;
            this.reader = reader;
        }

        T get() {
            return value;
        }

        void read(JsonElement element) {
            this.value = Objects.requireNonNull(reader.apply(element));
        }

        JsonElement writeValue() {
            return writer.apply(value);
        }

        JsonElement writeDefault() {
            return writer.apply(defaultValue);
        }
    }

}
