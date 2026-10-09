package appeng.gametest;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;

import guideme.Guides;
import guideme.PageAnchor;
import guideme.compiler.ParsedGuidePage;
import guideme.internal.GuideMEClient;
import guideme.internal.screen.GuideScreen;
import guideme.internal.screen.GuideSearchScreen;

import appeng.api.config.Actionable;
import appeng.api.ids.AEComponents;
import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.api.parts.IPartHost;
import appeng.api.parts.PartHelper;
import appeng.blockentity.networking.EnergyCellBlockEntity;
import appeng.client.gui.config.AEConfigScreen;
import appeng.core.AppEng;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import appeng.core.definitions.ItemDefinition;
import appeng.items.parts.PartItem;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.MenuOpener;
import appeng.menu.implementations.PriorityMenu;
import appeng.menu.locator.MenuLocators;

/**
 * Opens the user interfaces of AE2's machines and terminals in a real client and takes screenshots of them. The
 * screenshots end up in {@code build/run/clientGameTest/screenshots}.
 */
public class AE2ClientGameTest implements FabricClientGameTest {
    private static final Logger LOG = LoggerFactory.getLogger(AE2ClientGameTest.class);

    private static final List<String> MACHINES = List.of(
            "inscriber",
            "me_chest",
            "interface",
            "pattern_provider",
            "io_port",
            "drive",
            "sky_stone_chest",
            "condenser",
            "molecular_assembler",
            "vibration_chamber",
            "spatial_io_port",
            "cell_workbench",
            "wireless_access_point",
            "controller",
            "spatial_anchor",
            // A single crafting storage forms a crafting CPU
            "1k_crafting_storage");

    private static final List<ItemDefinition<? extends PartItem<?>>> PARTS = List.of(
            AEParts.STORAGE_BUS,
            AEParts.IMPORT_BUS,
            AEParts.EXPORT_BUS,
            AEParts.LEVEL_EMITTER,
            AEParts.ENERGY_LEVEL_EMITTER,
            AEParts.FORMATION_PLANE,
            AEParts.INTERFACE,
            AEParts.PATTERN_PROVIDER);

    private static final List<ItemDefinition<?>> ITEMS = List.of(
            AEItems.NETWORK_TOOL,
            AEItems.CERTUS_QUARTZ_KNIFE,
            AEItems.PORTABLE_ITEM_CELL1K,
            AEItems.PORTABLE_FLUID_CELL1K,
            AEItems.WIRELESS_TERMINAL,
            AEItems.WIRELESS_CRAFTING_TERMINAL);

    private static final List<String> TERMINALS = List.of(
            "terminal",
            "crafting_terminal",
            "pattern_encoding_terminal",
            "pattern_access_terminal");

    @Override
    public void runTest(ClientGameTestContext context) {
        // Config screen (opened through Mod Menu in normal play)
        context.setScreen(() -> new AEConfigScreen(null));
        context.waitTicks(10);
        context.takeScreenshot("ae2-config-screen");
        context.setScreen(() -> null);

        try (var world = context.worldBuilder().adjustSettings(settings -> {
            settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            // AE2's test world command requires a superflat world
            settings.getNormalPresetList().stream()
                    .filter(entry -> entry.preset() != null && entry.preset().is(WorldPresets.FLAT))
                    .findFirst()
                    .ifPresent(settings::setWorldType);
        }).create()) {
            var server = world.getServer();
            server.runCommand("time set noon");
            context.waitTicks(40);

            var origin = server.computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().blockPosition());

            // Machines: place each one in front of the player and open it
            var target = origin.offset(0, 0, 3);
            // Crafting CPUs only open when they are powered
            setBlock(server, target.below(), "ae2:creative_energy_cell");
            for (var machine : MACHINES) {
                setBlock(server, target, "ae2:" + machine);
                context.waitTicks(5);
                openAndScreenshot(context, target, "ae2-" + machine);
                setBlock(server, target, "minecraft:air");
            }

            // Parts: attach each one to a cable bus in front of the player, facing the player, and open it
            // (at eye level, so the player looks straight at small parts like level emitters)
            var partPos = target.above();
            for (var part : PARTS) {
                server.runOnServer(s -> PartHelper.setPart(s.overworld(), partPos, Direction.NORTH, null, part.get()));
                context.waitTicks(5);
                openAndScreenshot(context, partPos, "ae2-part-" + part.id().getPath());
                if (part == AEParts.STORAGE_BUS) {
                    // Opened through a button in the storage bus screen
                    server.runOnServer(s -> {
                        var bus = PartHelper.getPart(AEParts.STORAGE_BUS.get(), s.overworld(), partPos,
                                Direction.NORTH);
                        MenuOpener.open(PriorityMenu.TYPE, firstPlayer(s), MenuLocators.forPart(bus));
                    });
                    screenshotOpenedScreen(context, "ae2-priority");
                }
                setBlock(server, partPos, "minecraft:air");
            }

            // Quantum network bridge: a ring standing upright with the link chamber in front of the player
            for (var x = -1; x <= 1; x++) {
                for (var y = -1; y <= 1; y++) {
                    setBlock(server, target.offset(x, y, 0),
                            x == 0 && y == 0 ? "ae2:quantum_link" : "ae2:quantum_ring");
                }
            }
            context.waitTicks(20);
            openAndScreenshot(context, target, "ae2-quantum_link");
            server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(target.getX() - 1,
                    target.getY() - 1, target.getZ(), target.getX() + 1, target.getY() + 1, target.getZ()));

