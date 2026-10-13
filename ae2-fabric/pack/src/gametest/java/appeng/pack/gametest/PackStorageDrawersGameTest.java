package appeng.pack.gametest;

import java.util.ArrayList;
import java.util.function.Predicate;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.util.AEColor;
import appeng.core.definitions.AEParts;
import appeng.parts.storagebus.StorageBusPart;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Storage Drawers ships in the pack: an AE2 storage bus on a drawer (and on a drawer controller) sees, fills and empties
 * the drawers through the Fabric transfer API.
 */
public class PackStorageDrawersGameTest implements FabricClientGameTest {
    private static final Logger LOG = LoggerFactory.getLogger(PackStorageDrawersGameTest.class);

    @Override
    public void runTest(ClientGameTestContext context) {
        var drawers = FabricLoader.getInstance().getModContainer("storagedrawers")
                .orElseThrow(() -> new AssertionError("Storage Drawers is not loaded"));
        LOG.info("Storage Drawers {} loaded", drawers.getMetadata().getVersion().getFriendlyString());

        try (var world = context.worldBuilder()
                .adjustSettings(settings -> settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
                .create()) {
            var server = world.getServer();
            server.runCommand("gamerule advance_time false");
            server.runCommand("gamerule send_command_feedback false");
            context.waitTicks(40);
            var origin = server.computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().blockPosition());
            var failures = new ArrayList<String>();

            // energy cell -> cable + storage bus -> single drawer
            var cell = origin.offset(-2, 0, 4);
            var bus = cell.east();
            var drawer = bus.east();
            setBlock(server, cell, "ae2:creative_energy_cell");
            setBlock(server, drawer, "storagedrawers:oak_full_drawers_1");
            var drawerBus = placeStorageBus(server, bus);

            var inserted = server.computeOnServer(s -> {
                var storage = ItemStorage.SIDED.find(s.overworld(), drawer, Direction.WEST);
                if (storage == null) {
                    return -1L;
                }
                try (var tx = Transaction.openOuter()) {
                    var amount = storage.insert(ItemVariant.of(Items.COBBLESTONE), 100, tx);
                    tx.commit();
                    return amount;
                }
            });
            if (inserted != 100) {
                failures.add("drawer item storage (inserted " + inserted + ")");
            }
            if (!waitFor(context, server, s -> inNetwork(drawerBus, Items.COBBLESTONE) == 100, 200)) {
                failures.add("network sees drawer contents (" + inNetwork(server, drawerBus, Items.COBBLESTONE) + ")");
            }

            var networkInserted = server.computeOnServer(s -> drawerBus.getMainNode().getGrid().getStorageService()
                    .getInventory().insert(AEItemKey.of(Items.COBBLESTONE), 50, Actionable.MODULATE,
                            IActionSource.empty()));
            var networkExtracted = server.computeOnServer(s -> drawerBus.getMainNode().getGrid().getStorageService()
                    .getInventory().extract(AEItemKey.of(Items.COBBLESTONE), 30, Actionable.MODULATE,
                            IActionSource.empty()));
            var inDrawer = inDrawer(server, drawer, Items.COBBLESTONE);
            LOG.info("Network inserted {}, extracted {}, drawer holds {}", networkInserted, networkExtracted, inDrawer);
            if (networkInserted != 50 || networkExtracted != 30 || inDrawer != 120) {
                failures.add("network insert/extract into drawer (" + networkInserted + "/" + networkExtracted + "/"
                        + inDrawer + ")");
            }
            // A drawer locked to cobblestone does not take anything else
            var rejected = server.computeOnServer(s -> drawerBus.getMainNode().getGrid().getStorageService()
                    .getInventory().insert(AEItemKey.of(Items.DIRT), 10, Actionable.SIMULATE, IActionSource.empty()));
            if (rejected != 0) {
                failures.add("drawer accepted a second item type (" + rejected + ")");
            }

            // energy cell -> cable + storage bus -> drawer controller with two drawers
            var cell2 = origin.offset(-2, 0, 7);
            var bus2 = cell2.east();
            var controller = bus2.east();
            var drawerA = controller.east();
            var drawerB = controller.above();
            setBlock(server, cell2, "ae2:creative_energy_cell");
            setBlock(server, controller, "storagedrawers:controller");
            setBlock(server, drawerA, "storagedrawers:spruce_full_drawers_2");
            setBlock(server, drawerB, "storagedrawers:birch_full_drawers_4");
            insertInto(server, drawerA, Items.IRON_INGOT, 64);
            insertInto(server, drawerB, Items.GOLD_INGOT, 32);
            context.waitTicks(20);
            var controllerBus = placeStorageBus(server, bus2);
            if (!waitFor(context, server, s -> inNetwork(controllerBus, Items.IRON_INGOT) == 64
                    && inNetwork(controllerBus, Items.GOLD_INGOT) == 32, 200)) {
                failures.add("network sees drawers behind the controller (" + inNetwork(server, controllerBus,
                        Items.IRON_INGOT) + " iron, " + inNetwork(server, controllerBus, Items.GOLD_INGOT) + " gold)");
            }
            var viaController = server.computeOnServer(s -> controllerBus.getMainNode().getGrid().getStorageService()
                    .getInventory().insert(AEItemKey.of(Items.IRON_INGOT), 16, Actionable.MODULATE,
                            IActionSource.empty()));
            if (viaController != 16 || inDrawer(server, drawerA, Items.IRON_INGOT) != 80) {
                failures.add("insert through the controller (" + viaController + ")");
            }

            context.getInput().lookAt(drawer);
            context.waitTicks(10);
            context.takeScreenshot("pack-storage-drawers");

            if (!failures.isEmpty()) {
                throw new AssertionError("Storage Drawers integration failed: " + String.join(", ", failures));
            }
            LOG.info("Storage Drawers integration OK");
        }
    }

