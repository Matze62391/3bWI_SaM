package net.pedroksl.ae2addonlib.config;

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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.mojang.logging.LogUtils;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import net.fabricmc.loader.api.FabricLoader;

/**
 * A small replacement for NeoForge's ModConfigSpec. Values are grouped into sections and stored in a JSON file in the
 * config folder. Comments are written as {@code //} lines, which are skipped when the file is read.
 */
public class ModConfigSpec {
    private static final Logger LOG = LogUtils.getLogger();

    private final List<ConfigValue<?>> values;
    @Nullable
    private Path file;

    private ModConfigSpec(List<ConfigValue<?>> values) {
        this.values = List.copyOf(values);
    }

    public List<ConfigValue<?>> getValues() {
        return values;
    }

    /**
     * Loads the config from {@code config/<modId>-<type>.json} (creating the file with default values if it doesn't
     * exist) and remembers the file for {@link #save()}.
     */
    public void register(String modId, ModConfig.Type type) {
        this.file = FabricLoader.getInstance().getConfigDir().resolve(modId + "-" + type.extension() + ".json");
        load();
        save();
    }

    private void load() {
        if (file == null || !Files.exists(file)) {
            return;
        }
        try {
            var reader = new JsonReader(new StringReader(Files.readString(file, StandardCharsets.UTF_8)));
            reader.setStrictness(Strictness.LENIENT);
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            for (var value : values) {
                var section = root;
                for (var part : value.path) {
                    section = section != null && section.get(part) instanceof JsonObject obj ? obj : null;
                }
                if (section != null && section.has(value.name)) {
                    try {
                        value.read(section.get(value.name));
                    } catch (RuntimeException e) {
                        LOG.warn("Invalid value for {} in {}, using the default", value.name, file, e);
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            LOG.error("Failed to read config file {}, using defaults", file, e);
        }
    }

    public void save() {
        if (file == null) {
            return;
        }
        var out = new StringBuilder("{\n");
        writeSection(out, buildTree(), 1);
        out.append("}\n");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("Failed to write config file {}", file, e);
        }
    }

    private Map<String, Object> buildTree() {
        var root = new LinkedHashMap<String, Object>();
        for (var value : values) {
            Map<String, Object> section = root;
            for (var part : value.path) {
                @SuppressWarnings("unchecked")
                var child = (Map<String, Object>) section.computeIfAbsent(part, k -> new LinkedHashMap<String, Object>());
                section = child;
            }
            section.put(value.name, value);
        }
        return root;
    }

    private static void writeSection(StringBuilder out, Map<String, Object> section, int depth) {
        var indent = "  ".repeat(depth);
        var it = section.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (entry.getValue() instanceof ConfigValue<?> value) {
                for (var line : value.describe()) {
                    out.append(indent).append("// ").append(line).append('\n');
                }
                out.append(indent).append(quote(entry.getKey())).append(": ").append(value.write());
            } else {
                @SuppressWarnings("unchecked")
                var child = (Map<String, Object>) entry.getValue();
                out.append(indent).append(quote(entry.getKey())).append(": {\n");
                writeSection(out, child, depth + 1);
                out.append(indent).append('}');
            }
            out.append(it.hasNext() ? ",\n" : "\n");
        }
    }

    private static String quote(String s) {
        return new com.google.gson.JsonPrimitive(s).toString();
    }

    public static class Builder {
        private final List<ConfigValue<?>> values = new ArrayList<>();
        private final List<String> path = new ArrayList<>();
        @Nullable
        private String comment;

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder push(String section) {
            path.add(section);
            return this;
        }

        public Builder pop() {
            path.removeLast();
            return this;
        }

        public BooleanValue define(String name, boolean defaultValue) {
            return add(new BooleanValue(name, defaultValue));
        }

        public IntValue defineInRange(String name, int defaultValue, int min, int max) {
            return add(new IntValue(name, defaultValue, min, max));
        }

        public DoubleValue defineInRange(String name, double defaultValue, double min, double max) {
            return add(new DoubleValue(name, defaultValue, min, max));
        }

        public <T extends Enum<T>> EnumValue<T> defineEnum(String name, T defaultValue) {
            return add(new EnumValue<>(name, defaultValue));
        }

        private <V extends ConfigValue<?>> V add(V value) {
            ConfigValue<?> configValue = value;
            configValue.path = List.copyOf(path);
            configValue.comment = comment;
            comment = null;
            values.add(value);
            return value;
        }

        public ModConfigSpec build() {
            return new ModConfigSpec(values);
        }
    }

    public abstract static class ConfigValue<T> {
        private final String name;
        private final T defaultValue;
        private T value;
        private List<String> path = List.of();
        @Nullable
        private String comment;

        protected ConfigValue(String name, T defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
            this.value = defaultValue;
        }

        public String getName() {
            return name;
        }

        public List<String> getPath() {
            return path;
        }

        @Nullable
        public String getComment() {
            return comment;
        }

        public T get() {
            return value;
        }

        public T getDefault() {
            return defaultValue;
        }

        public void set(T value) {
            this.value = Objects.requireNonNull(validate(value));
        }

        protected T validate(T value) {
            return value;
        }

        protected abstract void read(JsonElement element);

        protected String write() {
            return String.valueOf(value);
        }

        protected List<String> describe() {
            var lines = new ArrayList<String>();
            if (comment != null) {
                lines.addAll(List.of(comment.split("\n")));
            }
            lines.add("Default: " + defaultValue);
            return lines;
        }
    }

    public static class BooleanValue extends ConfigValue<Boolean> {
        BooleanValue(String name, boolean defaultValue) {
            super(name, defaultValue);
        }

        public boolean getAsBoolean() {
            return get();
        }

        @Override
        protected void read(JsonElement element) {
            set(element.getAsBoolean());
        }
    }

    public static class IntValue extends ConfigValue<Integer> {
        private final int min;
        private final int max;

        IntValue(String name, int defaultValue, int min, int max) {
            super(name, defaultValue);
            this.min = min;
            this.max = max;
        }

        public int getAsInt() {
            return get();
        }

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        @Override
        protected Integer validate(Integer value) {
            return Math.clamp(value, min, max);
        }

        @Override
        protected void read(JsonElement element) {
            set(element.getAsInt());
        }

        @Override
        protected List<String> describe() {
            var lines = super.describe();
            lines.add("Range: " + min + " ~ " + max);
            return lines;
        }
    }

    public static class DoubleValue extends ConfigValue<Double> {
        private final double min;
        private final double max;

        DoubleValue(String name, double defaultValue, double min, double max) {
            super(name, defaultValue);
            this.min = min;
            this.max = max;
        }

        public double getAsDouble() {
            return get();
        }

        @Override
        protected Double validate(Double value) {
            return Math.clamp(value, min, max);
        }

        @Override
        protected void read(JsonElement element) {
            set(element.getAsDouble());
        }
    }

    public static class EnumValue<T extends Enum<T>> extends ConfigValue<T> {
        EnumValue(String name, T defaultValue) {
            super(name, defaultValue);
        }

        @Override
        protected void read(JsonElement element) {
            set(Enum.valueOf(getDefault().getDeclaringClass(), element.getAsString()));
        }

        @Override
        protected String write() {
            return quote(get().name());
        }

        @Override
        protected List<String> describe() {
            var lines = super.describe();
            lines.add("Allowed values: " + String.join(", ",
                    java.util.Arrays.stream(getDefault().getDeclaringClass().getEnumConstants()).map(Enum::name)
                            .toList()));
            return lines;
        }
    }
}
