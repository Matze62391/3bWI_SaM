package net.pedroksl.ae2addonlib.core.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import appeng.core.network.ServerboundPacket;

/**
 * Registers payload types with Fabric's networking API (replaces NeoForge's PayloadRegistrar).
 */
public class PayloadRegistrar {
    // Same limit as AE2 uses for its own clientbound packets
    private static final int MAX_CLIENTBOUND_PAYLOAD_SIZE = 8 * 1024 * 1024;

    public <T extends CustomPacketPayload> void playToClient(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        PayloadTypeRegistry.clientboundPlay().registerLarge(type, codec, MAX_CLIENTBOUND_PAYLOAD_SIZE);
    }

    public <T extends ServerboundPacket> void playToServer(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        PayloadTypeRegistry.serverboundPlay().register(type, codec);
        // Fabric invokes play payload handlers on the server thread
        ServerPlayNetworking.registerGlobalReceiver(type,
                (payload, context) -> payload.handleOnServer(context.player()));
    }

    public <T extends ServerboundPacket> void playBidirectional(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        playToClient(type, codec);
        playToServer(type, codec);
    }
}
