package rearth.oritech.port.neoforge.registries.datamaps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Loads all {@link DataMapType}s with the server's data and syncs them to clients.
 */
public final class DataMaps {
    private DataMaps() {}

    public static void init(String modId) {
        ServerLifecycleEvents.SERVER_STARTING.register(DataMaps::reload);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            reload(server);
            for (var player : server.getPlayerList().getPlayers()) {
                sync(player);
            }
        });
        PayloadTypeRegistry.clientboundPlay().register(SyncPayload.TYPE, SyncPayload.STREAM_CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));
    }

    private static void reload(MinecraftServer server) {
        for (var type : types()) {
            type.load(server.getResourceManager());
        }
    }

    private static List<DataMapType<?, ?>> types() {
        synchronized (DataMapType.ALL) {
            return new ArrayList<>(DataMapType.ALL);
        }
    }

    private static void sync(ServerPlayer player) {
        if (player.level().getServer().isSingleplayerOwner(player.nameAndId())) {
            return; // shares the static values with the integrated server
        }
        var data = new CompoundTag();
        for (var type : types()) {
            if (type.networkCodec() != null) {
                data.put(type.id().toString(), encode(type));
            }
        }
        if (ServerPlayNetworking.canSend(player, SyncPayload.TYPE)) {
            ServerPlayNetworking.send(player, new SyncPayload(data));
        }
    }

    private static <T> Tag encode(DataMapType<?, T> type) {
        Codec<Map<Identifier, T>> codec = Codec.unboundedMap(Identifier.CODEC, type.networkCodec());
        return codec.encodeStart(NbtOps.INSTANCE, type.values()).getOrThrow();
    }

    private static <T> void decode(DataMapType<?, T> type, Tag tag) {
        Codec<Map<Identifier, T>> codec = Codec.unboundedMap(Identifier.CODEC, type.networkCodec());
        codec.parse(NbtOps.INSTANCE, tag).result().ifPresent(values -> type.setValues(new HashMap<>(values)));
    }

    /** Called on the client when the sync payload arrives. */
    public static void receive(SyncPayload payload) {
        for (var type : types()) {
            var tag = payload.data().get(type.id().toString());
            if (tag != null && type.networkCodec() != null) {
                decode(type, tag);
            }
        }
    }

    public record SyncPayload(CompoundTag data) implements CustomPacketPayload {
        public static final Type<SyncPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath("oritech", "data_maps"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> STREAM_CODEC = ByteBufCodecs.COMPOUND_TAG
                .<RegistryFriendlyByteBuf>cast().map(SyncPayload::new, SyncPayload::data);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
