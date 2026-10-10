package com.glodblock.github.extendedae.gametest;

import appeng.api.config.Actionable;
import appeng.api.ids.AEComponents;
import appeng.api.parts.PartHelper;
import appeng.core.AppEng;
import appeng.core.definitions.AEBlocks;
import appeng.items.parts.PartItem;
import com.glodblock.github.extendedae.ExtendedAE;
import com.glodblock.github.extendedae.common.EAESingletons;
import com.glodblock.github.extendedae.common.tileentities.TileCircuitCutter;
import com.glodblock.github.extendedae.xmod.jei.recipe.CircuitCutterCategory;
import com.glodblock.github.extendedae.xmod.jei.recipe.CrystalAssemblerCategory;
import com.glodblock.github.extendedae.xmod.jei.recipe.CrystalFixerCategory;
import com.glodblock.github.extendedae.xmod.wt.GuiWirelessExPAT;
import guideme.Guides;
import guideme.PageAnchor;
import guideme.compiler.ParsedGuidePage;
import guideme.internal.screen.GuideScreen;
import mezz.jei.api.recipe.types.IRecipeType;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import appeng.core.registries.DeferredItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds ExtendedAE's machines and parts in a real client, opens their screens and takes screenshots. The screenshots
 * end up in {@code extendedae/build/run/clientGameTest/screenshots}.
 */
public class EAEClientGameTest implements FabricClientGameTest {
    private static final Logger LOG = LoggerFactory.getLogger(EAEClientGameTest.class);

    // Machines that have a screen
    private static final List<String> MACHINES = List.of(
            "ex_pattern_provider", "ex_interface", "oversize_interface", "wireless_connector", "wireless_hub",
            "ingredient_buffer", "ex_drive", "ex_molecular_assembler", "caner", "ex_io_port", "crystal_assembler",
            "circuit_cutter");

    // Blocks without a screen, shown together in the world
    private static final List<String> DECORATIVE = List.of(
            "ex_charger", "crystal_fixer", "fishbig", "mddyue", "entro_block", "machine_frame", "silicon_block",
            "entro_cluster", "entro_cluster_large", "fully_entroized_fluix_budding", "assembler_matrix_frame",
            "assembler_matrix_wall", "assembler_matrix_glass", "assembler_matrix_pattern", "assembler_matrix_crafter",
            "assembler_matrix_speed");

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
            server.runCommand("time set noon");
            server.runCommand("gamerule advance_time false");
            server.runCommand("gamerule send_command_feedback false");
            context.waitTicks(40);

            var origin = server.computeOnServer(s -> firstPlayer(s).blockPosition());
            var target = origin.offset(0, 0, 3);
            var failures = new ArrayList<String>();

            // Machines, powered by a creative energy cell below them
            setBlock(server, target.below(), "ae2:creative_energy_cell");
            for (var machine : MACHINES) {
                setBlock(server, target, "extendedae:" + machine);
                context.waitTicks(5);
                if (!openAndScreenshot(context, target, "eae-" + machine)) {
                    failures.add(machine);
                }
            }
            setBlock(server, target, "minecraft:air");

            // Parts on a cable
            for (var part : partItems()) {
                server.runOnServer(s -> PartHelper.setPart(s.overworld(), target, Direction.NORTH, null, part.get()));
                context.waitTicks(5);
                var name = part.getId().getPath();
                // Small parts like the level emitter are missed when looking at the block's center
                lookAtPoint(context, target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.1);
                context.getInput().pressKey(options -> options.keyUse);
                if (!screenshotOpenedScreen(context, "eae-part-" + name)) {
                    failures.add(name);
                }
                setBlock(server, target, "minecraft:air");
            }
            setBlock(server, target.below(), "minecraft:air");

