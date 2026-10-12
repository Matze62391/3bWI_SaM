package rearth.oritech.port.neoforge.registries.datamaps;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * A per-registry-entry value loaded from {@code data/<namespace>/data_maps/<registry>/<path>.json}. Fabric has no
 * data maps, so the files are read by {@link DataMaps} whenever server data is (re)loaded and sent to clients when
 * the type is synced.
 */
public final class DataMapType<R, T> {
    private static final Logger LOG = LoggerFactory.getLogger(DataMapType.class);
    static final List<DataMapType<?, ?>> ALL = new ArrayList<>();

    private final Identifier id;
    private final ResourceKey<? extends Registry<R>> registryKey;
    private final Codec<T> codec;
    @Nullable
    private final Codec<T> networkCodec;
    private volatile Map<Identifier, T> values = Map.of();

    private DataMapType(Identifier id, ResourceKey<? extends Registry<R>> registryKey, Codec<T> codec,
            @Nullable Codec<T> networkCodec) {
        this.id = id;
        this.registryKey = registryKey;
        this.codec = codec;
        this.networkCodec = networkCodec;
        synchronized (ALL) {
            ALL.add(this);
        }
    }

    public static <R, T> Builder<R, T> builder(Identifier id, ResourceKey<? extends Registry<R>> registry,
            Codec<T> codec) {
        return new Builder<>(id, registry, codec);
    }

    public Identifier id() {
        return id;
    }

    public ResourceKey<? extends Registry<R>> registryKey() {
        return registryKey;
    }

    public Codec<T> codec() {
        return codec;
    }

    @Nullable
    Codec<T> networkCodec() {
        return networkCodec;
    }

    @Nullable
    public T get(Holder<R> holder) {
        return holder.unwrapKey().map(key -> values.get(key.identifier())).orElse(null);
    }

    @Nullable
    public T get(Identifier entry) {
        return values.get(entry);
    }

    public Map<Identifier, T> values() {
        return values;
    }

    void setValues(Map<Identifier, T> values) {
        this.values = Collections.unmodifiableMap(new HashMap<>(values));
    }

    void load(ResourceManager resourceManager) {
        var registryPath = registryKey.identifier().getPath();
        var path = "data_maps/" + registryPath + "/" + id.getPath() + ".json";
        var file = Identifier.fromNamespaceAndPath(id.getNamespace(), path);
        var result = new HashMap<Identifier, T>();

        for (var resource : resourceManager.getResourceStack(file)) {
            try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                var json = JsonParser.parseReader(reader).getAsJsonObject();
                if (json.has("replace") && json.get("replace").getAsBoolean()) {
                    result.clear();
                }
                if (json.has("values")) {
                    for (var entry : json.getAsJsonObject("values").entrySet()) {
                        if (entry.getKey().startsWith("#")) {
                            LOG.warn("Tag entries are not supported in data map {}: {}", id, entry.getKey());
                            continue;
                        }
                        var key = Identifier.parse(entry.getKey());
                        codec.parse(JsonOps.INSTANCE, entry.getValue())
                                .resultOrPartial(error -> LOG.warn("Invalid {} entry {}: {}", id, key, error))
                                .ifPresent(value -> result.put(key, value));
                    }
                }
                if (json.has("remove")) {
                    for (var removed : json.getAsJsonArray("remove")) {
                        result.remove(Identifier.parse(removed.getAsString()));
                    }
                }
            } catch (Exception e) {
                LOG.error("Failed to read data map {} from {}", id, resource.sourcePackId(), e);
            }
        }

        setValues(result);
    }

    public static final class Builder<R, T> {
        private final Identifier id;
        private final ResourceKey<? extends Registry<R>> registryKey;
        private final Codec<T> codec;
        @Nullable
        private Codec<T> networkCodec;

        private Builder(Identifier id, ResourceKey<? extends Registry<R>> registryKey, Codec<T> codec) {
            this.id = id;
            this.registryKey = registryKey;
            this.codec = codec;
        }

        public Builder<R, T> synced(Codec<T> networkCodec, boolean mandatory) {
            this.networkCodec = networkCodec;
            return this;
        }

        public DataMapType<R, T> build() {
            return new DataMapType<>(id, registryKey, codec, networkCodec);
        }
    }
}
