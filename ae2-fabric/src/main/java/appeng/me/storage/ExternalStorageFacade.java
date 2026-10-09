package appeng.me.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.primitives.Ints;

import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.core.localization.GuiText;
import appeng.util.Platform;

/**
 * Adapts external platform storage to behave like an {@link MEStorage}.
 */
public abstract class ExternalStorageFacade implements MEStorage {
    /**
     * Clamp reported values to avoid overflows when amounts get too close to Long.MAX_VALUE.
     */
    private static final long MAX_REPORTED_AMOUNT = 1L << 42;

    @Nullable
    private Runnable changeListener;

    protected boolean extractableOnly;

    public void setChangeListener(@Nullable Runnable listener) {
        this.changeListener = listener;
    }

    public abstract int getSlots();

    @Override
    public int getEstimatedStackCount() {
        return getSlots();
    }

    @Nullable
    public abstract GenericStack getStackInSlot(int slot);

    public abstract AEKeyType getKeyType();

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        var inserted = insertExternal(what, amount, mode);
        if (inserted > 0 && mode == Actionable.MODULATE) {
            if (this.changeListener != null) {
                this.changeListener.run();
            }
        }
        return inserted;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        var extracted = extractExternal(what, amount, mode);
        if (extracted > 0 && mode == Actionable.MODULATE) {
            if (this.changeListener != null) {
                this.changeListener.run();
            }
        }
        return extracted;
    }

    @Override
    public Component getDescription() {
        return GuiText.ExternalStorage.text(AEKeyType.fluids().getDescription());
    }

    protected abstract long insertExternal(AEKey what, long amount, Actionable mode);

    protected abstract long extractExternal(AEKey what, long amount, Actionable mode);

    public abstract boolean containsAnyFuzzy(Set<AEKey> keys);

    public static ExternalStorageFacade ofFluidHandler(Storage<FluidVariant> handler) {
        return new FluidHandlerFacade(handler);
    }

    public static ExternalStorageFacade ofItemHandler(Storage<ItemVariant> handler) {
        return new ItemHandlerFacade(handler);
    }

    public void setExtractableOnly(boolean extractableOnly) {
        this.extractableOnly = extractableOnly;
    }

    private static abstract class ResourceHandlerFacade<R extends TransferVariant<?>, K extends AEKey>
            extends ExternalStorageFacade {
        protected final Storage<R> handler;
        /**
         * Fabric storages are not slot based. {@link #getSlots()} takes a snapshot of the storage's views, which
         * {@link #getStackInSlot(int)} then indexes into.
         */
        private List<StorageView<R>> views = List.of();

        public ResourceHandlerFacade(Storage<R> handler) {
            this.handler = handler;
        }

        @Override
        public int getSlots() {
            var result = new ArrayList<StorageView<R>>();
            for (var view : handler) {
                result.add(view);
            }
            views = result;
            return result.size();
        }

        @Nullable
        @Override
        public GenericStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= views.size()) {
                return null;
            }
            var view = views.get(slot);
            if (view.isResourceBlank()) {
                return null;
            }
            K key = toKey(view.getResource());
            return key == null ? null : new GenericStack(key, view.getAmount());
        }

        @Override
        public long insertExternal(AEKey what, long amount, Actionable mode) {
            var resource = toResource(what);
            if (resource == null || amount <= 0) {
                return 0;
            }

            try (var tx = Platform.openOrJoinTx()) {
                var inserted = handler.insert(resource, amount, tx);
                if (!mode.isSimulate()) {
                    tx.commit();
                }
                return inserted;
            }
        }

        @Override
        public long extractExternal(AEKey what, long amount, Actionable mode) {
            var resource = toResource(what);
            if (resource == null || amount <= 0) {
                return 0;
            }

            try (var tx = Platform.openOrJoinTx()) {
                var extracted = handler.extract(resource, amount, tx);
                if (!mode.isSimulate()) {
                    tx.commit();
                }
                return extracted;
            }
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            for (var view : handler) {
                if (view.isResourceBlank()) {
                    continue;
                }
                var resource = view.getResource();
                long amount = Math.min(view.getAmount(), MAX_REPORTED_AMOUNT);
                if (amount <= 0) {
                    continue;
                }

                // Skip resources that cannot be extracted if that filter was enabled
                if (extractableOnly) {
                    try (var tx = Platform.openOrJoinTx()) {
                        var extracted = view.extract(resource, 1, tx);
                        // Try again in case the storage only allows extracting the resource in its entirety
                        // (i.e. cauldrons)
                        if (extracted == 0) {
                            extracted = view.extract(resource, view.getAmount(), tx);
                        }
                        if (extracted == 0) {
                            continue; // Skip unextractable views
                        }
                    }
                }

                var key = toKey(resource);
                if (key != null) {
                    out.add(key, amount);
                }
            }
        }

        @Override
        public boolean containsAnyFuzzy(Set<AEKey> keys) {
            for (var view : handler) {
                if (view.isResourceBlank()) {
                    continue;
                }
                var what = toKey(view.getResource());
                if (what != null && keys.contains(what.dropSecondary())) {
                    return true;
                }
            }
            return false;
        }

        @Nullable
        protected abstract K toKey(R resource);

        @Nullable
        protected abstract R toResource(AEKey key);
    }

    private static class ItemHandlerFacade extends ResourceHandlerFacade<ItemVariant, AEItemKey> {
        public ItemHandlerFacade(Storage<ItemVariant> handler) {
            super(handler);
        }

        @Override
        public AEKeyType getKeyType() {
            return AEKeyType.items();
        }

        @Override
        protected @Nullable AEItemKey toKey(ItemVariant resource) {
            return AEItemKey.of(resource);
        }

        @Override
        protected @Nullable ItemVariant toResource(AEKey key) {
            return (key instanceof AEItemKey itemKey) ? itemKey.toVariant() : null;
        }
    }

    private static class FluidHandlerFacade extends ResourceHandlerFacade<FluidVariant, AEFluidKey> {
        public FluidHandlerFacade(Storage<FluidVariant> handler) {
            super(handler);
        }

        @Override
        public AEKeyType getKeyType() {
            return AEKeyType.fluids();
        }

        @Override
        protected @Nullable AEFluidKey toKey(FluidVariant resource) {
            return AEFluidKey.of(resource);
        }

        @Override
        protected @Nullable FluidVariant toResource(AEKey key) {
            return (key instanceof AEFluidKey fluidKey) ? fluidKey.toVariant() : null;
        }
    }
}
