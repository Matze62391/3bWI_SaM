package rearth.oritech.port.neoforge.transfer.item;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedSlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import rearth.oritech.port.neoforge.capabilities.FabricAdapters;
import rearth.oritech.port.neoforge.transfer.ResourceHandler;

/** The inventory of a player as resource handlers. */
public final class PlayerInventoryWrapper {
    private final PlayerInventoryStorage storage;

    private PlayerInventoryWrapper(PlayerInventoryStorage storage) {
        this.storage = storage;
    }

    public static PlayerInventoryWrapper of(Player player) {
        return new PlayerInventoryWrapper(PlayerInventoryStorage.of(player));
    }

    public static PlayerInventoryWrapper of(Inventory inventory) {
        return new PlayerInventoryWrapper(PlayerInventoryStorage.of(inventory));
    }

    public ResourceHandler<ItemResource> getMainSlots() {
        var slots = storage.getSlots().subList(0, Inventory.INVENTORY_SIZE);
        return FabricAdapters.fromFabricItems(new CombinedSlottedStorage<ItemVariant, SingleSlotStorage<ItemVariant>>(slots));
    }

    public ResourceHandler<ItemResource> getAll() {
        return FabricAdapters.fromFabricItems(storage);
    }
}
