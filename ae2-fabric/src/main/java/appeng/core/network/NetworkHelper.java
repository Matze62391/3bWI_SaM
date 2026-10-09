package appeng.core.network;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Replaces NeoForge's PacketDistributor / ClientPacketDistributor.
 */
public final class NetworkHelper {
    private NetworkHelper() {
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendToPlayersNear(ServerLevel level, @Nullable ServerPlayer except, double x, double y,
            double z, double radius, CustomPacketPayload payload) {
        for (var player : PlayerLookup.level(level)) {
            if (player != except && player.distanceToSqr(x, y, z) < radius * radius) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    /**
     * Must only be called on the client.
     */
    public static void sendToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }
}
