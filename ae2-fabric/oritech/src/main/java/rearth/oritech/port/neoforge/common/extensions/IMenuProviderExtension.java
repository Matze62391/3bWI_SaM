package rearth.oritech.port.neoforge.common.extensions;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;

public interface IMenuProviderExtension {
    default boolean shouldTriggerClientSideContainerClosingOnOpen() {
        return true;
    }

    /** Writes extra data that is sent to the client with the menu opening. */
    default void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
    }
}
