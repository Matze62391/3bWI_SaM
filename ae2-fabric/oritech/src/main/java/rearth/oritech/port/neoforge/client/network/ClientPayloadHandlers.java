package rearth.oritech.port.neoforge.client.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import rearth.oritech.port.neoforge.network.registration.PayloadRegistrar;

public final class ClientPayloadHandlers {
    private ClientPayloadHandlers() {}

    public static void register() {
        for (var handler : PayloadRegistrar.clientHandlers()) {
            register(handler);
        }
    }

    private static <T extends CustomPacketPayload> void register(PayloadRegistrar.ClientHandler<T> handler) {
        ClientPlayNetworking.registerGlobalReceiver(handler.type(),
                (payload, context) -> handler.handler().handle(payload, context::player));
    }
}
