package de.mari_023.ae2wtlib.wct;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import appeng.api.inventories.InternalInventory;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;

/**
 * PlayerInternalInventory returns the wrong size, so it doesn't work for the armor and offhand (what we actually care about)
 * @param playerInventory the Inventory to wrap
 */
public record WrappedPlayerInventory(Inventory playerInventory) implements InternalInventory {
    @Override
    public Storage<ItemVariant> toStorage() {
        return PlayerInventoryStorage.of(playerInventory);
    }

    @Override
    public int size() {
        return playerInventory.getContainerSize();
    }

    @Override
    public ItemStack getStackInSlot(int slotIndex) {
        return switch (slotIndex) {
            case 36, 37, 38, 39, Inventory.SLOT_OFFHAND -> playerInventory.getItem(slotIndex);
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public void setItemDirect(int slotIndex, ItemStack stack) {
        playerInventory.setItem(slotIndex, stack);
    }
}
