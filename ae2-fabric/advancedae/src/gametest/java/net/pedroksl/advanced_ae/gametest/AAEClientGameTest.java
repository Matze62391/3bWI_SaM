package net.pedroksl.advanced_ae.gametest;

import java.util.List;

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
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.pedroksl.advanced_ae.client.gui.QuantumCrafterWirelessTermScreen;
import net.pedroksl.advanced_ae.client.gui.ReactionChamberScreen;
import net.pedroksl.advanced_ae.common.definitions.AAEConfig;
import net.pedroksl.ae2addonlib.client.config.ModConfigScreen;
import appeng.api.ids.AEComponents;
import appeng.client.Hotkeys;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.material.Fluids;
import net.pedroksl.advanced_ae.AdvancedAE;
import net.pedroksl.advanced_ae.client.AAEHotkeys;
import net.pedroksl.advanced_ae.common.definitions.AAEBlocks;
import net.pedroksl.advanced_ae.common.definitions.AAEFluids;
import net.pedroksl.advanced_ae.common.definitions.AAEHotkeysRegistry;
import net.pedroksl.advanced_ae.common.definitions.AAEItems;
import net.pedroksl.advanced_ae.common.entities.AdvCraftingBlockEntity;
import net.pedroksl.advanced_ae.common.entities.QuantumCrafterEntity;
import net.pedroksl.advanced_ae.common.entities.ReactionChamberEntity;
import net.pedroksl.advanced_ae.common.items.armors.QuantumArmorBase;
import net.pedroksl.advanced_ae.common.items.upgrades.UpgradeType;
import net.pedroksl.advanced_ae.xmod.jei.ReactionChamberCategory;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;

import guideme.Guides;
import guideme.PageAnchor;
import guideme.compiler.ParsedGuidePage;
import guideme.internal.screen.GuideScreen;

import appeng.api.config.Actionable;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEFluidKey;
import appeng.core.AppEng;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.ItemDefinition;
import appeng.items.parts.PartItem;

/**
 * Builds Advanced AE's machines in a real client, checks that they work and takes screenshots of them. The screenshots
 * end up in {@code advancedae/build/run/clientGameTest/screenshots}.
 */
public class AAEClientGameTest implements FabricClientGameTest {
    private static final Logger LOG = LoggerFactory.getLogger(AAEClientGameTest.class);

    private static final List<String> MACHINES = List.of(
            "reaction_chamber", "quantum_crafter", "adv_pattern_provider", "small_adv_pattern_provider");

    private static final List<ItemDefinition<? extends PartItem<?>>> PARTS = List.of(
            AAEItems.ADV_PATTERN_PROVIDER,
            AAEItems.SMALL_ADV_PATTERN_PROVIDER,
            AAEItems.STOCK_EXPORT_BUS,
            AAEItems.IMPORT_EXPORT_BUS,
            AAEItems.ADVANCED_IO_BUS,
            AAEItems.QUANTUM_CRAFTER_TERMINAL);

    @Override
    public void runTest(ClientGameTestContext context) {
        // Config screen (opened through Mod Menu in normal play)
        context.setScreen(() -> new ModConfigScreen(
                null,
                Component.literal("Advanced AE"),
                List.of(
                        new ModConfigScreen.Section(Component.literal("Client"), AAEConfig.instance().getClientSpec()),
                        new ModConfigScreen.Section(
                                Component.literal("Common"), AAEConfig.instance().getCommonSpec()))));
        context.waitTicks(10);
        context.takeScreenshot("aae-config-screen");
        context.setScreen(() -> null);

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
            // Keep the chat empty for the screenshots
            server.runCommand("gamerule send_command_feedback false");
            context.waitTicks(40);

            var origin = server.computeOnServer(s -> firstPlayer(s).blockPosition());
            var target = origin.offset(0, 0, 3);

            // Machines: place each one in front of the player (powered by a creative energy cell) and open it
            setBlock(server, target.below(), "ae2:creative_energy_cell");
            for (var machine : MACHINES) {
                setBlock(server, target, "advanced_ae:" + machine);
                context.waitTicks(5);
                openAndScreenshot(context, target, "aae-" + machine);
            }
            setBlock(server, target, "minecraft:air");

            // Parts on a cable
            for (var part : PARTS) {
                var partPos = target;
                server.runOnServer(s -> PartHelper.setPart(s.overworld(), partPos, Direction.NORTH, null, part.get()));
                context.waitTicks(5);
                openAndScreenshot(context, partPos, "aae-part-" + part.id().getPath());
                setBlock(server, partPos, "minecraft:air");
            }
            setBlock(server, target.below(), "minecraft:air");

            testReactionChamber(context, server, origin.offset(3, 0, 3));
            testQuantumComputer(context, server, origin);

            // The quantum infusion fluid in the world and its bucket
            var fluidPos = origin.offset(0, 0, 4);
            setBlock(server, fluidPos, "advanced_ae:quantum_infusion_block");
            server.runOnServer(s -> firstPlayer(s).setItemInHand(InteractionHand.MAIN_HAND,
                    AAEFluids.QUANTUM_INFUSION.bucketItem().getDefaultInstance()));
            context.getInput().lookAt(fluidPos.below());
            context.waitTicks(20);
            context.takeScreenshot("aae-quantum-infusion-fluid");
            server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(fluidPos.getX() - 8, fluidPos.getY(),
                    fluidPos.getZ() - 8, fluidPos.getX() + 8, fluidPos.getY() + 1, fluidPos.getZ() + 8));

