package rearth.oritech.port.neoforge.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A small config spec that reads and writes a TOML file with sections ({@code [a.b]}) and scalar values. Values
 * keep their defaults until the file is loaded, invalid or out-of-range values are reset to the default.
 */
public final class ModConfigSpec {
    private static final Logger LOG = LoggerFactory.getLogger(ModConfigSpec.class);

    private final List<ConfigValue<?>> values;
    @Nullable
    private Path file;

    private ModConfigSpec(List<ConfigValue<?>> values) {
        this.values = List.copyOf(values);
    }

    public List<ConfigValue<?>> getValues() {
        return values;
    }

    public boolean isLoaded() {
        return file != null;
    }

    /** Loads the file (creating or completing it with defaults). */
    public void load(Path file) {
        this.file = file;
        Map<String, String> raw = new LinkedHashMap<>();
        if (Files.exists(file)) {
            try {
                String section = "";
                for (var line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    var trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }
                    if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                        section = trimmed.substring(1, trimmed.length() - 1).trim();
                        continue;
                    }
                    var eq = trimmed.indexOf('=');
                    if (eq < 0) {
                        continue;
                    }
                    var key = trimmed.substring(0, eq).trim();
                    var value = stripComment(trimmed.substring(eq + 1).trim());
                    raw.put(section.isEmpty() ? key : section + "." + key, value);
                }
            } catch (IOException e) {
                LOG.error("Failed to read config {}", file, e);
            }
        }

        boolean changed = !Files.exists(file);
        for (var value : values) {
            var text = raw.get(value.path());
            if (text == null) {
                changed = true;
                continue;
            }
            if (!value.parse(text)) {
                LOG.warn("Invalid value {} for {} in {}, using default", text, value.path(), file);
                changed = true;
            }
        }
        if (changed) {
            save();
        }
    }

    private static String stripComment(String value) {
        if (value.startsWith("\"")) {
            var end = value.indexOf('"', 1);
            return end > 0 ? value.substring(0, end + 1) : value;
        }
        var hash = value.indexOf('#');
        return hash >= 0 ? value.substring(0, hash).trim() : value;
    }

    public void save() {
        if (file == null) {
            return;
        }
        var out = new StringBuilder();
        String section = null;
        // group values by section, keeping their definition order
        Map<String, List<ConfigValue<?>>> sections = new LinkedHashMap<>();
        for (var value : values) {
            sections.computeIfAbsent(value.section(), s -> new ArrayList<>()).add(value);
        }
        for (var entry : sections.entrySet()) {
            if (!entry.getKey().equals(section)) {
                section = entry.getKey();
                if (!section.isEmpty()) {
                    out.append('\n').append('[').append(section).append("]\n");
                }
            }
            for (var value : entry.getValue()) {
                for (var comment : value.comments) {
                    out.append("\t# ").append(comment).append('\n');
                }
                if (value.rangeDescription() != null) {
                    out.append("\t# ").append(value.rangeDescription()).append('\n');
                }
                out.append('\t').append(value.name()).append(" = ").append(value.format()).append('\n');
            }
        }
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, out.toString().stripLeading(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("Failed to write config {}", file, e);
        }
    }

    public static class ConfigValue<T> implements Supplier<T> {
        private final List<String> path;
        private final List<String> comments;
        private final T defaultValue;
        private final Function<String, T> parser;
        private final Function<T, Boolean> validator;
        @Nullable
        private final String rangeDescription;
        private T value;

        ConfigValue(List<String> path, List<String> comments, T defaultValue, Function<String, T> parser,
                Function<T, Boolean> validator, @Nullable String rangeDescription) {
            this.path = List.copyOf(path);
            this.comments = List.copyOf(comments);
            this.defaultValue = defaultValue;
            this.parser = parser;
            this.validator = validator;
            this.rangeDescription = rangeDescription;
            this.value = defaultValue;
        }

        public List<String> getPath() {
            return path;
        }

        String path() {
            return String.join(".", path);
        }

        String section() {
            return String.join(".", path.subList(0, path.size() - 1));
        }

        String name() {
            return path.getLast();
        }

        @Nullable
        String rangeDescription() {
            return rangeDescription;
        }

        public List<String> getComments() {
            return comments;
        }

        boolean parse(String text) {
            try {
                var parsed = parser.apply(text);
                if (parsed != null && validator.apply(parsed)) {
                    value = parsed;
                    return true;
                }
            } catch (RuntimeException ignored) {
            }
            value = defaultValue;
            return false;
        }

        String format() {
            if (value instanceof String s) {
                return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
            }
            return String.valueOf(value);
        }

        @Override
        public T get() {
            return value;
        }

        public T getAsRaw() {
            return value;
        }

        public T getDefault() {
            return defaultValue;
        }

        public void set(T value) {
            if (validator.apply(value)) {
                this.value = value;
            }
        }

        public void save() {
        }
    }


    public static class BooleanValue extends ConfigValue<Boolean> {
        BooleanValue(List<String> path, List<String> comments, boolean defaultValue) {
            super(path, comments, defaultValue, text -> switch (text) {
                case "true" -> true;
                case "false" -> false;
                default -> null;
            }, v -> true, null);
        }

        public boolean getAsBoolean() {
            return get();
        }

        public boolean isTrue() {
            return get();
        }

        public boolean isFalse() {
            return !get();
        }
    }

    public static class IntValue extends ConfigValue<Integer> {
        IntValue(List<String> path, List<String> comments, int defaultValue, int min, int max) {
            super(path, comments, defaultValue, text -> (int) Double.parseDouble(text), v -> v >= min && v <= max,
                    "Range: " + rangeText(min, max, Integer.MIN_VALUE, Integer.MAX_VALUE));
        }

        public int getAsInt() {
            return get();
        }
    }

    public static class LongValue extends ConfigValue<Long> {
        LongValue(List<String> path, List<String> comments, long defaultValue, long min, long max) {
            super(path, comments, defaultValue, Long::parseLong, v -> v >= min && v <= max,
                    "Range: " + rangeText(min, max, Long.MIN_VALUE, Long.MAX_VALUE));
        }

        public long getAsLong() {
            return get();
        }
    }

    public static class DoubleValue extends ConfigValue<Double> {
        DoubleValue(List<String> path, List<String> comments, double defaultValue, double min, double max) {
            super(path, comments, defaultValue, Double::parseDouble, v -> v >= min && v <= max,
                    "Range: " + min + " ~ " + max);
        }

        public double getAsDouble() {
            return get();
        }
    }

    public static class EnumValue<T extends Enum<T>> extends ConfigValue<T> {
        EnumValue(List<String> path, List<String> comments, T defaultValue) {
            super(path, comments, defaultValue, text -> {
                var name = text.length() >= 2 && text.startsWith("\"") ? text.substring(1, text.length() - 1) : text;
                return Enum.valueOf(defaultValue.getDeclaringClass(), name);
            }, v -> true, "Allowed Values: " + String.join(", ",
                    java.util.Arrays.stream(defaultValue.getDeclaringClass().getEnumConstants()).map(Enum::name).toList()));
        }

        @Override
        String format() {
            return "\"" + get().name() + "\"";
        }
    }

    private static String rangeText(Number min, Number max, Number lowest, Number highest) {
        if (min.equals(lowest) && max.equals(highest)) {
            return "any";
        }
        if (max.equals(highest)) {
            return "> " + min;
        }
        if (min.equals(lowest)) {
            return "< " + max;
        }
        return min + " ~ " + max;
    }

    public static class Builder {
        private final Deque<String> currentPath = new ArrayDeque<>();
        private final List<ConfigValue<?>> values = new ArrayList<>();
        private final List<String> comments = new ArrayList<>();

        public Builder comment(String... comment) {
            for (var line : comment) {
                comments.addAll(List.of(line.split("\n")));
            }
            return this;
        }

        public Builder translation(String key) {
            return this;
        }

        public Builder worldRestart() {
            return this;
        }

        public Builder gameRestart() {
            return this;
        }

        public Builder push(String path) {
            for (var part : path.split("\\.")) {
                currentPath.addLast(part);
            }
            comments.clear();
            return this;
        }

        public Builder pop() {
            return pop(1);
        }

        public Builder pop(int count) {
            for (int i = 0; i < count; i++) {
                currentPath.removeLast();
            }
            return this;
        }

        private List<String> path(String name) {
            var path = new ArrayList<>(currentPath);
            path.addAll(List.of(name.split("\\.")));
            return path;
        }

        private <V extends ConfigValue<?>> V add(V value) {
            values.add(value);
            comments.clear();
            return value;
        }

        public BooleanValue define(String path, boolean defaultValue) {
            return add(new BooleanValue(path(path), comments, defaultValue));
        }

        public ConfigValue<String> define(String path, String defaultValue) {
            return add(new ConfigValue<>(path(path), comments, defaultValue, text -> {
                if (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
                    return text.substring(1, text.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
                }
                return text;
            }, v -> true, null));
        }

        public IntValue defineInRange(String path, int defaultValue, int min, int max) {
            return add(new IntValue(path(path), comments, defaultValue, min, max));
        }

        public LongValue defineInRange(String path, long defaultValue, long min, long max) {
            return add(new LongValue(path(path), comments, defaultValue, min, max));
        }

        public DoubleValue defineInRange(String path, double defaultValue, double min, double max) {
            return add(new DoubleValue(path(path), comments, defaultValue, min, max));
        }

        public <T extends Enum<T>> EnumValue<T> defineEnum(String path, T defaultValue) {
            return add(new EnumValue<>(path(path), comments, defaultValue));
        }

        public ModConfigSpec build() {
            return new ModConfigSpec(values);
        }
    }
}
