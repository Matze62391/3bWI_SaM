package rearth.oritech.port.neoforge.transfer.fluid;

import net.minecraft.core.component.DataComponentType;

import rearth.oritech.port.neoforge.fluids.SimpleFluidContent;
import rearth.oritech.port.neoforge.transfer.ResourceHandler;
import rearth.oritech.port.neoforge.transfer.TransferPreconditions;
import rearth.oritech.port.neoforge.transfer.access.ItemAccess;
import rearth.oritech.port.neoforge.transfer.item.ItemResource;
import rearth.oritech.port.neoforge.transfer.transaction.TransactionContext;

/**
 * A fluid tank stored in a {@link SimpleFluidContent} data component of the item in an {@link ItemAccess}.
 */
public class ItemAccessFluidHandler implements ResourceHandler<FluidResource> {
    protected final ItemAccess itemAccess;
    protected final ItemResource validItem;
    protected final DataComponentType<SimpleFluidContent> component;
    protected final int capacity;

    public ItemAccessFluidHandler(ItemAccess itemAccess, DataComponentType<SimpleFluidContent> component, int capacity) {
        this.itemAccess = itemAccess;
        this.validItem = itemAccess.getResource();
        this.component = component;
        this.capacity = capacity;
    }

    protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
        return FluidResource.of(accessResource.getOrDefault(component, SimpleFluidContent.EMPTY).copy());
    }

    protected int getAmountFrom(ItemResource accessResource, int index) {
        return accessResource.getOrDefault(component, SimpleFluidContent.EMPTY).getAmount();
    }

    protected ItemResource update(ItemResource accessResource, int index, FluidResource newResource, int newAmount) {
        return accessResource.with(component, SimpleFluidContent.copyOf(newResource.toStack(newAmount)));
    }

    private boolean valid() {
        var current = itemAccess.getResource();
        return !current.isEmpty() && current.getItem() == validItem.getItem();
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(int index) {
        return valid() ? getResourceFrom(itemAccess.getResource(), index) : FluidResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        return valid() ? getAmountFrom(itemAccess.getResource(), index) : 0;
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return valid() ? getCapacity(index, resource) : 0;
    }

    protected int getCapacity(int index, FluidResource resource) {
        return capacity;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return true;
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (!valid() || !isValid(index, resource) || itemAccess.getAmount() != 1) {
            return 0;
        }
        var accessResource = itemAccess.getResource();
        var current = getResourceFrom(accessResource, index);
        var currentAmount = getAmountFrom(accessResource, index);
        if (currentAmount > 0 && !current.equals(resource)) {
            return 0;
        }
        var inserted = Math.min(amount, getCapacity(index, resource) - currentAmount);
        if (inserted <= 0) {
            return 0;
        }
        return itemAccess.exchange(update(accessResource, index, resource, currentAmount + inserted), 1, transaction) == 1 ? inserted : 0;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (!valid() || itemAccess.getAmount() != 1) {
            return 0;
        }
        var accessResource = itemAccess.getResource();
        var current = getResourceFrom(accessResource, index);
        var currentAmount = getAmountFrom(accessResource, index);
        if (currentAmount <= 0 || !current.equals(resource)) {
            return 0;
        }
        var extracted = Math.min(amount, currentAmount);
        var remaining = currentAmount - extracted;
        var updated = update(accessResource, index, remaining > 0 ? resource : FluidResource.EMPTY, remaining);
        return itemAccess.exchange(updated, 1, transaction) == 1 ? extracted : 0;
    }
}
