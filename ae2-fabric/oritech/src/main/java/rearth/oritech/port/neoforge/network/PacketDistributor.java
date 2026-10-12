package rearth.oritech.port.neoforge.network;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public final class PacketDistributor {
    private PacketDistributor() {}

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        send(player, payload);
        for (var other : payloads) {
            send(player, other);
        }
    }

    public static void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos chunkPos, CustomPacketPayload payload,
            CustomPacketPayload... payloads) {
        for (var player : PlayerLookup.tracking(level, chunkPos)) {
            sendToPlayer(player, payload, payloads);
        }
    }

    public static void sendToAllPlayers(net.minecraft.server.MinecraftServer server, CustomPacketPayload payload) {
        for (var player : PlayerLookup.all(server)) {
            send(player, payload);
        }
    }

    private static void send(ServerPlayer player, CustomPacketPayload payload) {
        if (ServerPlayNetworking.canSend(player, payload.type())) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
