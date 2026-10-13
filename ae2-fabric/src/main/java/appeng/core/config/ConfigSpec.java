package appeng.core.config;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A small configuration system that replaces NeoForge's ModConfigSpec. Values are declared through a {@link Builder}
 * in sections and are stored in a commented JSON file (comments use <code>//</code>, which Gson reads in lenient
 * mode).
 */
public final class ConfigSpec {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigSpec.class);

    private final List<ConfigValue<?>> values;
    private final List<String> sectionComments;
    @Nullable
    private Path file;

    private ConfigSpec(List<ConfigValue<?>> values, List<String> sectionComments) {
        this.values = values;
        this.sectionComments = sectionComments;
    }

    /**
     * Loads this config from the given file. Missing or invalid values are reset to their default, and the file is
     * re-written so that it contains all current options.
     */
    public void load(Path file) {
        this.file = file;
        if (Files.exists(file)) {
            try {
                var reader = new JsonReader(new StringReader(Files.readString(file, StandardCharsets.UTF_8)));
                reader.setStrictness(com.google.gson.Strictness.LENIENT);
                var root = JsonParser.parseReader(reader);
                if (root.isJsonObject()) {
                    for (var value : values) {
                        value.read(root.getAsJsonObject());
                    }
                }
            } catch (Exception e) {
                LOG.error("Failed to read config file {}, using defaults", file, e);
            }
        }
        save();
    }

    /**
     * @return All values of this config, in the order they were defined.
     */
    public List<ConfigValue<?>> getValues() {
        return values;
    }

    public void save() {
        if (file == null) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, write(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("Failed to write config file {}", file, e);
        }
    }

    private String write() {
        var sb = new StringBuilder();
        sb.append("{\n");
        List<String> openSections = new ArrayList<>();
        boolean first = true;
        for (var value : values) {
            // Close sections that are no longer part of the path, then open new ones
            int common = 0;
            while (common < openSections.size() && common < value.section.size()
                    && openSections.get(common).equals(value.section.get(common))) {
                common++;
            }
            while (openSections.size() > common) {
                openSections.removeLast();
                sb.append("\n").append(indent(openSections.size() + 1)).append("}");
                first = false;
            }
            while (openSections.size() < value.section.size()) {
                var name = value.section.get(openSections.size());
                if (!first) {
                    sb.append(",");
                }
                sb.append("\n");
                var sectionPath = String.join(".", value.section.subList(0, openSections.size() + 1));
                for (var comment : sectionComments) {
                    if (comment.startsWith(sectionPath + "=")) {
                        appendComment(sb, comment.substring(sectionPath.length() + 1), openSections.size() + 1);
                    }
                }
                sb.append(indent(openSections.size() + 1)).append(quote(name)).append(": {");
                openSections.add(name);
                first = true;
            }
            if (!first) {
                sb.append(",");
            }
            sb.append("\n");
            var comment = value.describe();
            if (!comment.isEmpty()) {
                appendComment(sb, comment, openSections.size() + 1);
            }
            sb.append(indent(openSections.size() + 1)).append(quote(value.name)).append(": ")
                    .append(value.toJson());
            first = false;
        }
        while (!openSections.isEmpty()) {
            openSections.removeLast();
            sb.append("\n").append(indent(openSections.size() + 1)).append("}");
        }
        sb.append("\n}\n");
        return sb.toString();
    }

    private static void appendComment(StringBuilder sb, String comment, int depth) {
        for (var line : comment.split("\n")) {
            sb.append(indent(depth)).append("// ").append(line.strip()).append("\n");
        }
    }

    private static String indent(int depth) {
        return "  ".repeat(depth);
    }

    private static String quote(String s) {
        return new JsonPrimitive(s).toString();
    }

    public static final class Builder {
        private final List<String> path = new ArrayList<>();
        private final List<ConfigValue<?>> values = new ArrayList<>();
        private final List<String> sectionComments = new ArrayList<>();
        @Nullable
        private String pendingComment;

        public Builder comment(String comment) {
            pendingComment = pendingComment == null ? comment : pendingComment + "\n" + comment;
            return this;
        }

        public Builder push(String section) {
            path.add(section);
            if (pendingComment != null) {
                sectionComments.add(String.join(".", path) + "=" + pendingComment);
                pendingComment = null;
            }
            return this;
        }

        public Builder pop() {
            path.removeLast();
            return this;
        }

        public BooleanValue define(String name, boolean defaultValue) {
            return add(new BooleanValue(this, name, defaultValue));
        }

        public IntValue defineInRange(String name, int defaultValue, int min, int max) {
            return add(new IntValue(this, name, defaultValue, min, max));
        }

        public DoubleValue defineInRange(String name, double defaultValue, double min, double max) {
            return add(new DoubleValue(this, name, defaultValue, min, max));
        }

        public <T extends Enum<T>> EnumValue<T> defineEnum(String name, T defaultValue) {
            return add(new EnumValue<>(this, name, defaultValue));
        }

        private <V extends ConfigValue<?>> V add(V value) {
            values.add(value);
            return value;
        }

        public ConfigSpec build() {
            return new ConfigSpec(List.copyOf(values), List.copyOf(sectionComments));
        }
    }

    public abstract static sealed class ConfigValue<T> implements Supplier<T>
            permits BooleanValue, IntValue, DoubleValue, EnumValue {
        private final List<String> section;
        private final String name;
        private final String comment;
        private final T defaultValue;
        private T value;

        protected ConfigValue(Builder builder, String name, T defaultValue) {
            this.section = List.copyOf(builder.path);
            this.name = name;
            this.comment = builder.pendingComment != null ? builder.pendingComment : "";
            builder.pendingComment = null;
            this.defaultValue = defaultValue;
            this.value = defaultValue;
        }

        @Override
        public T get() {
            return value;
        }

        public T getDefault() {
            return defaultValue;
        }

        public void set(T value) {
            this.value = validate(value) ? value : defaultValue;
        }

        public String getName() {
            return name;
        }

        public List<String> getSection() {
            return section;
        }

        /**
         * @return The comment of this option, including the allowed range or values.
         */
        public String getDescription() {
            return describe();
        }

        public List<String> getPath() {
            var result = new ArrayList<>(section);
            result.add(name);
            return result;
        }

        protected boolean validate(T value) {
            return value != null;
        }

        protected String describe() {
            return comment;
        }

        protected abstract JsonElement toJson();

        @Nullable
        protected abstract T fromJson(JsonElement element);

        void read(JsonObject root) {
            JsonObject current = root;
            for (var part : section) {
                var child = current.get(part);
                if (child == null || !child.isJsonObject()) {
                    return;
                }
                current = child.getAsJsonObject();
            }
            var element = current.get(name);
            if (element == null) {
                return;
            }
            try {
                T parsed = fromJson(element);
                if (parsed != null && validate(parsed)) {
                    value = parsed;
                } else {
                    LOG.warn("Invalid value {} for config option {}, using default", element, getPath());
                }
            } catch (RuntimeException e) {
                LOG.warn("Invalid value {} for config option {}, using default", element, getPath());
            }
        }
    }

    public static final class BooleanValue extends ConfigValue<Boolean> implements BooleanSupplier {
        BooleanValue(Builder builder, String name, boolean defaultValue) {
            super(builder, name, defaultValue);
        }

        @Override
        public boolean getAsBoolean() {
            return get();
        }

        @Override
        protected JsonElement toJson() {
            return new JsonPrimitive(get());
        }

        @Override
        protected Boolean fromJson(JsonElement element) {
            return element.getAsBoolean();
        }
    }

    public static final class IntValue extends ConfigValue<Integer> implements IntSupplier {
        private final int min;
        private final int max;

        IntValue(Builder builder, String name, int defaultValue, int min, int max) {
            super(builder, name, defaultValue);
            this.min = min;
            this.max = max;
        }

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        @Override
        public int getAsInt() {
            return get();
        }

        @Override
        protected boolean validate(Integer value) {
            return value != null && value >= min && value <= max;
        }

        @Override
        protected String describe() {
            var range = min == Integer.MIN_VALUE && max == Integer.MAX_VALUE ? ""
                    : "Range: " + min + " ~ " + max;
            return join(super.describe(), range);
        }

        @Override
        protected JsonElement toJson() {
            return new JsonPrimitive(get());
        }

        @Override
        protected Integer fromJson(JsonElement element) {
            return element.getAsInt();
        }
    }

    public static final class DoubleValue extends ConfigValue<Double> implements DoubleSupplier {
        private final double min;
        private final double max;

        DoubleValue(Builder builder, String name, double defaultValue, double min, double max) {
            super(builder, name, defaultValue);
            this.min = min;
            this.max = max;
        }

        public double getMin() {
            return min;
        }

        public double getMax() {
            return max;
        }

        @Override
        public double getAsDouble() {
            return get();
        }

        @Override
        protected boolean validate(Double value) {
            return value != null && value >= min && value <= max;
        }

        @Override
        protected String describe() {
            var range = min == Double.MIN_VALUE && max == Double.MAX_VALUE ? ""
                    : "Range: " + min + " ~ " + max;
            return join(super.describe(), range);
        }

        @Override
        protected JsonElement toJson() {
            return new JsonPrimitive(get());
        }

        @Override
        protected Double fromJson(JsonElement element) {
            return element.getAsDouble();
        }
    }

    public static final class EnumValue<T extends Enum<T>> extends ConfigValue<T> {
        private final Class<T> enumClass;

        public Class<T> getEnumClass() {
            return enumClass;
        }

        EnumValue(Builder builder, String name, T defaultValue) {
            super(builder, name, defaultValue);
            this.enumClass = defaultValue.getDeclaringClass();
        }

        @Override
        protected String describe() {
            var allowed = new StringBuilder("Allowed values: ");
            var constants = enumClass.getEnumConstants();
            for (int i = 0; i < constants.length; i++) {
                if (i > 0) {
                    allowed.append(", ");
                }
                allowed.append(constants[i].name());
            }
            return join(super.describe(), allowed.toString());
        }

        @Override
        protected JsonElement toJson() {
            return new JsonPrimitive(get().name());
        }

        @Override
        protected T fromJson(JsonElement element) {
            var name = element.getAsString();
            for (var constant : enumClass.getEnumConstants()) {
                if (constant.name().equalsIgnoreCase(name)) {
                    return constant;
                }
            }
            return null;
        }
    }

    private static String join(String a, String b) {
        if (a.isEmpty()) {
            return b;
        }
        if (b.isEmpty()) {
            return a;
        }
        return a + "\n" + b;
    }
}
