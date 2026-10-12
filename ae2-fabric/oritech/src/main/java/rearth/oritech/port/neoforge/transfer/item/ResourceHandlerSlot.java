package rearth.oritech.port.neoforge.transfer.item;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import rearth.oritech.port.neoforge.transfer.IndexModifier;
import rearth.oritech.port.neoforge.transfer.ResourceHandler;
import rearth.oritech.port.neoforge.transfer.transaction.Transaction;

/**
 * A menu slot that shows and changes one index of an item resource handler.
 */
public class ResourceHandlerSlot extends Slot {
    private static final SimpleContainer EMPTY = new SimpleContainer(0);

    private final ResourceHandler<ItemResource> handler;
    private final IndexModifier<ItemResource> slotModifier;
    private final int index;

    public ResourceHandlerSlot(ResourceHandler<ItemResource> handler, IndexModifier<ItemResource> slotModifier, int handlerSlot, int xPosition, int yPosition) {
        super(EMPTY, handlerSlot, xPosition, yPosition);
        this.handler = handler;
        this.slotModifier = slotModifier;
        this.index = handlerSlot;
    }

    public int getSlotIndex() {
        return index;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !stack.isEmpty() && handler.isValid(index, ItemResource.of(stack));
    }

    @Override
    public ItemStack getItem() {
        return handler.getResource(index).toStack(handler.getAmountAsInt(index));
    }

    @Override
    public boolean hasItem() {
        return !handler.getResource(index).isEmpty() && handler.getAmountAsLong(index) > 0;
    }

    @Override
    public void set(ItemStack stack) {
        slotModifier.set(index, ItemResource.of(stack), stack.getCount());
        setChanged();
    }

    @Override
    public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
        set(newStack);
    }

    @Override
    public ItemStack remove(int amount) {
        var current = getItem();
        var removed = current.split(amount);
        set(current);
        return removed;
    }

    @Override
    public void setChanged() {
    }

    @Override
    public void onQuickCraft(ItemStack oldStack, ItemStack newStack) {
    }

    @Override
    public int getMaxStackSize() {
        return handler.getCapacityAsInt(index, ItemResource.EMPTY);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return handler.getCapacityAsInt(index, ItemResource.of(stack));
    }

    @Override
    public boolean mayPickup(Player player) {
        var resource = handler.getResource(index);
        if (resource.isEmpty()) {
            return false;
        }
        try (var tx = Transaction.openRoot()) {
            return handler.extract(index, resource, 1, tx) == 1;
        }
    }

    public ResourceHandler<ItemResource> getResourceHandler() {
        return handler;
    }

    public boolean isSameInventory(Slot other) {
        return other instanceof ResourceHandlerSlot slot && slot.handler == handler;
    }
}
