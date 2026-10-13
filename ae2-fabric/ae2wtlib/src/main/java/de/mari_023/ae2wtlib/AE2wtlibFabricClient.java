package de.mari_023.ae2wtlib;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import de.mari_023.ae2wtlib.networking.AE2wtlibPacket;

/**
 * Client side of ae2wtlib. Runs after all mods are initialized, so it also registers the terminals.
 */
public final class AE2wtlibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AE2wtlibFabric.finishTerminalRegistration();
        AE2wtlib.registerScreens();
        for (var type : AE2wtlibFabricClientPackets.TYPES) {
            registerReceiver(type);
        }
        ClientTickEvents.END_CLIENT_TICK.register(client -> AE2wtlibClient.clientTick());
    }

    private static <T extends AE2wtlibPacket> void registerReceiver(CustomPacketPayload.Type<T> type) {
        // Fabric runs play payload handlers on the client thread
        ClientPlayNetworking.registerGlobalReceiver(type, (packet, context) -> packet.processPacketData(context.player()));
    }
}
