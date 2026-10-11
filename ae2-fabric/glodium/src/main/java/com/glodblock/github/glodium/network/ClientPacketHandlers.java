package com.glodblock.github.glodium.network;

import com.glodblock.github.glodium.network.packet.IMessage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client-only part of {@link NetworkHandler}.
 */
final class ClientPacketHandlers {

    private ClientPacketHandlers() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static void register(CustomPacketPayload.Type type) {
        ClientPlayNetworking.registerGlobalReceiver(type,
                (payload, context) -> ((IMessage) payload).onMessage(context.player()));
    }

}
