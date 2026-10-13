package rearth.oritech.port.neoforge.common.extensions;

import io.netty.buffer.Unpooled;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import rearth.oritech.port.neoforge.network.IContainerFactory;

public interface IMenuTypeExtension {
    /**
     * A menu type whose client-side menu is created from the extra data written when opening it (see
     * {@link rearth.oritech.port.neoforge.common.MenuHelper}).
     */
    static <T extends AbstractContainerMenu> MenuType<T> create(IContainerFactory<T> factory) {
        return new ExtendedMenuType<T, byte[]>((windowId, inventory, data) -> factory.create(windowId, inventory,
                new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), inventory.player.registryAccess())),
                ByteBufCodecs.BYTE_ARRAY);
    }
}
