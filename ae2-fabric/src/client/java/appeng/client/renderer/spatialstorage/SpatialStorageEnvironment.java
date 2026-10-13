package appeng.client.renderer.spatialstorage;

import net.minecraft.client.Minecraft;

import appeng.spatial.SpatialStorageDimensionIds;

/**
 * Replaces the sky, clouds and weather of the spatial storage dimension. NeoForge offers custom environment effect
 * renderers for this; on Fabric, mixins into the vanilla renderers consult this class.
 */
public final class SpatialStorageEnvironment {
    private static SpatialStorageSkyRenderer skyRenderer;

    private SpatialStorageEnvironment() {
    }

    public static boolean isActive() {
        var level = Minecraft.getInstance().level;
        return level != null && level.dimension() == SpatialStorageDimensionIds.WORLD_ID;
    }

    public static SpatialStorageSkyRenderer getSkyRenderer() {
        if (skyRenderer == null) {
            skyRenderer = new SpatialStorageSkyRenderer();
        }
        return skyRenderer;
    }
}
