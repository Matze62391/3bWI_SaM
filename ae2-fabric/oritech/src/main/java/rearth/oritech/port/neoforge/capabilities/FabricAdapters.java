package rearth.oritech.port.neoforge.capabilities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import team.reborn.energy.api.EnergyStorage;

import rearth.oritech.port.neoforge.transfer.ResourceHandler;
import rearth.oritech.port.neoforge.transfer.access.ItemAccess;
import rearth.oritech.port.neoforge.transfer.energy.EnergyHandler;
import rearth.oritech.port.neoforge.transfer.fluid.FluidResource;
import rearth.oritech.port.neoforge.transfer.item.ItemResource;
import rearth.oritech.port.neoforge.transfer.resource.Resource;

/**
 * Converts between Oritech's handlers and Fabric's storages. Fluids are millibuckets in Oritech and droplets in
 * Fabric (81 droplets per millibucket); amounts that are not whole millibuckets are not moved.
 */
public final class FabricAdapters {
    public static final long DROPLETS_PER_MB = 81;

    private FabricAdapters() {}

    private static rearth.oritech.port.neoforge.transfer.transaction.TransactionContext tx(TransactionContext fabric) {
        return rearth.oritech.port.neoforge.transfer.transaction.TransactionContext.of(fabric);
    }

