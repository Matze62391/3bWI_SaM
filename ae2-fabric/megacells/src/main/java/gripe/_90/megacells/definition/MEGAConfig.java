package gripe._90.megacells.definition;

/**
 * NeoForge's config spec only had the Applied Mekanistics option, which doesn't exist on Fabric.
 */
public final class MEGAConfig {
    public static final MEGAConfig CONFIG = new MEGAConfig();

    public boolean isSpentWasteAllowed() {
        return false;
    }
}