    private static StorageBusPart placeStorageBus(TestServerContext server, BlockPos pos) {
        return server.computeOnServer(s -> {
            PartHelper.setPart(s.overworld(), pos, null, null, AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT));
            return PartHelper.setPart(s.overworld(), pos, Direction.EAST, null, AEParts.STORAGE_BUS.get());
        });
    }

    private static long inNetwork(StorageBusPart bus, Item item) {
        var grid = bus.getMainNode().getGrid();
        if (grid == null || !bus.getMainNode().isActive()) {
            return -1;
        }
        return grid.getStorageService().getInventory().extract(AEItemKey.of(item), Long.MAX_VALUE,
                Actionable.SIMULATE, IActionSource.empty());
    }

    private static long inNetwork(TestServerContext server, StorageBusPart bus, Item item) {
        return server.computeOnServer(s -> inNetwork(bus, item));
    }

    private static void insertInto(TestServerContext server, BlockPos pos, Item item, long amount) {
        server.runOnServer(s -> {
            var storage = ItemStorage.SIDED.find(s.overworld(), pos, Direction.UP);
            try (var tx = Transaction.openOuter()) {
                storage.insert(ItemVariant.of(item), amount, tx);
                tx.commit();
            }
        });
    }

    private static long inDrawer(TestServerContext server, BlockPos pos, Item item) {
        return server.computeOnServer(s -> {
            var storage = ItemStorage.SIDED.find(s.overworld(), pos, Direction.UP);
            try (var tx = Transaction.openOuter()) {
                return storage.extract(ItemVariant.of(item), Long.MAX_VALUE, tx);
            }
        });
    }

    private static boolean waitFor(ClientGameTestContext context, TestServerContext server,
            Predicate<MinecraftServer> condition, int maxTicks) {
        for (var i = 0; i < maxTicks; i += 10) {
            if (server.computeOnServer(condition::test)) {
                return true;
            }
            context.waitTicks(10);
        }
        return false;
    }

    private static void setBlock(TestServerContext server, BlockPos pos, String block) {
        server.runCommand("setblock %d %d %d %s".formatted(pos.getX(), pos.getY(), pos.getZ(), block));
    }
}
