package appeng.blockentity;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;

import appeng.hooks.ticking.TickHandler;

/**
 * Calls {@link AEBaseBlockEntity#onChunkUnloaded()} (a NeoForge block entity extension) when the chunk of an AE2
 * block entity unloads.
 * <p>
 * Unloading is deferred until the end of the tick, after the chunk has been saved to disk. The CHUNK_UNLOAD event runs
 * before the chunk has been saved, and if we disconnect nodes at that point, the saved data will be missing information
 * from the node (such as the player id).
 */
final class DeferredBlockEntityUnloader {
    private DeferredBlockEntityUnloader() {
    }

    static void register() {
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
            List<AEBaseBlockEntity> entitiesToRemove = null;
            for (var blockEntity : chunk.getBlockEntities().values()) {
                if (blockEntity instanceof AEBaseBlockEntity aeBlockEntity) {
                    if (entitiesToRemove == null) {
                        entitiesToRemove = new ArrayList<>();
                    }
                    entitiesToRemove.add(aeBlockEntity);
                }
            }
            if (entitiesToRemove != null) {
                var toRemove = entitiesToRemove;
                TickHandler.instance().addCallable(level, () -> {
                    for (var blockEntity : toRemove) {
                        blockEntity.onChunkUnloaded();
                    }
                });
            }
        });
    }
}
