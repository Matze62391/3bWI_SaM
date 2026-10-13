package rearth.oritech.port.neoforge.transfer.access;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import rearth.oritech.port.neoforge.capabilities.ItemCapability;
import rearth.oritech.port.neoforge.transfer.ResourceHandler;
import rearth.oritech.port.neoforge.transfer.item.ItemResource;
import rearth.oritech.port.neoforge.transfer.transaction.Transaction;
import rearth.oritech.port.neoforge.transfer.transaction.TransactionContext;

/**
 * Access to the item(s) in some location (a stack, a player's hand, an inventory slot), through which item
 * capabilities can change the item.
 */
public interface ItemAccess {
    static ItemAccess forPlayerInteraction(Player player, InteractionHand hand) {
        return new ItemAccessImpl.PlayerHand(player, hand);
    }

    static ItemAccess forInfiniteMaterials(Player player, ItemStack contents) {
        return new ItemAccessImpl.Infinite(contents);
    }

    static ItemAccess forPlayerCursor(Player player, AbstractContainerMenu menu) {
        return new ItemAccessImpl.Cursor(menu);
    }

    static ItemAccess forPlayerSlot(Player player, int slot) {
        return new ItemAccessImpl.PlayerSlot(player, slot);
    }

    static ItemAccess forHandlerIndex(ResourceHandler<ItemResource> handler, int index) {
        return new ItemAccessImpl.HandlerIndex(handler, index);
    }

    static ItemAccess forHandlerIndexStrict(ResourceHandler<ItemResource> handler, int index) {
        return new ItemAccessImpl.HandlerIndex(handler, index);
    }

    static ItemAccess forStack(ItemStack stack) {
        return new ItemAccessImpl.Stack(stack);
    }

    default ItemAccess oneByOne() {
        return this;
    }

    @Nullable
    default <T> T getCapability(ItemCapability<T, ItemAccess> capability) {
        return capability.getCapability(this);
    }

    ItemResource getResource();

    int getAmount();

    int insert(ItemResource resource, int amount, TransactionContext transaction);

    int extract(ItemResource resource, int amount, TransactionContext transaction);

    default int exchange(ItemResource newResource, int amount, @Nullable TransactionContext transaction) {
        var current = getResource();
        try (var tx = Transaction.open(transaction)) {
            var extracted = extract(current, amount, tx);
            if (extracted != amount) {
                return 0;
            }
            if (newResource.isEmpty() || insert(newResource, amount, tx) == amount) {
                tx.commit();
                return amount;
            }
        }
        return 0;
    }
}
