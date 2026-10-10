package net.pedroksl.advanced_ae.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.pedroksl.advanced_ae.gui.QuantumCrafterWirelessTermMenu;

import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ToolboxPanel;
import appeng.client.gui.widgets.UpgradesPanel;
import appeng.menu.SlotSemantics;

public class QuantumCrafterWirelessTermScreen extends QuantumCrafterTermScreen<QuantumCrafterWirelessTermMenu> {

    public QuantumCrafterWirelessTermScreen(
            QuantumCrafterWirelessTermMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        widgets.add("upgrades", new UpgradesPanel(menu.getSlots(SlotSemantics.UPGRADE), menu.getHost()));
        if (menu.getToolbox().isPresent()) {
            widgets.add("toolbox", new ToolboxPanel(style, menu.getToolbox().getName()));
        }
    }
}
