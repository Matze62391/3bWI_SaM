package guideme.internal.siteexport;

import guideme.internal.GuideOnStartup;
import guideme.internal.GuideRegistry;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.gui.screens.LoadingOverlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for exporting a Guide on startup and then exiting the game.
 */
public final class SiteExportOnStartup {
    private static final Logger LOG = LoggerFactory.getLogger(SiteExportOnStartup.class);

    private SiteExportOnStartup() {
    }

    public static void init() {
        var guidesToExport = getGuidesToExport();

        if (!guidesToExport.isEmpty()) {
            // Export once the initial resource reload has finished
            ClientLifecycleEvents.CLIENT_STARTED.register(client -> afterInitialReload(client, () -> {

                GuideOnStartup.runDatapackReload();

                for (var entry : guidesToExport.entrySet()) {
                    var guide = GuideRegistry.getById(entry.getKey());
                    if (guide == null) {
                        LOG.error("Cannot export guide '{}' since it does not exist.", entry.getKey());
                        System.exit(1);
                    }

                    Path outputFolder = entry.getValue();
                    try {
                        new SiteExporter(Minecraft.getInstance(), outputFolder, guide).export();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        System.exit(1);
                    }
                }
                Minecraft.getInstance().stop();
            }));
        }
    }

    private static void afterInitialReload(Minecraft client, Runnable task) {
        if (client.gui.overlay() instanceof LoadingOverlay loadingOverlay) {
            loadingOverlay.reload.done().thenRunAsync(task, client);
        } else {
            client.execute(task);
        }
    }

    private static Map<Identifier, Path> getGuidesToExport() {
        var guideIdsString = System.getProperty("guideme.exportOnStartupAndExit");

        if (guideIdsString == null) {
            return Map.of();
        }

        var guidesToExport = new HashMap<Identifier, Path>();
        for (String unparsedResourceId : guideIdsString.split(",")) {
            var guideId = Identifier.parse(unparsedResourceId);
            String destinationPropertyName = "guideme.exportDestination." + guideId.getNamespace() + "."
                    + guideId.getPath();
            var destinationDirectory = System.getProperty(destinationPropertyName);
            if (destinationDirectory == null) {
                throw new RuntimeException("When exporting GuideME guide " + guideId
                        + " also set a destination directory using system property " + destinationPropertyName);
            }
            guidesToExport.put(guideId, Paths.get(destinationDirectory));
        }
        return guidesToExport;
    }

}
