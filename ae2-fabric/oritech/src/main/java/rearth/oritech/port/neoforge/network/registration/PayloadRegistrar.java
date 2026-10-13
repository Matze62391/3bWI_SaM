package rearth.oritech.port.neoforge.network.registration;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import rearth.oritech.port.neoforge.network.handling.IPayloadHandler;

/**
 * Registers play payloads with Fabric's networking API. Client-bound handlers are kept until the client
 * initializes ({@code ClientPayloadHandlers}).
 */
public class PayloadRegistrar {
    public record ClientHandler<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
            IPayloadHandler<T> handler) {
    }

    private static final List<ClientHandler<?>> CLIENT_HANDLERS = new ArrayList<>();

    private final String version;

    public PayloadRegistrar(String version) {
        this.version = version;
    }

    public static List<ClientHandler<?>> clientHandlers() {
        return CLIENT_HANDLERS;
    }

    @SuppressWarnings("unchecked")
    public <T extends CustomPacketPayload> PayloadRegistrar playToServer(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, IPayloadHandler<T> handler) {
        PayloadTypeRegistry.serverboundPlay().register(type, (StreamCodec<RegistryFriendlyByteBuf, T>) codec);
        ServerPlayNetworking.registerGlobalReceiver(type,
                (payload, context) -> handler.handle(payload, context::player));
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T extends CustomPacketPayload> PayloadRegistrar playToClient(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, IPayloadHandler<T> handler) {
        PayloadTypeRegistry.clientboundPlay().register(type, (StreamCodec<RegistryFriendlyByteBuf, T>) codec);
        CLIENT_HANDLERS.add(new ClientHandler<>(type, handler));
        return this;
    }
}
