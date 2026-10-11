package appeng.pack.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
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
            context.takeScreenshot("pack-normal-world");
        }
    }
}
