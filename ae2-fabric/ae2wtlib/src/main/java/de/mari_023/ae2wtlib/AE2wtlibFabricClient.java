package de.mari_023.ae2wtlib;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import de.mari_023.ae2wtlib.networking.AE2wtlibPacket;

/**
 * Client side of ae2wtlib, called by {@link AE2wtlibFabric} on the client.
 */
public final class AE2wtlibFabricClient {
    private AE2wtlibFabricClient() {
    }

    public static void init() {
        AE2wtlib.registerScreens();
        for (var type : AE2wtlibFabricClientPackets.TYPES) {
            registerReceiver(type);
        }
        ClientTickEvents.END_CLIENT_TICK.register(client -> AE2wtlibClient.clientTick());
        // The terminals also have to be known in the main menu (creative tab search, recipe viewers)
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> AE2wtlibFabric.finishTerminalRegistration());
    }

    private static <T extends AE2wtlibPacket> void registerReceiver(CustomPacketPayload.Type<T> type) {
        // Fabric runs play payload handlers on the client thread
        ClientPlayNetworking.registerGlobalReceiver(type, (packet, context) -> packet.processPacketData(context.player()));
    }
}
