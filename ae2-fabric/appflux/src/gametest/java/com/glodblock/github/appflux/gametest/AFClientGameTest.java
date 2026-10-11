package com.glodblock.github.appflux.gametest;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.PartHelper;
import appeng.blockentity.misc.InterfaceBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.AppEng;
import com.glodblock.github.appflux.AppFlux;
import com.glodblock.github.appflux.common.AFSingletons;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import com.glodblock.github.appflux.common.tileentities.TileFluxAccessor;
import guideme.Guides;
import guideme.PageAnchor;
import guideme.compiler.ParsedGuidePage;
import guideme.internal.screen.GuideScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.reborn.energy.api.EnergyStorage;

import java.util.ArrayList;

/**
 * Stores energy (Team Reborn Energy on Fabric) in an ME network through Applied Flux and opens its screens. The
 * screenshots end up in {@code appflux/build/run/clientGameTest/screenshots}.
 */
public class AFClientGameTest implements FabricClientGameTest {
    private static final Logger LOG = LoggerFactory.getLogger(AFClientGameTest.class);

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
            var failures = new ArrayList<String>();

            // A small network: energy cell, drive with a 1k flux cell, flux accessor, interface
            var base = origin.offset(-2, 0, 4);
            var drive = base.east();
            var accessor = drive.east();
            var iface = accessor.east();
            setBlock(server, base, "ae2:creative_energy_cell");
            setBlock(server, drive, "ae2:drive");
            setBlock(server, accessor, "appflux:flux_accessor");
            setBlock(server, iface, "ae2:interface");
            server.runOnServer(s -> {
                var be = (DriveBlockEntity) s.overworld().getBlockEntity(drive);
                be.getInternalInventory().insertItem(0, new ItemStack(AFSingletons.FE_CELL_1k.get()), false);
            });
            waitForServer(context, server, s -> s.overworld().getBlockEntity(accessor) instanceof TileFluxAccessor te
                    && te.getMainNode().isActive(), 200, "the flux accessor to be online");

            // Energy pushed into the flux accessor (by a generator, cable, ...) ends up in the flux cell
            var inserted = server.computeOnServer(s -> {
                var storage = EnergyStorage.SIDED.find(s.overworld(), accessor, Direction.UP);
                if (storage == null) {
                    return -1L;
                }
                try (var tx = Transaction.openOuter()) {
                    var amount = storage.insert(100_000, tx);
                    tx.commit();
                    return amount;
                }
            });
            var stored = storedInNetwork(server, accessor);
            LOG.info("Inserted {} E through the flux accessor, the network stores {}", inserted, stored);
            if (inserted != 100_000 || stored != 100_000) {
                failures.add("insert through flux accessor (" + inserted + "/" + stored + ")");
            }

            // ... and can be taken out again
            var extracted = server.computeOnServer(s -> {
                var storage = EnergyStorage.SIDED.find(s.overworld(), accessor, Direction.UP);
                try (var tx = Transaction.openOuter()) {
                    var amount = storage.extract(40_000, tx);
                    tx.commit();
                    return amount;
                }
            });
            stored = storedInNetwork(server, accessor);
            LOG.info("Extracted {} E through the flux accessor, the network stores {}", extracted, stored);
            if (extracted != 40_000 || stored != 60_000) {
                failures.add("extract through flux accessor (" + extracted + "/" + stored + ")");
            }

            // An aborted transaction changes nothing
            server.runOnServer(s -> {
                var storage = EnergyStorage.SIDED.find(s.overworld(), accessor, Direction.UP);
                try (var tx = Transaction.openOuter()) {
                    storage.extract(10_000, tx);
                }
            });
            if (storedInNetwork(server, accessor) != 60_000) {
                failures.add("aborted transaction changed the network");
            }

