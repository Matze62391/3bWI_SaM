package appeng.server.testplots;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fired when the test world command kits out a player, so addons can give extra items.
 */
public class KitOutPlayerEvent {
    public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class,
            listeners -> event -> {
                for (var listener : listeners) {
                    listener.onKitOutPlayer(event);
                }
            });

    @FunctionalInterface
    public interface Listener {
        void onKitOutPlayer(KitOutPlayerEvent event);
    }

    private final ServerPlayer player;

    public KitOutPlayerEvent(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer getPlayer() {
        return player;
    }
}
