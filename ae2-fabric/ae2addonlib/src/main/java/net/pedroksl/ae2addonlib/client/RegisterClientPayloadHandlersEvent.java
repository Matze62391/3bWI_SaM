package net.pedroksl.ae2addonlib.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import appeng.core.network.ClientboundPacket;

/**
 * Registers handlers for clientbound packets with Fabric's networking API (replaces NeoForge's event of the same
 * name).
 */
public class RegisterClientPayloadHandlersEvent {
    public <T extends ClientboundPacket> void register(CustomPacketPayload.Type<T> type,
            ClientNetworkHandler.ClientPacketHandler<T> handler) {
        // Fabric invokes the handler on the render thread
        ClientPlayNetworking.registerGlobalReceiver(type,
                (payload, context) -> handler.handle(payload, context.client(), context.player()));
    }
}
