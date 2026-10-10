package net.pedroksl.advanced_ae.gui;

import net.minecraft.world.entity.player.Inventory;
import net.pedroksl.advanced_ae.common.definitions.AAEMenus;
import net.pedroksl.advanced_ae.common.helpers.QuantumCrafterWirelessTermMenuHost;

import appeng.menu.SlotSemantics;
import appeng.menu.ToolboxMenu;
import appeng.menu.slot.RestrictedInputSlot;

public class QuantumCrafterWirelessTermMenu extends QuantumCrafterTermMenu {

    private final QuantumCrafterWirelessTermMenuHost host;
    private final ToolboxMenu toolboxMenu;

    public QuantumCrafterWirelessTermMenu(int id, Inventory playerInventory, QuantumCrafterWirelessTermMenuHost host) {
        super(AAEMenus.QUANTUM_CRAFTER_WIRELESS_TERMINAL.get(), id, playerInventory, host, true);
        this.host = host;
        this.toolboxMenu = new ToolboxMenu(this);

        var upgrades = this.host.getUpgrades();
        for (int i = 0; i < upgrades.size(); i++) {
            var slot = new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.UPGRADES, upgrades, i);
            slot.setNotDraggable();
            addSlot(slot, SlotSemantics.UPGRADE);
        }
    }

    @Override
    public void broadcastChanges() {
        toolboxMenu.tick();
        super.broadcastChanges();
    }

    public QuantumCrafterWirelessTermMenuHost getHost() {
        return this.host;
    }

    public ToolboxMenu getToolbox() {
        return toolboxMenu;
    }
}
