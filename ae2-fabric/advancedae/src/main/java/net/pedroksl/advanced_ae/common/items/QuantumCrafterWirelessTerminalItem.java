package net.pedroksl.advanced_ae.common.items;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.pedroksl.advanced_ae.api.AAESettings;
import net.pedroksl.advanced_ae.api.ShowQuantumCrafters;
import net.pedroksl.advanced_ae.common.definitions.AAEMenus;
import net.pedroksl.advanced_ae.common.helpers.QuantumCrafterWirelessTermMenuHost;

import appeng.api.util.IConfigManager;
import appeng.core.AEConfig;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.locator.ItemMenuHostLocator;

/**
 * Wireless version of the quantum crafter terminal. Upstream builds it on ae2wtlib, which doesn't exist for Fabric
 * 26.3, so it uses AE2's own wireless terminal instead: link it in a wireless access point and it works in range of
 * the network.
 */
public class QuantumCrafterWirelessTerminalItem extends WirelessTerminalItem {

    public QuantumCrafterWirelessTerminalItem(Properties p) {
        super(AEConfig.instance().getWirelessTerminalBattery(), p.stacksTo(1));
    }

    @Override
    public IConfigManager getConfigManager(Supplier<ItemStack> target) {
        return IConfigManager.builder(target)
                .registerSetting(AAESettings.TERMINAL_SHOW_QUANTUM_CRAFTERS, ShowQuantumCrafters.VISIBLE)
                .build();
    }

    @Override
    public MenuType<?> getMenuType() {
        return AAEMenus.QUANTUM_CRAFTER_WIRELESS_TERMINAL.get();
    }

    @Nullable
    @Override
    public QuantumCrafterWirelessTermMenuHost getMenuHost(
            Player player, ItemMenuHostLocator locator, @Nullable BlockHitResult hitResult) {
        return new QuantumCrafterWirelessTermMenuHost(
                this, player, locator, (p, subMenu) -> openFromInventory(p, locator, true));
    }
}
