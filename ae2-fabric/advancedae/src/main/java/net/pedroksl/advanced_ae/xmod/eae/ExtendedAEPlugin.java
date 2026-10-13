package net.pedroksl.advanced_ae.xmod.eae;

/**
 * ExtendedAE's pattern providers can be upgraded to advanced pattern providers too. Compared by class name, so Advanced
 * AE doesn't need ExtendedAE to compile (ExtendedAE depends on Advanced AE).
 */
public class ExtendedAEPlugin {
    private static final String TILE = "com.glodblock.github.extendedae.common.tileentities.TileExPatternProvider";
    private static final String PART = "com.glodblock.github.extendedae.common.parts.PartExPatternProvider";

    public static boolean isEntityProvider(Class<?> clazz) {
        return clazz.getName().equals(TILE);
    }

    public static boolean isPartProvider(Class<?> clazz) {
        return clazz.getName().equals(PART);
    }
}
