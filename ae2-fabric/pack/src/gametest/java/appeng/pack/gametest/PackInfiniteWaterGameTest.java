package appeng.pack.gametest;

import java.util.ArrayList;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEFluidKey;
import appeng.api.util.AEColor;
import appeng.core.definitions.AEParts;
import appeng.pack.InfiniteWaterSources;
import appeng.parts.storagebus.StorageBusPart;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A storage bus on a Macaw's Furniture kitchen sink shows endless water in the terminal. Only runs when Macaw's
 * Furniture is installed (the world test adds it).
 */
public class PackInfiniteWaterGameTest implements FabricClientGameTest {
    private static final Logger LOG = LoggerFactory.getLogger(PackInfiniteWaterGameTest.class);
    private static final long BUCKET = FluidConstants.BUCKET;
    // AE2's storage bus reports at most 2^42 droplets of one resource (~54 million buckets)
    private static final long SHOWN = Math.min(InfiniteWaterSources.DISPLAYED_AMOUNT, 1L << 42);

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!FabricLoader.getInstance().isModLoaded("mcwfurnitures")) {
            LOG.info("Macaw's Furniture is not installed, skipping the kitchen sink test");
            return;
        }
        try (var world = context.worldBuilder()
                .adjustSettings(settings -> settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
                .create()) {
            var server = world.getServer();
            server.runCommand("gamerule advance_time false");
            server.runCommand("gamerule send_command_feedback false");
            context.waitTicks(40);
            var origin = server.computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().blockPosition());
            var failures = new ArrayList<String>();

            // energy cell -> cable with storage bus (east) and terminal (north, facing the player) -> sink
            var cell = origin.offset(-1, 0, 3);
            var cable = cell.east();
            var sink = cable.east();
            setBlock(server, cell, "ae2:creative_energy_cell");
            setBlock(server, sink, "mcwfurnitures:oak_kitchen_sink[facing=west]");
            var bus = server.computeOnServer(s -> {
                var level = s.overworld();
                PartHelper.setPart(level, cable, null, null, AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT));
                PartHelper.setPart(level, cable, Direction.NORTH, null, AEParts.TERMINAL.get());
                return PartHelper.setPart(level, cable, Direction.EAST, null, AEParts.STORAGE_BUS.get());
            });

            var water = AEFluidKey.of(Fluids.WATER);
            var lava = AEFluidKey.of(Fluids.LAVA);
            long shown = -1;
            for (var i = 0; i < 20 && shown <= 0; i++) {
                context.waitTicks(10);
                shown = server.computeOnServer(s -> available(bus, water));
            }
            LOG.info("The network shows {} buckets of water from the empty sink", shown / BUCKET);
            if (shown != SHOWN) {
                failures.add("empty sink shows " + shown + " droplets of water");
            }

            // Taking water does not use it up
            var extracted = server.computeOnServer(s -> bus.getMainNode().getGrid().getStorageService().getInventory()
                    .extract(water, 5000 * BUCKET, Actionable.MODULATE, IActionSource.empty()));
            var after = server.computeOnServer(s -> available(bus, water));
            if (extracted != 5000 * BUCKET || after != SHOWN) {
                failures.add("extracting water (" + extracted + ", afterwards " + after + ")");
            }
            // Only water, and nothing can be put in
            var lavaOut = server.computeOnServer(s -> bus.getMainNode().getGrid().getStorageService().getInventory()
                    .extract(lava, BUCKET, Actionable.MODULATE, IActionSource.empty()));
            var waterIn = server.computeOnServer(s -> bus.getMainNode().getGrid().getStorageService().getInventory()
                    .insert(water, BUCKET, Actionable.MODULATE, IActionSource.empty()));
            if (lavaOut != 0 || waterIn != 0) {
                failures.add("sink gave lava (" + lavaOut + ") or took water (" + waterIn + ")");
            }

            // A filled sink and a stripped sink work the same way
            setBlock(server, sink, "mcwfurnitures:stripped_spruce_kitchen_sink[facing=west,water=true]");
            context.waitTicks(40);
            var filled = server.computeOnServer(s -> available(bus, water));
            if (filled != SHOWN) {
                failures.add("filled stripped sink shows " + filled);
            }

            // The terminal lists the water
            context.getInput().lookAt(cable);
            context.getInput().pressKey(options -> options.keyUse);
            context.waitTicks(40);
            var screen = context.computeOnClient(mc -> mc.gui.screen() == null ? "none" : mc.gui.screen().getClass().getSimpleName());
            LOG.info("Opened {}", screen);
            context.takeScreenshot("pack-sink-terminal");
            context.runOnClient(mc -> mc.gui.setScreen(null));

            if (!failures.isEmpty()) {
                throw new AssertionError("Kitchen sink water failed: " + String.join(", ", failures));
            }
            LOG.info("Kitchen sink is an infinite water source");
        }
    }

    private static long available(StorageBusPart bus, AEFluidKey key) {
        var grid = bus.getMainNode().getGrid();
        if (grid == null || !bus.getMainNode().isActive()) {
            return -1;
        }
        return grid.getStorageService().getCachedInventory().get(key);
    }

    private static void setBlock(TestServerContext server, BlockPos pos, String block) {
        server.runCommand("setblock %d %d %d %s".formatted(pos.getX(), pos.getY(), pos.getZ(), block));
    }
}
