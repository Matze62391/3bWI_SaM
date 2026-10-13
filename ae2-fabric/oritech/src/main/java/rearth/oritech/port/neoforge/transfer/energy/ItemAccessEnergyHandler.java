package rearth.oritech.port.neoforge.transfer.energy;

import net.minecraft.core.component.DataComponentType;

import rearth.oritech.port.neoforge.transfer.TransferPreconditions;
import rearth.oritech.port.neoforge.transfer.access.ItemAccess;
import rearth.oritech.port.neoforge.transfer.item.ItemResource;
import rearth.oritech.port.neoforge.transfer.transaction.TransactionContext;

/**
 * Energy stored in an integer data component of the item in an {@link ItemAccess}.
 */
public class ItemAccessEnergyHandler implements EnergyHandler {
    protected final ItemAccess itemAccess;
    protected final ItemResource validItem;
    protected final DataComponentType<Integer> energyComponent;
    protected final int capacity;
    protected final int maxInsert;
    protected final int maxExtract;

    public ItemAccessEnergyHandler(ItemAccess itemAccess, DataComponentType<Integer> energyComponent, int capacity) {
        this(itemAccess, energyComponent, capacity, capacity, capacity);
    }

    public ItemAccessEnergyHandler(ItemAccess itemAccess, DataComponentType<Integer> energyComponent, int capacity, int maxTransfer) {
        this(itemAccess, energyComponent, capacity, maxTransfer, maxTransfer);
    }

    public ItemAccessEnergyHandler(ItemAccess itemAccess, DataComponentType<Integer> energyComponent, int capacity, int maxInsert, int maxExtract) {
        this.itemAccess = itemAccess;
        this.validItem = itemAccess.getResource();
        this.energyComponent = energyComponent;
        this.capacity = capacity;
        this.maxInsert = maxInsert;
        this.maxExtract = maxExtract;
    }

    protected int getAmountFrom(ItemResource accessResource) {
        return Math.max(0, accessResource.getOrDefault(energyComponent, 0));
    }

    protected ItemResource update(ItemResource accessResource, int newAmount) {
        return accessResource.with(energyComponent, newAmount);
    }

    private boolean valid() {
        var current = itemAccess.getResource();
        return !current.isEmpty() && current.getItem() == validItem.getItem() && itemAccess.getAmount() > 0;
    }

    @Override
    public long getAmountAsLong() {
        return valid() ? getAmountFrom(itemAccess.getResource()) : 0;
    }

    @Override
    public long getCapacityAsLong() {
        return valid() ? capacity : 0;
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        if (!valid()) {
            return 0;
        }
        var count = itemAccess.getAmount();
        var resource = itemAccess.getResource();
        var current = getAmountFrom(resource);
        var perItem = Math.min(Math.min(amount / count, maxInsert), capacity - current);
        if (perItem <= 0) {
            return 0;
        }
        return itemAccess.exchange(update(resource, current + perItem), count, transaction) == count ? perItem * count : 0;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        if (!valid()) {
            return 0;
        }
        var count = itemAccess.getAmount();
        var resource = itemAccess.getResource();
        var current = getAmountFrom(resource);
        var perItem = Math.min(Math.min(amount / count, maxExtract), current);
        if (perItem <= 0) {
            return 0;
        }
        return itemAccess.exchange(update(resource, current - perItem), count, transaction) == count ? perItem * count : 0;
    }
}
