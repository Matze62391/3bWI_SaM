package appeng.pack.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Takes screenshots of a player in the full quantum armor (GeckoLib models) in third person, standing and walking.
 * With -PshaderPack this runs with Sodium, Iris and a shader pack (./gradlew :pack:prodShaderTest).
 */
public class PackQuantumArmorGameTest implements FabricClientGameTest {
    private static final Logger LOG = LoggerFactory.getLogger(PackQuantumArmorGameTest.class);

    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder()
                .adjustSettings(settings -> settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
                .create()) {
            var server = world.getServer();
            server.runCommand("gamerule advance_time false");
            server.runCommand("gamerule send_command_feedback false");
            server.runCommand("time set noon");
            context.waitTicks(40);
            server.runCommand("item replace entity @a armor.head with advanced_ae:quantum_helmet");
            server.runCommand("item replace entity @a armor.chest with advanced_ae:quantum_chestplate");
            server.runCommand("item replace entity @a armor.legs with advanced_ae:quantum_leggings");
            server.runCommand("item replace entity @a armor.feet with advanced_ae:quantum_boots");
            context.waitTicks(20);

            context.runOnClient(mc -> {
                if (!mc.gui.hud.isHidden()) {
                    mc.gui.hud.toggle();
                }
            });
            for (var camera : new CameraType[] {CameraType.THIRD_PERSON_FRONT, CameraType.THIRD_PERSON_BACK}) {
                context.runOnClient(mc -> mc.options.setCameraType(camera));
                context.waitTicks(20);
                context.takeScreenshot("pack-quantum-armor-" + camera.name().toLowerCase());
            }
            // walking animates the arms and legs
            context.getInput().holdKeyFor(options -> options.keyUp, 10);
            context.takeScreenshot("pack-quantum-armor-walking");
            // all upgrades on the helmet and the chestplate (the strength upgrade shows the blades)
            server.runOnServer(s -> {
                var player = s.getPlayerList().getPlayers().getFirst();
                for (var slot : new net.minecraft.world.entity.EquipmentSlot[] {
                        net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST}) {
                    var stack = player.getItemBySlot(slot);
                    var armor = (net.pedroksl.advanced_ae.common.items.armors.IUpgradeableItem) stack.getItem();
                    for (var upgrade : net.pedroksl.advanced_ae.common.items.upgrades.UpgradeType.values()) {
                        try {
                            armor.applyUpgrade(stack, upgrade);
                        } catch (RuntimeException e) {
                            LOG.info("Upgrade {} not applied to {}: {}", upgrade, slot, e.toString());
                        }
                    }
                }
            });
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            context.waitTicks(20);
            context.takeScreenshot("pack-quantum-armor-upgrades");

            // only the helmet and the chestplate
            server.runCommand("item replace entity @a armor.legs with minecraft:air");
            server.runCommand("item replace entity @a armor.feet with minecraft:air");
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            context.waitTicks(20);
            context.takeScreenshot("pack-quantum-armor-helmet-chestplate");
            context.runOnClient(mc -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                if (mc.gui.hud.isHidden()) {
                    mc.gui.hud.toggle();
                }
            });
            LOG.info("Quantum armor screenshots taken");
        }
    }
}
