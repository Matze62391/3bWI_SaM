package gripe._90.megacells.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import appeng.api.config.Actionable;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.StorageCells;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.AppEng;

import guideme.Guides;
import guideme.PageAnchor;
import guideme.compiler.ParsedGuidePage;
import guideme.internal.screen.GuideScreen;

import gripe._90.megacells.definition.MEGAItems;
import gripe._90.megacells.item.cell.BulkCellItem;

/**
 * Builds MEGA Cells' machines and storage in a world, checks that they work and takes screenshots
 * ({@code megacells/build/run/clientGameTest/screenshots}).
 */
public class MEGAClientGameTest implements FabricClientGameTest {
    private static final Logger LOG = LoggerFactory.getLogger(MEGAClientGameTest.class);

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

            // Recipes for the cells and machines are loaded
            for (var recipe : List.of("cells/standard/item_storage_cell_256m", "cells/standard/fluid_storage_cell_1m",
                    "cells/standard/bulk_item_cell", "crafting/mega_crafting_unit", "network/mega_interface",
                    "network/mega_pattern_provider")) {
                var key = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("megacells", recipe));
                if (!server.computeOnServer(s -> s.getRecipeManager().byKey(key).isPresent())) {
                    failures.add("recipe " + recipe);
                }
            }

            // A drive with a 256M item cell stores 2 billion items (AE2's biggest cell holds about 2 million)
            var cell = origin.offset(-2, 0, 4);
            var drive = cell.east();
            setBlock(server, cell, "ae2:creative_energy_cell");
            setBlock(server, drive, "ae2:drive");
            server.runOnServer(s -> {
                var be = (DriveBlockEntity) s.overworld().getBlockEntity(drive);
                be.getInternalInventory().insertItem(0, MEGAItems.ITEM_CELL_256M.stack(), false);
            });
            waitForServer(context, server, s -> grid(s, drive) != null
                    && grid(s, drive).getStorageService().getInventory().getAvailableStacks().isEmpty()
                    && ((DriveBlockEntity) s.overworld().getBlockEntity(drive)).getMainNode().isActive(), 200,
                    "the drive to be online");
            var cobble = AEItemKey.of(Items.COBBLESTONE);
            var inserted = server.computeOnServer(s -> grid(s, drive).getStorageService().getInventory()
                    .insert(cobble, 2_000_000_000L, Actionable.MODULATE, IActionSource.empty()));
            LOG.info("Inserted {} cobblestone into a 256M item cell", inserted);
            if (inserted != 2_000_000_000L) {
                failures.add("256M cell stores 2 billion items (" + inserted + ")");
            }

            // A bulk cell partitioned to iron ingots with a compression card also offers iron blocks
            server.runOnServer(s -> {
                var be = (DriveBlockEntity) s.overworld().getBlockEntity(drive);
                var bulk = MEGAItems.BULK_ITEM_CELL.stack();
                var item = (BulkCellItem) bulk.getItem();
                item.getConfigInventory(bulk).setStack(0, GenericStack.fromItemStack(new ItemStack(Items.IRON_INGOT)));
                item.getUpgrades(bulk).addItems(MEGAItems.COMPRESSION_CARD.stack());
                be.getInternalInventory().insertItem(1, bulk, false);
            });
            context.waitTicks(20);
            var iron = server.computeOnServer(s -> {
                var inv = grid(s, drive).getStorageService().getInventory();
                var stored = inv.insert(AEItemKey.of(Items.IRON_INGOT), 900, Actionable.MODULATE,
                        IActionSource.empty());
                var before = inv.getAvailableStacks().get(AEItemKey.of(Items.IRON_BLOCK));
                // Taking ingots out decompresses a block
                var taken = inv.extract(AEItemKey.of(Items.IRON_INGOT), 5, Actionable.MODULATE,
                        IActionSource.empty());
                var available = inv.getAvailableStacks();
                return stored + "/" + before + "/" + taken + "/" + available.get(AEItemKey.of(Items.IRON_INGOT))
                        + "/" + available.get(AEItemKey.of(Items.IRON_BLOCK));
            });
            LOG.info("Bulk cell with compression: inserted/blocks/taken ingots/ingots/blocks {}", iron);
            if (!iron.equals("900/100/5/4/99")) {
                failures.add("bulk cell compression (" + iron + ")");
            }
            worldScreenshot(context, origin, "mega-drive");

            // A MEGA crafting CPU: 3x3x3 of units with a storage and an accelerator, next to the energy cell
            var cpuMin = origin.offset(3, 0, 4);
            fill(server, cpuMin, cpuMin.offset(2, 2, 2), "megacells:mega_crafting_unit");
            setBlock(server, cpuMin.offset(1, 1, 1), "megacells:256m_crafting_storage");
            setBlock(server, cpuMin.offset(1, 2, 1), "megacells:mega_crafting_accelerator");
            setBlock(server, cpuMin.offset(1, 1, 0), "megacells:mega_crafting_monitor");
            setBlock(server, cpuMin.offset(-1, 0, 1), "ae2:creative_energy_cell");
            if (!waitFor(context, server, s -> {
                var g = grid(s, cpuMin);
                return g != null && g.getCraftingService().getCpus().stream()
                        .anyMatch(cpu -> cpu.getAvailableStorage() >= 256L * 1024 * 1024
                                && cpu.getCoProcessors() >= 1);
            }, 200)) {
                failures.add("MEGA crafting CPU");
            }
            context.getInput().lookAt(cpuMin.offset(1, 1, 0));
            context.waitTicks(10);
            worldScreenshot(context, null, "mega-crafting-cpu");
            if (!openAndScreenshot(context, cpuMin.offset(1, 1, 0), "mega-crafting-cpu-status")) {
                failures.add("crafting CPU screen");
            }

            // Screens of the machines and parts
            var screens = origin.offset(-3, 0, -3);
            setBlock(server, screens, "megacells:mega_interface");
            if (!openAndScreenshot(context, screens, "mega-interface")) {
                failures.add("MEGA interface screen");
            }
            setBlock(server, screens, "megacells:mega_pattern_provider");
            if (!openAndScreenshot(context, screens, "mega-pattern-provider")) {
                failures.add("MEGA pattern provider screen");
            }
            setBlock(server, screens, "minecraft:air");
            server.runOnServer(s -> PartHelper.setPart(s.overworld(), screens, Direction.SOUTH, null,
                    MEGAItems.CELL_DOCK.get()));
            if (!openAndScreenshot(context, screens, "mega-cell-dock")) {
                failures.add("cell dock screen");
            }
            setBlock(server, screens, "minecraft:air");

            server.runOnServer(s -> firstPlayer(s).setItemInHand(InteractionHand.MAIN_HAND,
                    MEGAItems.PORTABLE_CELL_WORKBENCH.stack()));
            context.waitTicks(10);
            context.getInput().lookAt(origin.above(30));
            context.getInput().pressKey(options -> options.keyUse);
            if (!screenshotOpenedScreen(context, "mega-portable-cell-workbench")) {
                failures.add("portable cell workbench screen");
            }
            server.runOnServer(s -> firstPlayer(s).setItemInHand(InteractionHand.MAIN_HAND,
                    MEGAItems.PORTABLE_ITEM_CELL_1M.stack()));
            context.waitTicks(10);
            context.getInput().pressKey(options -> options.keyUse);
            if (!screenshotOpenedScreen(context, "mega-portable-item-cell")) {
                failures.add("portable item cell screen");
            }
            server.runOnServer(s -> firstPlayer(s).setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));

            // Every item is in MEGA Cells' creative tab exactly once
            var tabItems = context.computeOnClient(mc -> {
                var tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(Identifier.fromNamespaceAndPath("megacells", "tab"));
                return tab.getDisplayItems().stream().map(ItemStack::getItem).toList();
            });
            if (tabItems.size() != new java.util.HashSet<>(tabItems).size()) {
                failures.add("duplicate creative tab entries");
            }
            LOG.info("MEGA Cells creative tab has {} items", tabItems.size());

            // MEGA Cells' pages in AE2's guidebook
            var guide = Guides.getById(AppEng.makeId("guide"));
            var pageIds = guide.getPages().stream()
                    .map(ParsedGuidePage::getId)
                    .filter(id -> id.getNamespace().equals("megacells"))
                    .sorted()
                    .toList();
            LOG.info("MEGA Cells guide pages: {}", pageIds);
            if (pageIds.isEmpty()) {
                failures.add("guide pages");
            }
            for (var pageId : pageIds) {
                context.runOnClient(mc -> mc.gui.setScreen(GuideScreen.openNew(guide, PageAnchor.page(pageId))));
                context.waitTicks(10);
                context.takeScreenshot("mega-guide-" + pageId.getPath().replace('/', '-'));
            }
            context.runOnClient(mc -> mc.gui.setScreen(null));

            if (!failures.isEmpty()) {
                throw new AssertionError("MEGA Cells checks failed: " + String.join(", ", failures));
            }
            LOG.info("MEGA Cells checks passed");
        }
    }

    private static appeng.api.networking.IGrid grid(MinecraftServer server, BlockPos pos) {
        if (!(server.overworld().getBlockEntity(pos) instanceof IInWorldGridNodeHost host)) {
            return null;
        }
        for (var side : Direction.values()) {
            var node = host.getGridNode(side);
            if (node != null) {
                return node.getGrid();
            }
        }
        var node = host.getGridNode(null);
        return node != null ? node.getGrid() : null;
    }

    private static ServerPlayer firstPlayer(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static void setBlock(TestServerContext server, BlockPos pos, String block) {
        server.runCommand("setblock %d %d %d %s".formatted(pos.getX(), pos.getY(), pos.getZ(), block));
    }

    private static void fill(TestServerContext server, BlockPos from, BlockPos to, String block) {
        server.runCommand("fill %d %d %d %d %d %d %s".formatted(from.getX(), from.getY(), from.getZ(), to.getX(),
                to.getY(), to.getZ(), block));
    }

    private static void worldScreenshot(ClientGameTestContext context, BlockPos lookAt, String name) {
        if (lookAt != null) {
            context.getInput().lookAt(lookAt.offset(0, 0, 4));
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

    private static void waitForServer(ClientGameTestContext context, TestServerContext server,
            Predicate<MinecraftServer> condition, int maxTicks, String what) {
        if (!waitFor(context, server, condition, maxTicks)) {
            throw new AssertionError("Timed out waiting for " + what);
        }
    }
}
