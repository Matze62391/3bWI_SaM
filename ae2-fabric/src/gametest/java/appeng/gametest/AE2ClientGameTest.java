package appeng.gametest;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;

import guideme.Guides;
import guideme.PageAnchor;
import guideme.internal.screen.GuideScreen;

import appeng.api.parts.IPartHost;
import appeng.client.gui.config.AEConfigScreen;
import appeng.core.AppEng;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;

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
            "wireless_access_point");

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
            for (var machine : MACHINES) {
                setBlock(server, target, "ae2:" + machine);
                context.waitTicks(5);
                openAndScreenshot(context, target, "ae2-" + machine);
                setBlock(server, target, "minecraft:air");
            }

            // Guidebook: the start page and pages with 3D scenes of blocks and cables
            for (var page : List.of("index.md", "items-blocks-machines/controller.md", "items-blocks-machines/cables.md",
                    "items-blocks-machines/facades.md", "getting-started.md", "index.md")) {
                var guide = Guides.getById(AppEng.makeId("guide"));
                context.runOnClient(mc -> mc.gui.setScreen(GuideScreen.openNew(guide,
                        PageAnchor.page(AppEng.makeId(page)))));
                String screenNow = context.computeOnClient(mc -> String.valueOf(mc.gui.screen()));
                LOG.info("Screen after opening guide page {}: {}", page, screenNow);
                context.waitTicks(40);
                String screenLater = context.computeOnClient(mc -> String.valueOf(mc.gui.screen()));
                LOG.info("Screen 40 ticks later: {}", screenLater);
                context.takeScreenshot("ae2-guide-" + page.replace('/', '-').replace(".md", ""));
                context.setScreen(() -> null);
                context.waitTicks(5);
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

    private static void openAndScreenshot(ClientGameTestContext context, BlockPos pos, String name) {
        context.getInput().lookAt(pos);
        context.getInput().pressKey(options -> options.keyUse);
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
