package net.pedroksl.advanced_ae.gui;

import net.minecraft.world.entity.player.Inventory;
import net.pedroksl.advanced_ae.common.definitions.AAEMenus;
import net.pedroksl.advanced_ae.common.helpers.QuantumCrafterWirelessTermMenuHost;

import appeng.menu.SlotSemantics;
import appeng.menu.ToolboxMenu;
import appeng.menu.slot.RestrictedInputSlot;

import de.mari_023.ae2wtlib.api.gui.AE2wtlibSlotSemantics;
import de.mari_023.ae2wtlib.api.terminal.ItemWUT;
import de.mari_023.ae2wtlib.api.terminal.WTMenuHost;

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

        addSlot(
                new RestrictedInputSlot(
                        RestrictedInputSlot.PlacableItemType.QE_SINGULARITY,
                        this.host.getSubInventory(WTMenuHost.INV_SINGULARITY),
                        0),
                AE2wtlibSlotSemantics.SINGULARITY);
    }

    public boolean isWUT() {
        return this.host.getItemStack().getItem() instanceof ItemWUT;
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