    private static int clamp(long value) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, value));
    }

    // ---------------------------------------------------------------- energy

    @Nullable
    public static EnergyStorage toFabric(@Nullable EnergyHandler handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof FromFabricEnergy wrapped) {
            return wrapped.storage;
        }
        return new ToFabricEnergy(handler);
    }

    @Nullable
    public static EnergyHandler fromFabric(@Nullable EnergyStorage storage) {
        if (storage == null) {
            return null;
        }
        if (storage instanceof ToFabricEnergy wrapped) {
            return wrapped.handler;
        }
        return new FromFabricEnergy(storage);
    }

    private record ToFabricEnergy(EnergyHandler handler) implements EnergyStorage {
        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            return handler.insert(clamp(maxAmount), tx(transaction));
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            return handler.extract(clamp(maxAmount), tx(transaction));
        }

        @Override
        public long getAmount() {
            return handler.getAmountAsLong();
        }

        @Override
        public long getCapacity() {
            return handler.getCapacityAsLong();
        }
    }

    private record FromFabricEnergy(EnergyStorage storage) implements EnergyHandler {
        @Override
        public long getAmountAsLong() {
            return storage.getAmount();
        }

        @Override
        public long getCapacityAsLong() {
            return storage.getCapacity();
        }

        @Override
        public int insert(int amount, rearth.oritech.port.neoforge.transfer.transaction.TransactionContext transaction) {
            return storage.supportsInsertion() ? clamp(storage.insert(amount, transaction.fabric())) : 0;
        }

        @Override
        public int extract(int amount, rearth.oritech.port.neoforge.transfer.transaction.TransactionContext transaction) {
            return storage.supportsExtraction() ? clamp(storage.extract(amount, transaction.fabric())) : 0;
        }
    }

    // ---------------------------------------------------------------- items and fluids

    /**
     * How a resource type maps to a Fabric variant, and the unit conversion.
     */
    private interface Kind<R extends Resource, V extends TransferVariant<?>> {
        V toVariant(R resource);

        R fromVariant(V variant);

        long toFabric(long amount);

        long fromFabric(long amount);

        R empty();
    }

    private static final Kind<ItemResource, ItemVariant> ITEMS = new Kind<>() {
        @Override
        public ItemVariant toVariant(ItemResource resource) {
            return resource.toVariant();
        }

        @Override
        public ItemResource fromVariant(ItemVariant variant) {
            return ItemResource.of(variant);
        }

        @Override
        public long toFabric(long amount) {
            return amount;
        }

        @Override
        public long fromFabric(long amount) {
            return amount;
        }

        @Override
        public ItemResource empty() {
            return ItemResource.EMPTY;
        }
    };

    private static final Kind<FluidResource, FluidVariant> FLUIDS = new Kind<>() {
        @Override
        public FluidVariant toVariant(FluidResource resource) {
            return resource.toVariant();
        }

        @Override
        public FluidResource fromVariant(FluidVariant variant) {
            return FluidResource.of(variant);
        }

        @Override
        public long toFabric(long amount) {
            return amount * DROPLETS_PER_MB;
        }

        @Override
        public long fromFabric(long amount) {
            return amount / DROPLETS_PER_MB;
        }

        @Override
        public FluidResource empty() {
            return FluidResource.EMPTY;
        }
    };

    @Nullable
    public static Storage<ItemVariant> toFabricItems(@Nullable ResourceHandler<ItemResource> handler) {
        return toFabric(handler, ITEMS);
    }

    @Nullable
    public static Storage<FluidVariant> toFabricFluids(@Nullable ResourceHandler<FluidResource> handler) {
        return toFabric(handler, FLUIDS);
    }

    @Nullable
    public static ResourceHandler<ItemResource> fromFabricItems(@Nullable Storage<ItemVariant> storage) {
        return fromFabric(storage, ITEMS);
    }

    @Nullable
    public static ResourceHandler<FluidResource> fromFabricFluids(@Nullable Storage<FluidVariant> storage) {
        return fromFabric(storage, FLUIDS);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <R extends Resource, V extends TransferVariant<?>> Storage<V> toFabric(@Nullable ResourceHandler<R> handler, Kind<R, V> kind) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof FromFabric<?, ?> wrapped && wrapped.kind == kind) {
            return (Storage<V>) wrapped.storage;
        }
        return new ToFabric<>(handler, kind);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <R extends Resource, V extends TransferVariant<?>> ResourceHandler<R> fromFabric(@Nullable Storage<V> storage, Kind<R, V> kind) {
        if (storage == null) {
            return null;
        }
        if (storage instanceof ToFabric<?, ?> wrapped && wrapped.kind == kind) {
            return (ResourceHandler<R>) wrapped.handler;
        }
        return new FromFabric<>(storage, kind);
    }

    private static final class ToFabric<R extends Resource, V extends TransferVariant<?>> implements SlottedStorage<V> {
        private final ResourceHandler<R> handler;
        private final Kind<R, V> kind;

        private ToFabric(ResourceHandler<R> handler, Kind<R, V> kind) {
            this.handler = handler;
            this.kind = kind;
        }

        @Override
        public long insert(V resource, long maxAmount, TransactionContext transaction) {
            var amount = clamp(kind.fromFabric(maxAmount));
            if (resource.isBlank() || amount <= 0) {
                return 0;
            }
            return kind.toFabric(handler.insert(kind.fromVariant(resource), amount, tx(transaction)));
        }

        @Override
        public long extract(V resource, long maxAmount, TransactionContext transaction) {
            var amount = clamp(kind.fromFabric(maxAmount));
            if (resource.isBlank() || amount <= 0) {
                return 0;
            }
            return kind.toFabric(handler.extract(kind.fromVariant(resource), amount, tx(transaction)));
        }

        @Override
        public int getSlotCount() {
            return handler.size();
        }

        @Override
        public SingleSlotStorage<V> getSlot(int slot) {
            return new Slot(slot);
        }

        @Override
        public Iterator<StorageView<V>> iterator() {
            var views = new ArrayList<StorageView<V>>(handler.size());
            for (var i = 0; i < handler.size(); i++) {
                views.add(new Slot(i));
            }
            return views.iterator();
        }

        private final class Slot implements SingleSlotStorage<V> {
            private final int index;

            private Slot(int index) {
                this.index = index;
            }

            @Override
            public long insert(V resource, long maxAmount, TransactionContext transaction) {
                var amount = clamp(kind.fromFabric(maxAmount));
                if (resource.isBlank() || amount <= 0) {
                    return 0;
                }
                return kind.toFabric(handler.insert(index, kind.fromVariant(resource), amount, tx(transaction)));
            }

            @Override
            public long extract(V resource, long maxAmount, TransactionContext transaction) {
                var amount = clamp(kind.fromFabric(maxAmount));
                if (resource.isBlank() || amount <= 0) {
                    return 0;
                }
                return kind.toFabric(handler.extract(index, kind.fromVariant(resource), amount, tx(transaction)));
            }

            @Override
            public boolean isResourceBlank() {
                return handler.getResource(index).isEmpty();
            }

            @Override
            public V getResource() {
                return kind.toVariant(handler.getResource(index));
            }

            @Override
            public long getAmount() {
                return kind.toFabric(handler.getAmountAsLong(index));
            }

            @Override
            public long getCapacity() {
                return kind.toFabric(handler.getCapacityAsLong(index, handler.getResource(index)));
            }
        }
    }

    private static final class FromFabric<R extends Resource, V extends TransferVariant<?>> implements ResourceHandler<R> {
        private final Storage<V> storage;
        private final Kind<R, V> kind;

        private FromFabric(Storage<V> storage, Kind<R, V> kind) {
            this.storage = storage;
            this.kind = kind;
        }

        private List<StorageView<V>> views() {
            if (storage instanceof SlottedStorage<V> slotted) {
                return new ArrayList<>(slotted.getSlots());
            }
            var views = new ArrayList<StorageView<V>>();
            for (var view : storage) {
                views.add(view);
            }
            return views;
        }

        @Override
        public int size() {
            return views().size();
        }

        @Override
        public R getResource(int index) {
            var views = views();
            return index < views.size() ? kind.fromVariant(views.get(index).getResource()) : kind.empty();
        }

        @Override
        public long getAmountAsLong(int index) {
            var views = views();
            return index < views.size() ? kind.fromFabric(views.get(index).getAmount()) : 0;
        }

        @Override
        public long getCapacityAsLong(int index, R resource) {
            var views = views();
            return index < views.size() ? kind.fromFabric(views.get(index).getCapacity()) : 0;
        }

        @Override
        public boolean isValid(int index, R resource) {
            return storage.supportsInsertion();
        }

        @Override
        public int insert(int index, R resource, int amount, rearth.oritech.port.neoforge.transfer.transaction.TransactionContext transaction) {
            if (storage instanceof SlottedStorage<V> slotted && index < slotted.getSlotCount()) {
                return clamp(kind.fromFabric(slotted.getSlot(index).insert(kind.toVariant(resource), kind.toFabric(amount), transaction.fabric())));
            }
            return insert(resource, amount, transaction);
        }

        @Override
        public int insert(R resource, int amount, rearth.oritech.port.neoforge.transfer.transaction.TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0 || !storage.supportsInsertion()) {
                return 0;
            }
            return clamp(kind.fromFabric(storage.insert(kind.toVariant(resource), kind.toFabric(amount), transaction.fabric())));
        }

        @Override
        public int extract(int index, R resource, int amount, rearth.oritech.port.neoforge.transfer.transaction.TransactionContext transaction) {
            var views = views();
            if (index < views.size()) {
                return clamp(kind.fromFabric(views.get(index).extract(kind.toVariant(resource), kind.toFabric(amount), transaction.fabric())));
            }
            return 0;
        }

        @Override
        public int extract(R resource, int amount, rearth.oritech.port.neoforge.transfer.transaction.TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0 || !storage.supportsExtraction()) {
                return 0;
            }
            return clamp(kind.fromFabric(storage.extract(kind.toVariant(resource), kind.toFabric(amount), transaction.fabric())));
        }
    }

    // ---------------------------------------------------------------- item access

    /**
     * An Oritech item access on a Fabric item context (used when other mods query Oritech's items).
     */
    public static ItemAccess fromFabric(ContainerItemContext context) {
        return new ItemAccess() {
            @Override
            public ItemResource getResource() {
                return ItemResource.of(context.getItemVariant());
            }

            @Override
            public int getAmount() {
                return clamp(context.getAmount());
            }

            @Override
            public int insert(ItemResource resource, int amount, rearth.oritech.port.neoforge.transfer.transaction.TransactionContext transaction) {
                return clamp(context.insert(resource.toVariant(), amount, transaction.fabric()));
            }

            @Override
            public int extract(ItemResource resource, int amount, rearth.oritech.port.neoforge.transfer.transaction.TransactionContext transaction) {
                return clamp(context.extract(resource.toVariant(), amount, transaction.fabric()));
            }

            @Override
            public int exchange(ItemResource newResource, int amount, @Nullable rearth.oritech.port.neoforge.transfer.transaction.TransactionContext transaction) {
                try (var nested = rearth.oritech.port.neoforge.transfer.transaction.Transaction.open(transaction)) {
                    var exchanged = clamp(context.exchange(newResource.toVariant(), amount, nested.fabric()));
                    nested.commit();
                    return exchanged;
                }
            }
        };
    }

    /**
     * A Fabric item context on an Oritech item access (used when Oritech queries other mods' items).
     */
    public static ContainerItemContext toFabric(ItemAccess access) {
        SingleSlotStorage<ItemVariant> slot = new SingleSlotStorage<>() {
            @Override
            public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
                return access.insert(ItemResource.of(resource), clamp(maxAmount), tx(transaction));
            }

            @Override
            public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
                return access.extract(ItemResource.of(resource), clamp(maxAmount), tx(transaction));
            }

            @Override
            public boolean isResourceBlank() {
                return access.getResource().isEmpty();
            }

            @Override
            public ItemVariant getResource() {
                return access.getResource().toVariant();
            }

            @Override
            public long getAmount() {
                return access.getAmount();
            }

            @Override
            public long getCapacity() {
                return access.getResource().isEmpty() ? 64 : access.getResource().getMaxStackSize();
            }
        };
        return new ContainerItemContext() {
            @Override
            public SingleSlotStorage<ItemVariant> getMainSlot() {
                return slot;
            }

            @Override
            public long insertOverflow(ItemVariant itemVariant, long maxAmount, TransactionContext transactionContext) {
                return 0;
            }

            @Override
            public List<SingleSlotStorage<ItemVariant>> getAdditionalSlots() {
                return List.of();
            }
        };
    }
}
