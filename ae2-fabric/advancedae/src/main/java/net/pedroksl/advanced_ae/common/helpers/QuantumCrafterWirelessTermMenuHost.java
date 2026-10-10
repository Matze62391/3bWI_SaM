package net.pedroksl.advanced_ae.common.helpers;

import java.util.function.BiConsumer;

import net.minecraft.world.entity.player.Player;
import net.pedroksl.advanced_ae.api.IQuantumCrafterTermMenuHost;
import net.pedroksl.advanced_ae.common.items.QuantumCrafterWirelessTerminalItem;

import appeng.helpers.WirelessTerminalMenuHost;
import appeng.menu.ISubMenu;
import appeng.menu.locator.ItemMenuHostLocator;

public class QuantumCrafterWirelessTermMenuHost extends WirelessTerminalMenuHost<QuantumCrafterWirelessTerminalItem>
        implements IQuantumCrafterTermMenuHost {
    public QuantumCrafterWirelessTermMenuHost(
            QuantumCrafterWirelessTerminalItem item,
            Player player,
            ItemMenuHostLocator locator,
            BiConsumer<Player, ISubMenu> returnToMainMenu) {
        super(item, player, locator, returnToMainMenu);
    }
}
