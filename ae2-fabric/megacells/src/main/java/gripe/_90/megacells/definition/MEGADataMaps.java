package gripe._90.megacells.definition;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import gripe._90.megacells.MEGACells;
import gripe._90.megacells.misc.CompressionOverride;

/**
 * NeoForge data maps don't exist on Fabric. The compression overrides are read from the same JSON files
 * ({@code data/<namespace>/data_maps/item/compression_overrides.json}) whenever the compression chains are rebuilt.
 */
public final class MEGADataMaps {
    private static final Logger LOG = LoggerFactory.getLogger(MEGADataMaps.class);
    private static final Identifier FILE = MEGACells.makeId("data_maps/item/compression_overrides.json");

    private static final Map<Item, Item> COMPRESSION_OVERRIDES = new HashMap<>();

    private MEGADataMaps() {}

    /**
     * @return the overridden compression variant of the item, {@link net.minecraft.world.item.Items#AIR} if it should
     *     never be compressed, or null if there is no override.
     */
    @Nullable
    public static Item getCompressionOverride(Item item) {
        return COMPRESSION_OVERRIDES.get(item);
    }

    public static void load(ResourceManager resourceManager) {
        COMPRESSION_OVERRIDES.clear();
        var path = FILE.getPath();

        for (var namespace : resourceManager.getNamespaces()) {
            var id = Identifier.fromNamespaceAndPath(namespace, path);
            for (var resource : resourceManager.getResourceStack(id)) {
                try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                    apply(JsonParser.parseReader(reader).getAsJsonObject());
                } catch (Exception e) {
                    LOG.error("Failed to read compression overrides from {}", id, e);
                }
            }
        }
    }

    private static void apply(JsonObject json) {
        if (json.has("replace") && json.get("replace").getAsBoolean()) {
            COMPRESSION_OVERRIDES.clear();
        }

        if (json.has("values")) {
            for (var entry : json.getAsJsonObject("values").entrySet()) {
                var value = entry.getValue().getAsJsonObject();
                if (!conditionsMet(value)) {
                    continue;
                }

                var variant = CompressionOverride.CODEC
                        .parse(JsonOps.INSTANCE, value)
                        .resultOrPartial(error -> LOG.warn("Invalid compression override {}: {}", entry.getKey(), error));
                if (variant.isEmpty()) {
                    continue;
                }

                forEachItem(entry.getKey(), item -> COMPRESSION_OVERRIDES.put(item, variant.get()));
            }
        }

        if (json.has("remove")) {
            for (JsonElement removed : json.getAsJsonArray("remove")) {
                forEachItem(removed.getAsString(), COMPRESSION_OVERRIDES::remove);
            }
        }
    }

    private static boolean conditionsMet(JsonObject value) {
        if (!value.has("neoforge:conditions")) {
            return true;
        }

        for (var condition : value.getAsJsonArray("neoforge:conditions")) {
            var object = condition.getAsJsonObject();
            var type = object.get("type").getAsString();
            if (type.equals("neoforge:mod_loaded")
                    && !FabricLoader.getInstance().isModLoaded(object.get("modid").getAsString())) {
                return false;
            }
        }

        return true;
    }

    private static void forEachItem(String key, java.util.function.Consumer<Item> action) {
        if (key.startsWith("#")) {
            var tag = TagKey.create(Registries.ITEM, Identifier.parse(key.substring(1)));
            BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(holder -> action.accept(holder.value()));
        } else {
            BuiltInRegistries.ITEM.getOptional(Identifier.parse(key)).ifPresent(action);
        }
    }
}
