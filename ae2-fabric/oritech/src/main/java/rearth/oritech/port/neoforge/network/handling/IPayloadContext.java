package rearth.oritech.port.neoforge.network.handling;

import java.util.concurrent.CompletableFuture;

import net.minecraft.world.entity.player.Player;

/**
 * The context a payload handler is called with. Handlers run on the main thread of the receiving side.
 */
public interface IPayloadContext {
    Player player();

    default CompletableFuture<Void> enqueueWork(Runnable task) {
        task.run();
        return CompletableFuture.completedFuture(null);
    }
}
