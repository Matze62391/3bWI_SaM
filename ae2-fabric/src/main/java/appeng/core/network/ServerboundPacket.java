package appeng.core.network;

import net.minecraft.server.level.ServerPlayer;

public interface ServerboundPacket extends CustomAppEngPayload {
    void handleOnServer(ServerPlayer player);
}
