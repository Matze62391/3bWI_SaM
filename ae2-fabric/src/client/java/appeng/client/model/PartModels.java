package appeng.client.model;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StrictJsonParser;

import appeng.api.implementations.parts.ICablePart;
import appeng.api.parts.IPartItem;
import appeng.client.api.model.parts.ClientPart;
import appeng.client.api.model.parts.PartModel;
import appeng.client.api.model.parts.RegisterPartModelsEvent;

public final class PartModels {
    private static final Logger LOG = LoggerFactory.getLogger(PartModels.class);

    private static final ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends PartModel.Unbaked>> PART_MODEL_IDS = new ExtraCodecs.LateBoundIdMapper<>();
    private static final MapCodec<PartModel.Unbaked> MAP_CODEC = PART_MODEL_IDS.codec(Identifier.CODEC)
            .dispatchMap(PartModel.Unbaked::codec, c -> c);
    public static final Codec<PartModel.Unbaked> CODEC = MAP_CODEC.codec();

    private Map<Identifier, ClientPart> clientParts = null;

    public PartModels() {
        RegisterPartModelsEvent.EVENT.invoker().registerPartModels(new RegisterPartModelsEvent(PART_MODEL_IDS));
    }

    public CompletableFuture<Void> reload(ResourceManager resourceManager, Executor executor) {
        var fileToIdConverter = FileToIdConverter.json("ae2/parts");
        return CompletableFuture.supplyAsync(() -> {
            var clientParts = new HashMap<Identifier, ClientPart>();
            for (var entry : fileToIdConverter.listMatchingResources(resourceManager).entrySet()) {
                var location = entry.getKey();
                var id = fileToIdConverter.fileToId(location);
                try (var reader = entry.getValue().openAsReader()) {
                    ClientPart.CODEC.parse(JsonOps.INSTANCE, StrictJsonParser.parse(reader))
                            .ifSuccess(parsed -> clientParts.putIfAbsent(id, parsed))
                            .ifError(error -> LOG.error("Couldn't parse part model '{}' from '{}': {}", id,
                                    location, error));
                } catch (Exception e) {
                    LOG.error("Couldn't parse part model '{}' from '{}'", id, location, e);
                }
            }
            return clientParts;
        }, executor)
                .thenAccept(clientParts -> {
                    this.clientParts = clientParts;

                    for (var entry : BuiltInRegistries.ITEM.entrySet()) {
                        var item = entry.getValue();
                        if (item instanceof IPartItem<?> partItem) {
                            // Skip cables, those are special
                            if (ICablePart.class.isAssignableFrom(partItem.getPartClass())) {
                                continue;
                            }

                            var itemId = entry.getKey().identifier();
                            var modelId = fileToIdConverter.idToFile(itemId);
                            if (!this.clientParts.containsKey(itemId)) {
                                LOG.warn("No part model loaded for part item ID {}. Expected at {}", itemId, modelId);
                            }
                        }
                    }
                });
    }

    @Nullable
    public PartModel.Unbaked getPartModel(Identifier id) {
        var clientPart = clientParts.get(id);
        return clientPart != null ? clientPart.model() : null;
    }

    public Map<IPartItem<?>, PartModel.Unbaked> getUnbaked() {
        var result = new IdentityHashMap<IPartItem<?>, PartModel.Unbaked>(this.clientParts.size());

        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            if (entry.getValue() instanceof IPartItem<?> partItem) {
                var model = getPartModel(entry.getKey().identifier());
                if (model != null) {
                    result.put(partItem, model);
                }
            }
        }

        return result;
    }

    public void resolveDependencies(ResolvableModel.Resolver resolver) {
        if (clientParts == null) {
            throw new IllegalStateException("Part models have not been initialized yet.");
        }
        for (var clientPart : clientParts.values()) {
            clientPart.model().resolveDependencies(resolver);
        }
    }
}
