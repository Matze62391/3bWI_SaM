package de.mari_023.ae2wtlib;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import de.mari_023.ae2wtlib.networking.AE2wtlibPacket;

/**
 * The clientbound packets, whose handlers the client registers.
 */
final class AE2wtlibFabricClientPackets {
    static final List<CustomPacketPayload.Type<? extends AE2wtlibPacket>> TYPES = new ArrayList<>();

    private AE2wtlibFabricClientPackets() {
    }

    static void add(CustomPacketPayload.Type<? extends AE2wtlibPacket> type) {
        TYPES.add(type);
    }
}
