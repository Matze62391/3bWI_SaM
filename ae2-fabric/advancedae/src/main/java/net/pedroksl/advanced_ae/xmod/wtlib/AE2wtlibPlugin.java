package net.pedroksl.advanced_ae.xmod.wtlib;

import net.pedroksl.advanced_ae.common.definitions.AAEItems;
import net.pedroksl.advanced_ae.common.definitions.AAEMenus;
import net.pedroksl.advanced_ae.common.helpers.QuantumCrafterWirelessTermMenuHost;

import de.mari_023.ae2wtlib.api.gui.Icon;
import de.mari_023.ae2wtlib.api.registration.AddTerminalEvent;

/**
 * Makes the wireless quantum crafter terminal an ae2wtlib terminal, so it can be combined into the universal terminal.
 * ae2wtlib also registers its hotkey ({@code wireless_quantum_crafter_terminal}).
 */
public class AE2wtlibPlugin {

    public static final String TERMINAL_ID = "quantum_crafter";

    public static void registerTerminal() {
        AddTerminalEvent.register(e -> e.builder(
                        TERMINAL_ID,
                        QuantumCrafterWirelessTermMenuHost::new,
                        AAEMenus.QUANTUM_CRAFTER_WIRELESS_TERMINAL.get(),
                        AAEItems.QUANTUM_CRAFTER_WIRELESS_TERMINAL.asItem(),
                        Icon.PATTERN_ACCESS)
                .addTerminal());
    }
}
