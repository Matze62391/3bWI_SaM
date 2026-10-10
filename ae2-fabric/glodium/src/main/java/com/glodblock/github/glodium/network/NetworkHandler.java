package com.glodblock.github.glodium.network;

import com.glodblock.github.glodium.Glodium;
import com.glodblock.github.glodium.network.packet.IMessage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Registers a mod's packets with Fabric's networking API. The mod calls {@link #register()} during its common
 * initialization and {@link #registerClient()} during its client initialization (NeoForge does both in one event).
 */
public class NetworkHandler {

    /**
     * Fabric limits payloads to 1 MiB unless they are registered as "large".
     */
    private static final int MAX_CLIENTBOUND_PAYLOAD_SIZE = 8 * 1024 * 1024;

    protected final List<Supplier<IMessage>> LAZY_INIT = new ArrayList<>();
    protected final String modid;
    private final List<Packet> packets = new ArrayList<>();

    public NetworkHandler(String modid) {
        this.modid = modid;
    }

    protected void registerPacket(Supplier<IMessage> factory) {
        if (factory == null) {
            throw new IllegalArgumentException("Packet Constructor is null");
        }
        this.LAZY_INIT.add(factory);
    }

    /**
     * Registers the packet types and the handlers of serverbound packets.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void register() {
        this.initPackets();
        for (var factory : this.LAZY_INIT) {
            var instance = factory.get();
            var type = (CustomPacketPayload.Type) instance.type();
            var codec = (StreamCodec) this.codec(factory);
            if (instance.isClient()) {
                PayloadTypeRegistry.clientboundPlay().registerLarge(type, codec, MAX_CLIENTBOUND_PAYLOAD_SIZE);
            } else {
                PayloadTypeRegistry.serverboundPlay().register(type, codec);
                // Fabric runs play payload handlers on the server thread
                ServerPlayNetworking.registerGlobalReceiver(type,
                        (payload, context) -> ((IMessage) payload).onMessage(context.player()));
            }
            this.packets.add(new Packet(type, instance.isClient()));
        }
    }

    /**
     * Registers the handlers of clientbound packets. Only call this on the client.
     */
    public void registerClient() {
        for (var packet : this.packets) {
            if (packet.client()) {
                // In its own class: the handler passes a LocalPlayer, which can't be loaded on dedicated servers
                ClientPacketHandlers.register(packet.type());
            }
        }
    }

    /**
     * Called before the packet types are registered, mods register their packets here.
     */
    protected void initPackets() {
    }

    protected StreamCodec<? super RegistryFriendlyByteBuf, ? extends IMessage> codec(Supplier<? extends IMessage> factory) {
        return StreamCodec.of(
                (pBuffer, pValue) -> pValue.toBytes(pBuffer),
                pBuffer -> {
                    IMessage msg = factory.get();
                    msg.fromBytes(pBuffer);
                    return msg;
                }
        );
    }

    public void sendToAll(IMessage message) {
        var server = Glodium.INSTANCE.getServer();
        if (server != null) {
            for (var player : PlayerLookup.all(server)) {
                ServerPlayNetworking.send(player, message);
            }
        }
    }

    public void sendTo(IMessage message, ServerPlayer player) {
        ServerPlayNetworking.send(player, message);
    }

    public void sendToAllAround(IMessage message, ServerLevel world, BlockPos pos, double r, @Nullable ServerPlayer excludePlayer) {
        this.sendToAllAround(message, world, Vec3.atCenterOf(pos), r, excludePlayer);
    }

    public void sendToAllAround(IMessage message, ServerLevel world, Position pos, double r, @Nullable ServerPlayer excludePlayer) {
        for (var player : PlayerLookup.around(world, new Vec3(pos.x(), pos.y(), pos.z()), r)) {
            if (player != excludePlayer) {
                ServerPlayNetworking.send(player, message);
            }
        }
    }

    public void sendToServer(IMessage message) {
        ClientPlayNetworking.send(message);
    }

    private record Packet(CustomPacketPayload.Type<?> type, boolean client) {
    }

}
