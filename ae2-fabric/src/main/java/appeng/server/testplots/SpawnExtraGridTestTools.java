package appeng.server.testplots;

import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;

/**
 * Triggered to spawn additional testing tools into a container placed next to a spawned AE2 grid.
 */
@TestPlotClass
public class SpawnExtraGridTestTools {
    public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class,
            listeners -> event -> {
                for (var listener : listeners) {
                    listener.onSpawnExtraGridTestTools(event);
                }
            });

    static {
        EVENT.register(SpawnTestTools::spawnWirelessTerminals);
    }

    @FunctionalInterface
    public interface Listener {
        void onSpawnExtraGridTestTools(SpawnExtraGridTestTools event);
    }

    private final Identifier plotId;
    private final InternalInventory inventory;
    private final IGrid grid;

    public SpawnExtraGridTestTools(Identifier plotId, InternalInventory inventory, IGrid grid) {
        this.plotId = plotId;
        this.inventory = inventory;
        this.grid = grid;
    }

    public Identifier getPlotId() {
        return plotId;
    }

    public InternalInventory getInventory() {
        return inventory;
    }

    public IGrid getGrid() {
        return grid;
    }
}
