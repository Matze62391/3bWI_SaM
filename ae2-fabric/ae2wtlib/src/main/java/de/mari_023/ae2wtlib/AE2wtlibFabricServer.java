package de.mari_023.ae2wtlib;

import net.fabricmc.api.DedicatedServerModInitializer;

/**
 * Registers the terminals on a dedicated server, once all mods are initialized.
 */
public final class AE2wtlibFabricServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        AE2wtlibFabric.finishTerminalRegistration();
    }
}
