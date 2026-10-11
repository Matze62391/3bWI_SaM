package appeng.pack.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a normal survival world (default world type, like players do) with the pack and looks around.
 */
public class PackWorldGameTest implements FabricClientGameTest {
    private static final Logger LOG = LoggerFactory.getLogger(PackWorldGameTest.class);

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            for (var mapping : client.options.keyMappings) {
                var key = KeyMappingHelper.getBoundKeyOf(mapping);
                if (key.getType() == InputConstants.Type.KEYBOARD && key.getValue() < 0) {
                    throw new AssertionError("Key mapping " + mapping.getName() + " bound to invalid key " + key);
                }
            }
            // Simulate an options.txt written by an older version with "key.keyboard.-1"
            var mapping = client.options.keyMappings[client.options.keyMappings.length - 1];
            var old = KeyMappingHelper.getBoundKeyOf(mapping);
            mapping.setKey(InputConstants.Type.KEYBOARD.getOrCreate(-1));
            client.options.save();
            client.options.load();
            var migrated = KeyMappingHelper.getBoundKeyOf(mapping);
            if (migrated.getValue() != InputConstants.UNKNOWN.getValue()) {
                throw new AssertionError("key.keyboard.-1 was not migrated: " + migrated);
            }
            mapping.setKey(old);
            KeyMapping.resetMapping();
            client.options.save();
            LOG.info("Legacy key.keyboard.-1 binding migrated");
        });
        var start = System.currentTimeMillis();
        try (var world = context.worldBuilder().adjustSettings(settings -> {
            settings.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
            settings.getNormalPresetList().stream()
                    .filter(entry -> entry.preset() != null && entry.preset().is(WorldPresets.NORMAL))
                    .findFirst()
                    .ifPresent(settings::setWorldType);
        }).create()) {
            LOG.info("Normal world created in {} ms", System.currentTimeMillis() - start);
            context.waitTicks(100);
            // A key bound to an out-of-range scancode used to crash KeyMapping.setAll() when the mouse got grabbed.
            context.runOnClient(client -> {
                var mapping = client.options.keyMappings[client.options.keyMappings.length - 1];
                var old = KeyMappingHelper.getBoundKeyOf(mapping);
                mapping.setKey(InputConstants.Type.KEYBOARD.getOrCreate(1073742049));
                KeyMapping.resetMapping();
                KeyMapping.setAll();
                mapping.setKey(old);
                KeyMapping.resetMapping();
                LOG.info("Invalid key binding handled without crash");
            });
            context.takeScreenshot("pack-normal-world");

            var server = world.getServer();
            // Tech Reborn and Effortless Building are part of the pack
            for (var modId : java.util.List.of("techreborn", "reborncore", "effortlessbuilding")) {
                if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(modId)) {
                    throw new AssertionError(modId + " is not loaded");
                }
            }
            var furnacePos = server.computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().blockPosition()
                    .offset(0, 0, 2));
            server.runCommand("setblock %d %d %d techreborn:iron_furnace".formatted(furnacePos.getX(),
                    furnacePos.getY(), furnacePos.getZ()));
            context.waitTicks(5);
            context.getInput().lookAt(furnacePos);
            context.getInput().pressKey(options -> options.keyUse);
            context.waitFor(mc -> mc.gui.screen() != null, 100);
            String trScreen = context.computeOnClient(mc -> mc.gui.screen().getClass().getName());
            LOG.info("Tech Reborn screen: {}", trScreen);
            context.takeScreenshot("pack-techreborn-iron-furnace");
            context.runOnClient(mc -> mc.gui.setScreen(null));
            server.runCommand("setblock %d %d %d minecraft:air".formatted(furnacePos.getX(), furnacePos.getY(),
                    furnacePos.getZ()));

            // The universal terminal in one step: receiver, the three terminals and a dense energy cell
            var wut = server.computeOnServer(s -> {
                var level = s.overworld();
                java.util.function.Function<String, net.minecraft.world.item.ItemStack> item = id ->
                        new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM
                                .getValue(net.minecraft.resources.Identifier.parse(id)));
                var empty = net.minecraft.world.item.ItemStack.EMPTY;
                var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                        empty, item.apply("ae2:wireless_receiver"), empty,
                        item.apply("ae2:crafting_terminal"), item.apply("ae2:pattern_encoding_terminal"),
                        item.apply("ae2:pattern_access_terminal"),
                        empty, item.apply("ae2:dense_energy_cell"), empty));
                return s.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,
                        input, level).map(r -> r.value().assemble(input)).orElse(empty);
            });
            var terminals = wut.getComponents().keySet().stream()
                    .map(type -> net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type))
                    .filter(id -> id != null && id.getPath().startsWith("has_"))
                    .map(Object::toString).sorted().toList();
            LOG.info("Crafted {} with {}", wut.getItem(), terminals);
            if (!terminals.equals(java.util.List.of("ae2wtlib:has_crafting_terminal",
                    "ae2wtlib:has_pattern_access_terminal", "ae2wtlib:has_pattern_encoding_terminal"))) {
                throw new AssertionError("Universal terminal recipe gave " + wut + " with " + terminals);
            }
            server.runCommand("clear @a");
            server.runCommand("give @a ae2wtlib:wireless_universal_terminal[ae2wtlib:has_crafting_terminal={},"
                    + "ae2wtlib:has_pattern_encoding_terminal={},ae2wtlib:has_pattern_access_terminal={}]");
            context.waitTicks(5);
            var given = server.computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().getInventory()
                    .getNonEquipmentItems().stream().filter(st -> !st.isEmpty()).findFirst()
                    .map(st -> st.getComponents().keySet().stream().filter(t -> {
                        var id = net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(t);
                        return id != null && id.getPath().startsWith("has_");
                    }).count()).orElse(0L));
            if (given != 3) {
                throw new AssertionError("/give universal terminal has " + given + " terminals");
            }
            context.getInput().pressKey(options -> options.keyUse);
            context.waitFor(mc -> mc.gui.screen() != null, 100);
            context.takeScreenshot("pack-universal-terminal");
            context.runOnClient(mc -> mc.gui.setScreen(null));

            // An item cell filled by command with 1,000,000 white wool and 1,000,000 glowstone dust
            server.runCommand("clear @a");
            server.runCommand("give @a ae2:item_storage_cell_256k[ae2:storage_cell_inv=["
                    + "{\"#t\":\"ae2:i\",id:\"minecraft:white_wool\",\"#\":1000000L},"
                    + "{\"#t\":\"ae2:i\",id:\"minecraft:glowstone_dust\",\"#\":1000000L}]]");
            context.waitTicks(5);
            var itemCell = server.computeOnServer(s -> {
                var player = s.getPlayerList().getPlayers().getFirst();
                for (var stack : player.getInventory().getNonEquipmentItems()) {
                    var cell = appeng.api.storage.StorageCells.getCellInventory(stack, null);
                    if (cell != null) {
                        var stacks = cell.getAvailableStacks();
                        return stacks.get(appeng.api.stacks.AEItemKey.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("minecraft:white_wool"))))
                                + "/" + stacks.get(appeng.api.stacks.AEItemKey.of(
                                        net.minecraft.world.item.Items.GLOWSTONE_DUST));
                    }
                }
                return "none";
            });
            LOG.info("The given item cell holds wool/glowstone dust {}", itemCell);
            if (!itemCell.equals("1000000/1000000")) {
                throw new AssertionError("Item cell from /give holds " + itemCell);
            }

            // Fluid cells filled by command (1,000,000 buckets in droplets): lava and Advanced AE's quantum infusion
            for (var fluidId : java.util.List.of("minecraft:lava", "advanced_ae:quantum_infusion_source")) {
                server.runCommand("clear @a");
                server.runCommand("give @a ae2:fluid_storage_cell_256k[ae2:storage_cell_inv=[{\"#t\":\"ae2:f\","
                        + "id:\"" + fluidId + "\",\"#\":81000000000L}]]");
                context.waitTicks(5);
                var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.getValue(
                        net.minecraft.resources.Identifier.parse(fluidId));
                var amount = server.computeOnServer(s -> {
                    var player = s.getPlayerList().getPlayers().getFirst();
                    for (var stack : player.getInventory().getNonEquipmentItems()) {
                        var cell = appeng.api.storage.StorageCells.getCellInventory(stack, null);
                        if (cell != null) {
                            return cell.getAvailableStacks().get(appeng.api.stacks.AEFluidKey.of(fluid));
                        }
                    }
                    return -1L;
                });
                LOG.info("The given fluid cell holds {} buckets of {}", amount / 81000, fluidId);
                if (amount != 81_000_000_000L) {
                    throw new AssertionError("Fluid cell with " + fluidId + " from /give holds " + amount);
                }
            }
        }
    }
}