            // Items that open a screen when used in the air. The wireless terminals are linked to an access point.
            var accessPointPos = origin.offset(-3, 0, 0);
            setBlock(server, accessPointPos.below(), "ae2:creative_energy_cell");
            setBlock(server, accessPointPos, "ae2:wireless_access_point[facing=up]");
            context.waitTicks(20);
            for (var item : ITEMS) {
                server.runOnServer(s -> {
                    var player = firstPlayer(s);
                    var stack = item.stack();
                    if (stack.getItem() instanceof IAEItemPowerStorage powered) {
                        powered.injectAEPower(stack, powered.getAEMaxPower(stack), Actionable.MODULATE);
                    }
                    if (stack.getItem() instanceof WirelessTerminalItem) {
                        stack.set(AEComponents.WIRELESS_LINK_TARGET,
                                GlobalPos.of(player.level().dimension(), accessPointPos));
                    }
                    player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                });
                context.waitTicks(5);
                openAndScreenshot(context, origin.above(30), "ae2-item-" + item.id().getPath());
            }
            server.runOnServer(s -> firstPlayer(s).setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));

            // Guidebook: every page, with screenshots of a few pages with 3D scenes
            var guide = Guides.getById(AppEng.makeId("guide"));
            var pageIds = guide.getPages().stream().map(ParsedGuidePage::getId).sorted().toList();
            LOG.info("Opening {} guide pages", pageIds.size());
            for (var pageId : pageIds) {
                context.runOnClient(mc -> mc.gui.setScreen(GuideScreen.openNew(guide, PageAnchor.page(pageId))));
                context.waitTicks(3);
                if (!context.computeOnClient(mc -> mc.gui.screen() instanceof GuideScreen)) {
                    LOG.warn("Guide page {} did not stay open", pageId);
                }
                context.takeScreenshot("ae2-guide-" + pageId.getPath().replace('/', '-').replace(".md", ""));
            }

            // Guidebook search (the index is built in the background)
            context.runOnClient(mc -> mc.gui.setScreen(GuideSearchScreen.open(guide, null)));
            context.waitFor(mc -> !GuideMEClient.instance().getSearch().searchGuide("controller", guide).isEmpty(),
                    1200);
            context.runOnClient(mc -> mc.gui.setScreen(GuideSearchScreen.open(guide, "controller")));
            context.waitTicks(5);
            context.takeScreenshot("ae2-guide-search");
            context.setScreen(() -> null);
            context.waitTicks(5);

            // Tech Reborn (only in the compatibility run): its creative solar panel powers an AE2 network
            if (FabricLoader.getInstance().isModLoaded("techreborn")) {
                var panelPos = origin.offset(5, 0, 0);
                setBlock(server, panelPos, "techreborn:creative_solar_panel");
                setBlock(server, panelPos.east(), "ae2:energy_acceptor");
                setBlock(server, panelPos.east(2), "ae2:energy_cell");
                context.waitTicks(100);
                var stored = server.computeOnServer(s -> s.overworld()
                        .getBlockEntity(panelPos.east(2)) instanceof EnergyCellBlockEntity cell
                                ? cell.getAECurrentPower()
                                : -1);
                LOG.info("AE stored in the energy cell powered by Tech Reborn: {}", stored);
            }

            // Jade: look at a drive without opening it
            setBlock(server, target, "ae2:drive");
            context.getInput().lookAt(target);
            context.waitTicks(20);
            context.takeScreenshot("ae2-jade-drive");
            setBlock(server, target, "minecraft:air");

            // JEI: show the recipes for some of AE2's items
            for (var item : List.of(AEItems.LOGIC_PROCESSOR, AEItems.CERTUS_QUARTZ_CRYSTAL)) {
                showJeiRecipes(context, item.stack());
                context.takeScreenshot("ae2-jei-" + item.id().getPath());
                context.setScreen(() -> null);
                context.waitTicks(5);
            }
            showJeiRecipes(context, AEBlocks.CONTROLLER.stack());
            context.takeScreenshot("ae2-jei-controller");
            context.setScreen(() -> null);

            // Terminals: build AE2's terminal test plot and open the terminals of its first line, which face north
            server.runCommand("execute as @p at @p run ae2 setuptestworld all_terminals");
            context.waitTicks(100);
            context.takeScreenshot("ae2-all-terminals-overview");
            for (var terminal : TERMINALS) {
                var terminalPos = server.computeOnServer(s -> findNorthFacingPart(s, terminal));
                if (terminalPos == null) {
                    LOG.warn("Could not find {} in the test plot", terminal);
                    continue;
                }
                server.runCommand("tp @p %d.5 %d %d.5".formatted(terminalPos.getX(), terminalPos.getY(),
                        terminalPos.getZ() - 2));
                context.waitTicks(10);
                openAndScreenshot(context, terminalPos, "ae2-" + terminal);
            }
        }

        // World generation: meteorites generate in a normal world
        try (var world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(settings -> {
            settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            settings.setSeed("ae2");
            settings.setGenerateStructures(true);
        }).create()) {
            var server = world.getServer();
            var meteorite = server.computeOnServer(s -> {
                var level = s.overworld();
                var structure = s.registryAccess().lookupOrThrow(Registries.STRUCTURE)
                        .getOrThrow(ResourceKey.create(Registries.STRUCTURE, AppEng.makeId("meteorite")));
                var found = level.getChunkSource().getGenerator().findNearestMapStructure(level,
                        HolderSet.direct(structure), BlockPos.ZERO, 100, false);
                return found == null ? null : found.getFirst();
            });
            LOG.info("Nearest meteorite: {}", meteorite);
            if (meteorite != null) {
                server.runCommand("tp @p %d 200 %d".formatted(meteorite.getX(), meteorite.getZ()));
                context.waitTicks(200);
                var skyStone = server.computeOnServer(s -> {
                    var level = s.overworld();
                    var count = 0;
                    for (var pos : BlockPos.betweenClosed(meteorite.offset(-16, 0, -16).atY(level.getMinY()),
                            meteorite.offset(16, 0, 16).atY(level.getMaxY()))) {
                        if (level.getBlockState(pos).is(AEBlocks.SKY_STONE_BLOCK.block())) {
                            count++;
                        }
                    }
                    return count;
                });
                LOG.info("Sky stone blocks around the meteorite: {}", skyStone);
                context.getInput().lookAt(meteorite.atY(60));
                context.waitTicks(20);
                context.takeScreenshot("ae2-meteorite");
            }
        }
    }

    /**
     * Finds a cable bus near the player that has the given part attached to its north side.
     */
    @Nullable
    private static BlockPos findNorthFacingPart(MinecraftServer server, String partId) {
        var player = server.getPlayerList().getPlayers().getFirst();
        var level = player.level();
        var center = player.blockPosition();
        for (var pos : BlockPos.betweenClosed(center.offset(-32, -8, -32), center.offset(32, 24, 32))) {
            if (level.getBlockEntity(pos) instanceof IPartHost host) {
                var part = host.getPart(Direction.NORTH);
                if (part != null && BuiltInRegistries.ITEM.getKey(part.getPartItem().asItem()).getPath()
                        .equals(partId)) {
                    return pos.immutable();
                }
            }
        }
        return null;
    }

    private static void showJeiRecipes(ClientGameTestContext context, ItemStack stack) {
        var runtime = TestJeiPlugin.runtime;
        if (runtime == null) {
            LOG.warn("JEI runtime is not available");
            return;
        }
        context.runOnClient(mc -> runtime.getRecipesGui().show(runtime.getJeiHelpers().getFocusFactory()
                .createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack)));
        context.waitTicks(20);
    }

    private static void setBlock(TestServerContext server, BlockPos pos, String block) {
        server.runCommand("setblock %d %d %d %s".formatted(pos.getX(), pos.getY(), pos.getZ(), block));
    }

    private static ServerPlayer firstPlayer(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static void openAndScreenshot(ClientGameTestContext context, BlockPos pos, String name) {
        context.getInput().lookAt(pos);
        context.getInput().pressKey(options -> options.keyUse);
        screenshotOpenedScreen(context, name);
    }

    /**
     * Waits for a screen to open, takes a screenshot and closes it again.
     */
    private static void screenshotOpenedScreen(ClientGameTestContext context, String name) {
        try {
            context.waitFor(mc -> mc.gui.screen() != null, 60);
            context.waitTicks(10);
            LOG.info("Opened screen for {}: {}", name,
                    context.computeOnClient(mc -> mc.gui.screen().getClass().getName()));
        } catch (AssertionError e) {
            LOG.warn("No screen opened for {}", name);
        }
        context.takeScreenshot(name);
        // Close the menu like the player would, so the server doesn't close it later (which would also close
        // whatever screen is open by then)
        context.runOnClient(mc -> {
            if (mc.player != null && mc.player.containerMenu != mc.player.inventoryMenu) {
                mc.player.closeContainer();
            } else {
                mc.gui.setScreen(null);
            }
        });
        context.waitTicks(5);
    }
}
