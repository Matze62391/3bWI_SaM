package rearth.oritech.port.neoforge.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

@FunctionalInterface
public interface IContainerFactory<T extends AbstractContainerMenu> {
    T create(int windowId, Inventory inv, RegistryFriendlyByteBuf data);
}