            // The interface only offers its energy with an induction card
            var withoutCard = server.computeOnServer(s -> EnergyStorage.SIDED.find(s.overworld(), iface, Direction.UP) != null);
            server.runOnServer(s -> ((InterfaceBlockEntity) s.overworld().getBlockEntity(iface)).getUpgrades()
                    .addItems(new ItemStack(AFSingletons.INDUCTION_CARD.get())));
            context.waitTicks(5);
            var withCard = server.computeOnServer(s -> EnergyStorage.SIDED.find(s.overworld(), iface, Direction.UP) != null);
            LOG.info("Interface energy storage without card: {}, with card: {}", withoutCard, withCard);
            if (withoutCard || !withCard) {
                failures.add("induction card on interface (" + withoutCard + "/" + withCard + ")");
            }

            // Flux cells are batteries for other mods
            var cellCharge = server.computeOnServer(s -> {
                var stack = new ItemStack(AFSingletons.FE_CELL_1k.get());
                var storage = EnergyStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack));
                if (storage == null) {
                    return -1L;
                }
                try (var tx = Transaction.openOuter()) {
                    var amount = storage.insert(5_000, tx);
                    tx.commit();
                    return amount;
                }
            });
            LOG.info("Charged a flux cell item with {} E", cellCharge);
            if (cellCharge != 5_000) {
                failures.add("flux cell item energy (" + cellCharge + ")");
            }

            // The creative tab shows each portable cell empty and charged (the charged one must differ)
            var sameWhenCharged = server.computeOnServer(s -> {
                var cell = AFSingletons.FE_PORTABLE_CELL_1k.get();
                var plain = new ItemStack(cell);
                var charged = new ItemStack(cell);
                cell.injectAEPower(charged, cell.getAEMaxPower(charged), Actionable.MODULATE);
                LOG.info("Portable cell max power {}, plain {}, charged {}", cell.getAEMaxPower(plain),
                        plain.getComponentsPatch(), charged.getComponentsPatch());
                return ItemStack.isSameItemSameComponents(plain, charged);
            });
            if (sameWhenCharged) {
                failures.add("charged portable cell equals the empty one");
            }
            var duplicates = context.computeOnClient(mc -> {
                try {
                    var tab = net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.getValue(AppFlux.id("tab_main"));
                    var field = net.minecraft.world.item.CreativeModeTab.class.getDeclaredField("displayItemsGenerator");
                    field.setAccessible(true);
                    var generator = (net.minecraft.world.item.CreativeModeTab.DisplayItemsGenerator) field.get(tab);
                    var stacks = new ArrayList<ItemStack>();
                    generator.accept(new net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters(
                            mc.level.enabledFeatures(), true, mc.level.registryAccess()),
                            (stack, visibility) -> stacks.add(stack));
                    var dups = new ArrayList<String>();
                    for (int i = 0; i < stacks.size(); i++) {
                        for (int j = 0; j < i; j++) {
                            if (ItemStack.isSameItemSameComponents(stacks.get(i), stacks.get(j))) {
                                dups.add(stacks.get(i).getItem() + " " + stacks.get(i).getComponentsPatch());
                            }
                        }
                    }
                    return dups;
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            });
            if (!duplicates.isEmpty()) {
                failures.add("duplicate creative tab items " + duplicates);
            }

            // With ExtendedAE and Advanced AE installed (like in the pack), their pattern providers and interfaces take
            // induction cards, and their Applied Flux recipes are loaded
            var integrations = new java.util.LinkedHashMap<String, String>();
            if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("extendedae")) {
                integrations.put("extendedae:ex_interface", "extendedae:assembler/energy_processor");
                integrations.put("extendedae:ex_pattern_provider", "extendedae:cutter/energy_processor");
            }
            if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("advanced_ae")) {
                integrations.put("advanced_ae:adv_pattern_provider", "advanced_ae:redstonecrystal");
            }
            for (var entry : integrations.entrySet()) {
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                        net.minecraft.resources.Identifier.parse(entry.getKey()));
                var slots = appeng.api.upgrades.Upgrades.getMaxInstallable(AFSingletons.INDUCTION_CARD.get(), item);
                var recipeKey = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                        net.minecraft.resources.Identifier.parse(entry.getValue()));
                var recipe = server.computeOnServer(s -> s.getRecipeManager().byKey(recipeKey).isPresent());
                LOG.info("Induction cards for {}: {}, recipe {} loaded: {}", entry.getKey(), slots, entry.getValue(), recipe);
                if (slots <= 0 || !recipe) {
                    failures.add("integration " + entry.getKey());
                }
            }

            // Screens
            context.getInput().lookAt(accessor);
            context.waitTicks(10);
            worldScreenshot(context, "af-network");
            if (!openAndScreenshot(context, accessor, "af-flux-accessor")) {
                failures.add("flux accessor screen");
            }
            if (!openAndScreenshot(context, iface, "af-interface-with-induction-card")) {
                failures.add("interface screen");
            }
            var provider = origin.offset(3, 0, 3);
            setBlock(server, provider, "ae2:pattern_provider");
            context.waitTicks(5);
            if (!openAndScreenshot(context, provider, "af-pattern-provider-upgrades")) {
                failures.add("pattern provider screen");
            }
            setBlock(server, provider, "minecraft:air");
            var part = origin.offset(0, 0, 2);
            server.runOnServer(s -> PartHelper.setPart(s.overworld(), part, Direction.NORTH, null, AFSingletons.PART_FLUX_ACCESSOR.get()));
            context.waitTicks(5);
            if (!openAndScreenshot(context, part, "af-flux-accessor-part")) {
                failures.add("flux accessor part screen");
            }
            setBlock(server, part, "minecraft:air");

            // A portable flux cell
            server.runOnServer(s -> {
                var cell = AFSingletons.FE_PORTABLE_CELL_1k.get();
                var stack = new ItemStack(cell);
                cell.injectAEPower(stack, cell.getAEMaxPower(stack), Actionable.MODULATE);
                firstPlayer(s).setItemInHand(InteractionHand.MAIN_HAND, stack);
            });
            context.waitTicks(5);
            context.getInput().lookAt(origin.above(30));
            context.getInput().pressKey(options -> options.keyUse);
            if (!screenshotOpenedScreen(context, "af-portable-flux-cell")) {
                failures.add("portable flux cell screen");
            }
            server.runCommand("clear @a");

            // Applied Flux's pages in AE2's guidebook
            var guide = Guides.getById(AppEng.makeId("guide"));
            var pageIds = guide.getPages().stream()
                    .map(ParsedGuidePage::getId)
                    .filter(id -> id.getNamespace().equals(AppFlux.MODID))
                    .sorted()
                    .toList();
            LOG.info("Applied Flux guide pages: {}", pageIds);
            if (pageIds.isEmpty()) {
                failures.add("guide pages");
            }
            for (var pageId : pageIds) {
                context.runOnClient(mc -> mc.gui.setScreen(GuideScreen.openNew(guide, PageAnchor.page(pageId))));
                context.waitTicks(3);
                context.takeScreenshot("af-guide-" + pageId.getPath().replace('/', '-').replace(".md", ""));
            }
            context.setScreen(() -> null);

            if (!failures.isEmpty()) {
                throw new AssertionError("Failed: " + failures);
            }
        }
    }

    private static long storedInNetwork(TestServerContext server, BlockPos accessor) {
        return server.computeOnServer(s -> {
            var te = (TileFluxAccessor) s.overworld().getBlockEntity(accessor);
            return te.getStorage().getInventory().extract(FluxKey.of(EnergyType.FE), Long.MAX_VALUE,
                    Actionable.SIMULATE, IActionSource.empty());
        });
    }

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

    private static ServerPlayer firstPlayer(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static boolean openAndScreenshot(ClientGameTestContext context, BlockPos pos, String name) {
        context.getInput().lookAt(pos);
        context.getInput().pressKey(options -> options.keyUse);
        return screenshotOpenedScreen(context, name);
    }

    private static boolean screenshotOpenedScreen(ClientGameTestContext context, String name) {
        boolean opened;
        try {
            context.waitFor(mc -> mc.gui.screen() != null, 60);
            context.waitTicks(10);
            LOG.info("Opened screen for {}: {}", name, context.computeOnClient(mc -> mc.gui.screen().getClass().getName()));
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
}