            // Items with a screen
            for (var item : List.of(EAESingletons.PATTERN_MODIFIER, EAESingletons.VOID_CELL, EAESingletons.CONFIG_MODIFIER)) {
                server.runOnServer(s -> firstPlayer(s).setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item.get())));
                context.waitTicks(5);
                context.getInput().lookAt(origin.above(30));
                context.getInput().pressKey(options -> options.keyUse);
                if (!screenshotOpenedScreen(context, "eae-item-" + item.getId().getPath())) {
                    failures.add(item.getId().getPath());
                }
            }
            server.runCommand("clear @a");

            // All other blocks in a row
            for (int i = 0; i < DECORATIVE.size(); i++) {
                setBlock(server, origin.offset(i - DECORATIVE.size() / 2, 0, 5), "extendedae:" + DECORATIVE.get(i));
            }
            context.getInput().lookAt(origin.offset(0, 0, 5));
            context.waitTicks(20);
            worldScreenshot(context, "eae-blocks");
            fill(server, origin.offset(-12, 0, 5), origin.offset(12, 1, 5), "minecraft:air");

            testCircuitCutter(context, server, origin.offset(3, 0, 3));
            testWirelessTerminal(context, server, origin, failures);

            // JEI shows ExtendedAE's recipe categories
            var runtime = TestJeiPlugin.runtime;
            if (runtime == null) {
                throw new AssertionError("JEI runtime is not available");
            }
            for (IRecipeType<?> type : List.of(CircuitCutterCategory.RECIPE_TYPE, CrystalAssemblerCategory.RECIPE_TYPE,
                    CrystalFixerCategory.RECIPE_TYPE)) {
                var count = context.computeOnClient(mc -> runtime.getRecipeManager().createRecipeLookup(type).get().count());
                LOG.info("JEI recipes for {}: {}", type.getUid(), count);
                if (count == 0) {
                    failures.add("jei " + type.getUid());
                }
                context.runOnClient(mc -> runtime.getRecipesGui().showTypes(List.of(type)));
                context.waitTicks(20);
                context.takeScreenshot("eae-jei-" + type.getUid().getPath());
            }
            context.setScreen(() -> null);

            // ExtendedAE's pages in AE2's guidebook
            var guide = Guides.getById(AppEng.makeId("guide"));
            var pageIds = guide.getPages().stream()
                    .map(ParsedGuidePage::getId)
                    .filter(id -> id.getNamespace().equals(ExtendedAE.MODID))
                    .sorted()
                    .toList();
            LOG.info("ExtendedAE guide pages: {}", pageIds.size());
            if (pageIds.isEmpty()) {
                failures.add("guide pages");
            }
            for (var pageId : pageIds) {
                context.runOnClient(mc -> mc.gui.setScreen(GuideScreen.openNew(guide, PageAnchor.page(pageId))));
                context.waitTicks(3);
                if (pageId.getPath().startsWith("epp_intro/")) {
                    context.takeScreenshot("eae-guide-" + pageId.getPath().replace('/', '-').replace(".md", ""));
                }
            }
            context.setScreen(() -> null);

            if (!failures.isEmpty()) {
                throw new AssertionError("Failed: " + failures);
            }
        }
    }

    private static List<DeferredItem<? extends PartItem<?>>> partItems() {
        return List.of(
                EAESingletons.EX_PATTERN_PROVIDER_PART, EAESingletons.EX_INTERFACE_PART,
                EAESingletons.OVERSIZE_INTERFACE_PART, EAESingletons.EX_EXPORT_BUS, EAESingletons.EX_IMPORT_BUS,
                EAESingletons.EX_PATTERN_TERMINAL, EAESingletons.TAG_STORAGE_BUS, EAESingletons.TAG_EXPORT_BUS,
                EAESingletons.THRESHOLD_LEVEL_EMITTER, EAESingletons.MOD_STORAGE_BUS, EAESingletons.MOD_EXPORT_BUS,
                EAESingletons.ACTIVE_FORMATION_PLANE, EAESingletons.PRECISE_EXPORT_BUS,
                EAESingletons.PRECISE_STORAGE_BUS, EAESingletons.THRESHOLD_EXPORT_BUS,
                EAESingletons.SMART_ANNIHILATION_PLANE);
    }

    /**
     * Inserts quartz blocks into a circuit cutter through Fabric's transfer API, like a pipe would, and waits for the
     * printed processors.
     */
    private static void testCircuitCutter(ClientGameTestContext context, TestServerContext server, BlockPos pos) {
        setBlock(server, pos.below(), "ae2:creative_energy_cell");
        setBlock(server, pos, "extendedae:circuit_cutter");
        context.waitTicks(20);
        server.runOnServer(s -> {
            var storage = ItemStorage.SIDED.find(s.overworld(), pos, Direction.UP);
            if (storage == null) {
                throw new AssertionError("The circuit cutter exposes no item storage");
            }
            try (var tx = Transaction.openOuter()) {
                var inserted = storage.insert(ItemVariant.of(AEBlocks.QUARTZ_BLOCK.asItem()), 4, tx);
                tx.commit();
                LOG.info("Inserted {} quartz blocks into the circuit cutter", inserted);
            }
        });
        waitForServer(context, server, s -> s.overworld().getBlockEntity(pos) instanceof TileCircuitCutter cutter
                && !cutter.getOutput().getStackInSlot(0).isEmpty(), 1200, "the circuit cutter's output");
        var output = server.computeOnServer(s -> ((TileCircuitCutter) s.overworld().getBlockEntity(pos))
                .getOutput().getStackInSlot(0).toString());
        LOG.info("Circuit cutter output: {}", output);
        context.getInput().lookAt(pos);
        context.waitTicks(10);
        worldScreenshot(context, "eae-circuit-cutter-working");
        openAndScreenshot(context, pos, "eae-circuit-cutter-output");
        setBlock(server, pos, "minecraft:air");
        setBlock(server, pos.below(), "minecraft:air");
    }

    /**
     * The wireless extended pattern access terminal comes from ae2wtlib's terminal registration.
     */
    private static void testWirelessTerminal(ClientGameTestContext context, TestServerContext server, BlockPos origin,
            List<String> failures) {
        var accessPointPos = origin.offset(-3, 0, 0);
        setBlock(server, accessPointPos.below(), "ae2:creative_energy_cell");
        setBlock(server, accessPointPos, "ae2:wireless_access_point[facing=up]");
        setBlock(server, accessPointPos.below().west(), "extendedae:ex_pattern_provider");
        context.waitTicks(20);
        server.runOnServer(s -> {
            var player = firstPlayer(s);
            var terminal = EAESingletons.WIRELESS_EX_PAT.get();
            var stack = new ItemStack(terminal);
            terminal.injectAEPower(stack, terminal.getAEMaxPower(stack), Actionable.MODULATE);
            stack.set(AEComponents.WIRELESS_LINK_TARGET, GlobalPos.of(player.level().dimension(), accessPointPos));
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        });
        context.waitTicks(5);
        context.getInput().lookAt(origin.above(30));
        context.getInput().pressKey(options -> options.keyUse);
        screenshotOpenedScreen(context, "eae-wireless-ex-pattern-access-terminal");
        if (!lastScreen.equals(GuiWirelessExPAT.class.getName())) {
            failures.add("wireless_ex_pat (" + lastScreen + ")");
        }
        server.runCommand("clear @a");
        setBlock(server, accessPointPos, "minecraft:air");
        setBlock(server, accessPointPos.below(), "minecraft:air");
        setBlock(server, accessPointPos.below().west(), "minecraft:air");
    }

    private static String lastScreen = "";

    private static void worldScreenshot(ClientGameTestContext context, String name) {
        context.runOnClient(mc -> {
            if (!mc.gui.hud.isHidden()) {
                mc.gui.hud.toggle();
            }
        });
        context.waitTicks(2);
        context.takeScreenshot(name);
        context.runOnClient(mc -> {
            if (mc.gui.hud.isHidden()) {
                mc.gui.hud.toggle();
            }
        });
    }

    private static void waitForServer(ClientGameTestContext context, TestServerContext server,
            java.util.function.Predicate<MinecraftServer> condition, int maxTicks, String what) {
        for (var i = 0; i < maxTicks; i += 10) {
            if (server.computeOnServer(condition::test)) {
                return;
            }
            context.waitTicks(10);
        }
        throw new AssertionError("Timed out waiting for " + what);
    }

    private static void setBlock(TestServerContext server, BlockPos pos, String block) {
        server.runCommand("setblock %d %d %d %s".formatted(pos.getX(), pos.getY(), pos.getZ(), block));
    }

    private static void fill(TestServerContext server, BlockPos from, BlockPos to, String block) {
        server.runCommand("fill %d %d %d %d %d %d %s".formatted(from.getX(), from.getY(), from.getZ(), to.getX(),
                to.getY(), to.getZ(), block));
    }

    private static ServerPlayer firstPlayer(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static void lookAtPoint(ClientGameTestContext context, double x, double y, double z) {
        var angles = context.computeOnClient(mc -> {
            var eye = mc.player.getEyePosition();
            double dx = x - eye.x, dy = y - eye.y, dz = z - eye.z;
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
            return new float[] {yaw, pitch};
        });
        context.getInput().lookAt(angles[0], angles[1]);
    }

    private static boolean openAndScreenshot(ClientGameTestContext context, BlockPos pos, String name) {
        context.getInput().lookAt(pos);
        context.getInput().pressKey(options -> options.keyUse);
        return screenshotOpenedScreen(context, name);
    }

    /**
     * Waits for a screen to open, takes a screenshot and closes it again.
     */
    private static boolean screenshotOpenedScreen(ClientGameTestContext context, String name) {
        boolean opened;
        try {
            context.waitFor(mc -> mc.gui.screen() != null, 60);
            context.waitTicks(10);
            lastScreen = context.computeOnClient(mc -> mc.gui.screen().getClass().getName());
            LOG.info("Opened screen for {}: {}", name, lastScreen);
            opened = true;
        } catch (AssertionError e) {
            lastScreen = "";
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
}
