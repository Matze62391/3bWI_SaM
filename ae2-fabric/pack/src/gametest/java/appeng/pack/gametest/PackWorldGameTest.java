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

            // Fluid cells filled by command (1,000,000 buckets in droplets): lava and Advanced AE's quantum infusion
            var server = world.getServer();
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
