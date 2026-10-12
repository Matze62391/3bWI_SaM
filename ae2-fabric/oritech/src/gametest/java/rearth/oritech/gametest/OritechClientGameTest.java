package rearth.oritech.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.material.Fluids;

import team.reborn.energy.api.EnergyStorage;

/**
 * Builds Oritech machines in a world, checks that they work through Fabric's transfer and energy APIs and takes
 * screenshots ({@code oritech/build/run/clientGameTest/screenshots}).
 */
public class OritechClientGameTest implements FabricClientGameTest {
    private static final Logger LOG = LoggerFactory.getLogger(OritechClientGameTest.class);

    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().adjustSettings(settings -> {
            settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            settings.getNormalPresetList().stream()
                    .filter(entry -> entry.preset() != null && entry.preset().is(WorldPresets.FLAT))
                    .findFirst()
                    .ifPresent(settings::setWorldType);
        }).create()) {
            var server = world.getServer();
            server.runCommand("gamerule advance_time false");
            server.runCommand("gamerule send_command_feedback false");
            server.runCommand("time set noon");
            context.waitTicks(40);
            var origin = server.computeOnServer(s -> firstPlayer(s).blockPosition());
            var failures = new ArrayList<String>();

            // Oritech's recipe types are loaded
            for (var type : List.of("pulverizer", "fragment_forge", "assembler", "foundry", "centrifuge",
                    "atomic_forge", "refinery", "bio_generator", "reactor", "laser")) {
                var count = server.computeOnServer(s -> s.getRecipeManager().getRecipes().stream()
                        .filter(holder -> BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType())
                                .equals(Identifier.fromNamespaceAndPath("oritech", type)))
                        .count());
                LOG.info("{} oritech:{} recipes", count, type);
                if (count == 0) {
                    failures.add("recipes of oritech:" + type);
                }
            }

            // A pulverizer powered and fed through Fabric's energy and item APIs grinds raw iron to dust
            var pulverizer = origin.offset(-3, 0, 4);
            setBlock(server, pulverizer, "oritech:pulverizer[facing=south]");
            context.waitTicks(5);
            var energy = server.computeOnServer(s -> insertEnergy(s, pulverizer, 100_000));
            var items = server.computeOnServer(s -> insertItem(s, pulverizer, ItemVariant.of(Items.RAW_IRON), 4));
            LOG.info("Pulverizer accepted {} energy and {} raw iron", energy, items);
            if (energy <= 0 || items != 4) {
                failures.add("pulverizer accepts energy/items (" + energy + "/" + items + ")");
            }
            // machines limit the energy accepted per tick, so keep feeding it like a cable would
            if (!waitFor(context, server, s -> {
                insertEnergy(s, pulverizer, 100_000);
                return countItem(s, pulverizer, "oritech:iron_dust") > 0;
            }, 1200)) {
                failures.add("pulverizer grinds raw iron");
            }

            // A basic generator burns coal into energy (the burn time comes from the item's cooking fuel component)
            var generator = origin.offset(-1, 0, 4);
            setBlock(server, generator, "oritech:basic_generator[facing=south]");
            context.waitTicks(5);
            var coal = server.computeOnServer(s -> insertItem(s, generator, ItemVariant.of(Items.COAL), 2));
            if (coal != 2 || !waitFor(context, server, s -> storedEnergy(s, generator) > 0, 400)) {
                failures.add("basic generator burns coal (" + coal + ")");
            }
            long generatorEnergy = server.computeOnServer(s -> storedEnergy(s, generator));
            LOG.info("Basic generator stored {} energy", generatorEnergy);

            // The creative energy storage supplies energy
            var creative = origin.offset(1, 0, 4);
            setBlock(server, creative, "oritech:creative_storage");
            context.waitTicks(5);
            var extracted = server.computeOnServer(s -> {
                long best = -1;
                for (var side : Direction.values()) {
                    var storage = EnergyStorage.SIDED.find(s.overworld(), creative, side);
                    if (storage == null) continue;
                    try (var tx = Transaction.openOuter()) {
                        best = Math.max(best, storage.extract(5_000, tx));
                    }
                }
                return best;
            });
            LOG.info("Extracted {} energy from the creative storage", extracted);
            if (extracted <= 0) {
                failures.add("creative storage supplies energy (" + extracted + ")");
            }

            // A portable tank stores fluids (Oritech counts millibuckets, Fabric droplets)
            var tank = origin.offset(3, 0, 4);
            setBlock(server, tank, "oritech:portable_tank");
            context.waitTicks(5);
            var water = server.computeOnServer(s -> {
                var storage = FluidStorage.SIDED.find(s.overworld(), tank, Direction.UP);
                if (storage == null) return -1L;
                try (var tx = Transaction.openOuter()) {
                    var inserted = storage.insert(FluidVariant.of(Fluids.WATER), 2 * FluidConstants.BUCKET, tx);
                    tx.commit();
                    return inserted;
                }
            });
            var stored = server.computeOnServer(s -> {
                var storage = FluidStorage.SIDED.find(s.overworld(), tank, Direction.UP);
                return storage == null ? -1L
                        : StorageUtil.findStoredResource(storage) == null ? 0L
                                : storage.iterator().next().getAmount();
            });
            LOG.info("Portable tank accepted {} droplets of water, stores {}", water, stored);
            if (water != 2 * FluidConstants.BUCKET || stored != 2 * FluidConstants.BUCKET) {
                failures.add("portable tank stores water (" + water + "/" + stored + ")");
            }

            // A basic generator and a few animated machines for the screenshot
            setBlock(server, origin.offset(-3, 0, 6), "oritech:powered_furnace[facing=south]");
            setBlock(server, origin.offset(-1, 0, 6), "oritech:fragment_forge[facing=south]");
            setBlock(server, origin.offset(1, 0, 6), "oritech:assembler[facing=south]");
            setBlock(server, origin.offset(3, 0, 6), "oritech:bio_generator[facing=south]");
            setBlock(server, origin.offset(-2, 0, 9), "oritech:nickel_ore");
            setBlock(server, origin.offset(0, 0, 9), "oritech:machine_frame");
            setBlock(server, origin.offset(2, 0, 9), "oritech:energy_pipe");
            context.waitTicks(40);
            worldScreenshot(context, origin.offset(0, 0, 4), "oritech-machines");

            // Screens
            for (var entry : List.of(new Object[] { pulverizer, "pulverizer" }, new Object[] { generator, "basic-generator" },
                    new Object[] { creative, "creative-storage" }, new Object[] { tank, "portable-tank" })) {
                var pos = (BlockPos) entry[0];
                // stand right in front of the block so nothing is in the way
                server.runCommand("tp @p %d %d %d".formatted(pos.getX(), pos.getY(), pos.getZ() - 2));
                context.waitTicks(5);
                if (!openAndScreenshot(context, pos, "oritech-" + entry[1])) {
                    failures.add("screen of " + entry[1]);
                }
            }

            // Items render in the inventory
            server.runCommand("give @p oritech:target_designator");
            server.runCommand("give @p oritech:chainsaw");
            server.runCommand("give @p oritech:portable_tank");
            server.runCommand("give @p oritech:exo_helmet");
            context.waitTicks(10);
            context.runOnClient(mc -> mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
            if (!screenshotOpenedScreen(context, "oritech-inventory")) {
                failures.add("inventory screen");
            }

            if (!failures.isEmpty()) {
                throw new AssertionError("Oritech checks failed: " + failures);
            }
            LOG.info("All Oritech checks passed");
        }
    }

    private static EnergyStorage findEnergy(MinecraftServer server, BlockPos pos) {
        for (var side : Direction.values()) {
            var storage = EnergyStorage.SIDED.find(server.overworld(), pos, side);
            if (storage != null) return storage;
        }
        return EnergyStorage.SIDED.find(server.overworld(), pos, null);
    }

    private static long insertEnergy(MinecraftServer server, BlockPos pos, long amount) {
        var storage = findEnergy(server, pos);
        if (storage == null) return -1;
        try (var tx = Transaction.openOuter()) {
            var inserted = storage.insert(amount, tx);
            tx.commit();
            return inserted;
        }
    }

    private static long insertItem(MinecraftServer server, BlockPos pos, ItemVariant item, long amount) {
        var storage = ItemStorage.SIDED.find(server.overworld(), pos, Direction.UP);
        if (storage == null) return -1;
        try (var tx = Transaction.openOuter()) {
            var inserted = storage.insert(item, amount, tx);
            tx.commit();
            return inserted;
        }
    }

    private static long storedEnergy(MinecraftServer server, BlockPos pos) {
        var storage = findEnergy(server, pos);
        return storage == null ? -1 : storage.getAmount();
    }

    private static long countItem(MinecraftServer server, BlockPos pos, String id) {
        var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
        var storage = ItemStorage.SIDED.find(server.overworld(), pos, Direction.DOWN);
        if (storage == null) return 0;
        long count = 0;
        for (var view : storage) {
            if (view.getResource().isOf(item)) count += view.getAmount();
        }
        return count;
    }

    private static ServerPlayer firstPlayer(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static void setBlock(TestServerContext server, BlockPos pos, String block) {
        server.runCommand("setblock %d %d %d %s".formatted(pos.getX(), pos.getY(), pos.getZ(), block));
    }

    private static void worldScreenshot(ClientGameTestContext context, BlockPos lookAt, String name) {
        if (lookAt != null) {
            context.getInput().lookAt(lookAt);
        }
        context.runOnClient(mc -> {
            if (!mc.gui.hud.isHidden()) {
                mc.gui.hud.toggle();
            }
        });
        context.waitTicks(10);
        context.takeScreenshot(name);
        context.runOnClient(mc -> {
            if (mc.gui.hud.isHidden()) {
                mc.gui.hud.toggle();
            }
        });
    }

    private static boolean openAndScreenshot(ClientGameTestContext context, BlockPos pos, String name) {
        context.waitTicks(5);
        context.getInput().lookAt(pos);
        context.waitTicks(2);
        context.getInput().pressKey(options -> options.keyUse);
        return screenshotOpenedScreen(context, name);
    }

    private static boolean screenshotOpenedScreen(ClientGameTestContext context, String name) {
        boolean opened;
        try {
            context.waitFor(mc -> mc.gui.screen() != null, 60);
            context.waitTicks(10);
            String screen = context.computeOnClient(mc -> mc.gui.screen().getClass().getName());
            LOG.info("Opened screen for {}: {}", name, screen);
            opened = true;
        } catch (AssertionError e) {
            LOG.warn("No screen opened for {}", name);
            opened = false;
        }
        context.takeScreenshot(name);
        context.runOnClient(mc -> {
            if (mc.player != null && mc.player.containerMenu != mc.player.inventoryMenu) {
                mc.player.closeContainer();
            } else {
                mc.gui.setScreen(null);
            }
        });
        context.waitTicks(5);
        return opened;
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
}