            // Advanced pattern encoder (opened by using the item)
            server.runOnServer(s -> firstPlayer(s).setItemInHand(InteractionHand.MAIN_HAND,
                    AAEItems.ADV_PATTERN_ENCODER.stack()));
            context.waitTicks(5);
            context.getInput().lookAt(origin.above(30));
            context.getInput().pressKey(options -> options.keyUse);
            screenshotOpenedScreen(context, "aae-adv-pattern-encoder");
            server.runOnServer(s -> firstPlayer(s).setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));

            testQuantumArmor(context, server);
            testWirelessTerminal(context, server, origin);

            // JEI shows the reaction chamber's recipes
            var runtime = TestJeiPlugin.runtime;
            if (runtime == null) {
                throw new AssertionError("JEI runtime is not available");
            }
            context.runOnClient(mc -> runtime.getRecipesGui().showTypes(List.of(ReactionChamberCategory.RECIPE_TYPE)));
            context.waitTicks(20);
            context.takeScreenshot("aae-jei-reaction-chamber");
            showJeiRecipes(context, AEItems.FLUIX_CRYSTAL.stack());
            context.takeScreenshot("aae-jei-fluix-crystal");
            showJeiRecipes(context, AAEItems.SHATTERED_SINGULARITY.stack());
            context.takeScreenshot("aae-jei-shattered-singularity");
            context.setScreen(() -> null);
            testJeiClickArea(context, server, origin);
            context.setScreen(() -> null);

            // Advanced AE's pages in AE2's guidebook
            var guide = Guides.getById(AppEng.makeId("guide"));
            var pageIds = guide.getPages().stream()
                    .map(ParsedGuidePage::getId)
                    .filter(id -> id.getNamespace().equals(AdvancedAE.MOD_ID))
                    .sorted()
                    .toList();
            LOG.info("Advanced AE guide pages: {}", pageIds);
            if (pageIds.isEmpty()) {
                throw new AssertionError("The guidebook has no Advanced AE pages");
            }
            for (var pageId : pageIds) {
                context.runOnClient(mc -> mc.gui.setScreen(GuideScreen.openNew(guide, PageAnchor.page(pageId))));
                context.waitTicks(3);
                context.takeScreenshot("aae-guide-" + pageId.getPath().replace('/', '-').replace(".md", ""));
            }
            context.setScreen(() -> null);
        }
    }

    /**
     * Feeds two recipes into a reaction chamber through Fabric's transfer API, like pipes would, and waits for the
     * results: one with an item output and one with a fluid output.
     */
    private static void testReactionChamber(ClientGameTestContext context, TestServerContext server, BlockPos pos) {
        setBlock(server, pos.below(), "ae2:creative_energy_cell");
        setBlock(server, pos, "advanced_ae:reaction_chamber");
        context.waitTicks(20);

        // Fluix crystals: items and 500 mB of water
        server.runOnServer(s -> {
            var level = s.overworld();
            var items = ItemStorage.SIDED.find(level, pos, Direction.UP);
            var fluids = FluidStorage.SIDED.find(level, pos, Direction.UP);
            if (items == null || fluids == null) {
                throw new AssertionError("The reaction chamber exposes no item or fluid storage");
            }
            try (var tx = Transaction.openOuter()) {
                insert(items, AEItems.CERTUS_QUARTZ_CRYSTAL_CHARGED.stack(), 16, tx);
                insert(items, new ItemStack(Items.REDSTONE), 16, tx);
                insert(items, new ItemStack(Items.QUARTZ), 16, tx);
                var water = fluids.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET / 2, tx);
                if (water != FluidConstants.BUCKET / 2) {
                    throw new AssertionError("Only " + water + " droplets of water were accepted");
                }
                tx.commit();
            }
        });
        context.getInput().lookAt(pos);
        waitForServer(context, server, s -> {
            var chamber = (ReactionChamberEntity) s.overworld().getBlockEntity(pos);
            return chamber != null && AEItems.FLUIX_CRYSTAL.is(chamber.getOutput().getStackInSlot(0))
                    && chamber.getOutput().getStackInSlot(0).getCount() == 64;
        }, 2400, "64 fluix crystals from the reaction chamber");
        LOG.info("The reaction chamber made 64 fluix crystals");
        openAndScreenshot(context, pos, "aae-reaction-chamber-fluix");

        // Quantum infusion: a dust and 4 buckets of water make 1 bucket of quantum infusion
        server.runOnServer(s -> {
            var chamber = (ReactionChamberEntity) s.overworld().getBlockEntity(pos);
            chamber.getOutput().setItemDirect(0, ItemStack.EMPTY);
            var items = ItemStorage.SIDED.find(s.overworld(), pos, Direction.UP);
            var fluids = FluidStorage.SIDED.find(s.overworld(), pos, Direction.UP);
            try (var tx = Transaction.openOuter()) {
                insert(items, AAEItems.QUANTUM_INFUSED_DUST.stack(), 1, tx);
                fluids.insert(FluidVariant.of(Fluids.WATER), 4 * FluidConstants.BUCKET, tx);
                tx.commit();
            }
        });
        var infusion = AEFluidKey.of(AAEFluids.QUANTUM_INFUSION.source());
        waitForServer(context, server, s -> {
            var chamber = (ReactionChamberEntity) s.overworld().getBlockEntity(pos);
            var output = chamber.getTank().getStack(0);
            return output != null && output.what().equals(infusion) && output.amount() == FluidConstants.BUCKET;
        }, 2400, "a bucket of quantum infusion from the reaction chamber");
        LOG.info("The reaction chamber made a bucket of quantum infusion");
        worldScreenshot(context, "aae-reaction-chamber-block");
        openAndScreenshot(context, pos, "aae-reaction-chamber-infusion");

        // The fluid can be pulled out like a pipe would
        server.runOnServer(s -> {
            var fluids = FluidStorage.SIDED.find(s.overworld(), pos, Direction.DOWN);
            try (var tx = Transaction.openOuter()) {
                var extracted = fluids.extract(FluidVariant.of(AAEFluids.QUANTUM_INFUSION.source()),
                        FluidConstants.BUCKET, tx);
                if (extracted != FluidConstants.BUCKET) {
                    throw new AssertionError("Extracted " + extracted + " droplets of quantum infusion");
                }
            }
        });

        setBlock(server, pos, "minecraft:air");
        setBlock(server, pos.below(), "minecraft:air");
    }

    private static void insert(net.fabricmc.fabric.api.transfer.v1.storage.Storage<ItemVariant> storage,
            ItemStack stack, int amount, Transaction tx) {
        var inserted = storage.insert(ItemVariant.of(stack), amount, tx);
        if (inserted != amount) {
            throw new AssertionError("Only " + inserted + " of " + amount + " " + stack + " were accepted");
        }
    }

    /**
     * A 5x5x5 quantum computer: a shell of quantum structure blocks around storage, a core and the other units.
     */
    private static void testQuantumComputer(ClientGameTestContext context, TestServerContext server, BlockPos origin) {
        var min = origin.offset(-2, 0, 5);
        var max = min.offset(4, 4, 4);
        fill(server, min, max, "advanced_ae:quantum_structure");
        fill(server, min.offset(1, 1, 1), max.offset(-1, -1, -1), "advanced_ae:quantum_storage_128");
        var center = min.offset(2, 2, 2);
        setBlock(server, center, "advanced_ae:quantum_core");
        setBlock(server, center.above(), "advanced_ae:quantum_accelerator");
        setBlock(server, center.below(), "advanced_ae:quantum_multi_threader");
        setBlock(server, center.east(), "advanced_ae:data_entangler");
        setBlock(server, center.west(), "advanced_ae:quantum_unit");
        setBlock(server, min.offset(-1, 0, 2), "ae2:creative_energy_cell");
        context.waitTicks(40);

        var formed = server.computeOnServer(s -> s.overworld().getBlockEntity(center) instanceof AdvCraftingBlockEntity be
                && be.isFormed());
        if (!formed) {
            throw new AssertionError("The quantum computer did not form");
        }
        LOG.info("The quantum computer formed");

        server.runCommand("tp @a %d %d %d 0 0".formatted(origin.getX(), origin.getY(), origin.getZ() - 3));
        context.waitTicks(10);
        context.getInput().lookAt(min.offset(2, 2, 0));
        worldScreenshot(context, "aae-quantum-computer");
        server.runCommand("tp @a %d %d %d 0 0".formatted(origin.getX(), origin.getY(), origin.getZ()));
        context.waitTicks(10);
        openAndScreenshot(context, min.offset(2, 2, 0), "aae-quantum-computer-gui");

        fill(server, min.offset(-1, 0, 0), max, "minecraft:air");
    }

    /**
     * Equips the quantum armor, opens its configuration with the hotkey and looks at it in third person.
     */
    private static void testQuantumArmor(ClientGameTestContext context, TestServerContext server) {
        server.runCommand("item replace entity @a armor.head with advanced_ae:quantum_helmet");
        server.runCommand("item replace entity @a armor.chest with advanced_ae:quantum_chestplate");
        server.runCommand("item replace entity @a armor.legs with advanced_ae:quantum_leggings");
        server.runCommand("item replace entity @a armor.feet with advanced_ae:quantum_boots");
        context.waitTicks(10);

        var hotkey = AAEHotkeys.INSTANCE.getHotkeyMapping(AAEHotkeysRegistry.Keys.ARMOR_CONFIG.getId());
        if (hotkey == null) {
            throw new AssertionError("The quantum armor configuration hotkey is not registered");
        }
        context.getInput().pressKey(options -> hotkey.mapping());
        screenshotOpenedScreen(context, "aae-quantum-armor-config");

        server.runCommand("tp @a ~ ~ ~ 180 0");
        context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        context.waitTicks(20);
        worldScreenshot(context, "aae-quantum-armor");
        context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        context.waitTicks(5);

        // Upgrades: flight (granted through the player's abilities on Fabric) and step assist (an attribute modifier)
        server.runCommand("gamemode survival @a");
        server.runOnServer(s -> {
            var player = firstPlayer(s);
            for (var slot : List.of(EquipmentSlot.CHEST, EquipmentSlot.FEET)) {
                var stack = player.getItemBySlot(slot);
                var armor = (QuantumArmorBase) stack.getItem();
                armor.applyUpgrade(stack, slot == EquipmentSlot.CHEST ? UpgradeType.FLIGHT : UpgradeType.STEP_ASSIST);
                armor.injectAEPower(stack, armor.getAEMaxPower(stack), Actionable.MODULATE);
            }
        });
        context.waitTicks(10);
        server.runOnServer(s -> {
            var player = firstPlayer(s);
            if (!player.getAbilities().mayfly) {
                throw new AssertionError("The flight upgrade doesn't let the player fly");
            }
            var stepHeight = player.getAttributeValue(Attributes.STEP_HEIGHT);
            if (stepHeight <= 0.6) {
                throw new AssertionError("The step assist upgrade doesn't change the step height: " + stepHeight);
            }
            LOG.info("Quantum armor upgrades work: flight, step height {}", stepHeight);

            var chest = player.getItemBySlot(EquipmentSlot.CHEST);
            ((QuantumArmorBase) chest.getItem()).removeUpgrade(chest, UpgradeType.FLIGHT);
        });
        context.waitTicks(10);
        server.runOnServer(s -> {
            if (firstPlayer(s).getAbilities().mayfly) {
                throw new AssertionError("The player can still fly after removing the flight upgrade");
            }
        });
        server.runCommand("gamemode creative @a");
        server.runCommand("clear @a");
        context.waitTicks(5);
    }

    /**
     * The wireless quantum crafter terminal, linked to a network with a quantum crafter, opened by using it and by
     * AE2's hotkey.
     */
    private static void testWirelessTerminal(ClientGameTestContext context, TestServerContext server, BlockPos origin) {
        var accessPointPos = origin.offset(-3, 0, 0);
        setBlock(server, accessPointPos.below(), "ae2:creative_energy_cell");
        setBlock(server, accessPointPos, "ae2:wireless_access_point[facing=up]");
        setBlock(server, accessPointPos.below().west(), "advanced_ae:quantum_crafter");
        context.waitTicks(20);
        var crafterOnline = server.computeOnServer(s -> s.overworld().getBlockEntity(accessPointPos.below().west())
                        instanceof QuantumCrafterEntity crafter
                && crafter.getMainNode().isActive());
        LOG.info("Quantum crafter for the wireless terminal is online: {}", crafterOnline);

        server.runOnServer(s -> {
            var player = firstPlayer(s);
            var stack = AAEItems.QUANTUM_CRAFTER_WIRELESS_TERMINAL.stack();
            var terminal = AAEItems.QUANTUM_CRAFTER_WIRELESS_TERMINAL.get();
            terminal.injectAEPower(stack, terminal.getAEMaxPower(stack), Actionable.MODULATE);
            stack.set(
                    AEComponents.WIRELESS_LINK_TARGET,
                    GlobalPos.of(player.level().dimension(), accessPointPos));
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        });
        context.waitTicks(5);
        context.getInput().lookAt(origin.above(30));
        context.getInput().pressKey(options -> options.keyUse);
        screenshotOpenedScreen(context, "aae-wireless-quantum-crafter-terminal");
        assertLastScreen(context, QuantumCrafterWirelessTermScreen.class);

        // AE2's hotkey opens it from anywhere in the inventory
        server.runOnServer(s -> {
            var player = firstPlayer(s);
            var stack = player.getMainHandItem();
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.getInventory().setItem(20, stack);
        });
        context.waitTicks(5);
        var hotkey = Hotkeys.getHotkeyMapping("wireless_quantum_crafter_terminal");
        if (hotkey == null) {
            throw new AssertionError("The wireless quantum crafter terminal has no hotkey");
        }
        // Like all of AE2's hotkeys, it is unbound by default
        context.runOnClient(mc -> {
            hotkey.mapping()
                    .setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYBOARD.getOrCreate(
                            com.mojang.blaze3d.platform.InputConstants.KEY_K));
            net.minecraft.client.KeyMapping.resetMapping();
        });
        context.getInput().pressKey(options -> hotkey.mapping());
        screenshotOpenedScreen(context, "aae-wireless-quantum-crafter-terminal-hotkey");
        assertLastScreen(context, QuantumCrafterWirelessTermScreen.class);

        server.runCommand("clear @a");
        setBlock(server, accessPointPos, "minecraft:air");
        setBlock(server, accessPointPos.below(), "minecraft:air");
        setBlock(server, accessPointPos.below().west(), "minecraft:air");
    }

    /**
     * Clicking the arrow in the reaction chamber's screen shows its recipes in JEI.
     */
    private static void testJeiClickArea(ClientGameTestContext context, TestServerContext server, BlockPos origin) {
        var pos = origin.offset(0, 0, 3);
        setBlock(server, pos, "advanced_ae:reaction_chamber");
        context.waitTicks(5);
        context.getInput().lookAt(pos);
        context.getInput().pressKey(options -> options.keyUse);
        context.waitFor(mc -> mc.gui.screen() instanceof ReactionChamberScreen, 60);
        context.waitTicks(10);
        var cursor = context.computeOnClient(mc -> {
            var screen = (ReactionChamberScreen) mc.gui.screen();
            var scale = mc.getWindow().getGuiScale();
            var left = (screen.width - screen.imageWidth) / 2;
            var top = (screen.height - screen.imageHeight) / 2;
            return new double[] {(left + 107) * scale, (top + 51) * scale};
        });
        context.getInput().setCursorPos(cursor[0], cursor[1]);
        context.waitTicks(2);
        context.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(20);
        var screenName = context.computeOnClient(
                mc -> mc.gui.screen() == null ? "none" : mc.gui.screen().getClass().getName());
        context.takeScreenshot("aae-jei-click-area");
        if (!screenName.contains("RecipesGui")) {
            throw new AssertionError("Clicking the reaction chamber's arrow opened " + screenName);
        }
        context.setScreen(() -> null);
        context.runOnClient(mc -> {
            if (mc.player != null) {
                mc.player.closeContainer();
            }
        });
        setBlock(server, pos, "minecraft:air");
    }

    private static String lastScreen = "";

    private static void assertLastScreen(ClientGameTestContext context, Class<?> screenClass) {
        if (!lastScreen.equals(screenClass.getName())) {
            throw new AssertionError("Expected " + screenClass.getSimpleName() + " but got " + lastScreen);
        }
    }

    /**
     * Takes a screenshot of the world without the HUD.
     */
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
            lastScreen = context.computeOnClient(mc -> mc.gui.screen().getClass().getName());
            LOG.info("Opened screen for {}: {}", name, lastScreen);
        } catch (AssertionError e) {
            lastScreen = "";
            LOG.warn("No screen opened for {}", name);
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
    }
}
