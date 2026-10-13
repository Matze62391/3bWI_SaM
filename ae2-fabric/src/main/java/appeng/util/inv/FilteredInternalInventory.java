/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2017, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.util.inv;

import java.util.Objects;

import com.google.common.primitives.Ints;

import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import appeng.api.inventories.BaseInternalInventory;
import appeng.api.inventories.InternalInventory;
import appeng.util.inv.filter.IAEItemFilter;
import appeng.util.transfer.IndexedStorage;

public class FilteredInternalInventory extends BaseInternalInventory {
    private final InternalInventory delegate;
    private final IAEItemFilter filter;

    public FilteredInternalInventory(InternalInventory delegate, IAEItemFilter filter) {
        this.delegate = Objects.requireNonNull(delegate);
        this.filter = Objects.requireNonNull(filter);
    }

    @Override
    public void setItemDirect(int slot, ItemStack stack) {
        delegate.setItemDirect(slot, stack);
    }

    @Override
    public int size() {
        return this.delegate.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.delegate.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!this.filter.allowInsert(this.delegate, slot, stack)) {
            return stack;
        }

        return this.delegate.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!this.filter.allowExtract(this.delegate, slot, amount)) {
            return ItemStack.EMPTY;
        }

        return this.delegate.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return this.delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (!this.filter.allowInsert(this.delegate, slot, stack)) {
            return false;
        }
        return this.delegate.isItemValid(slot, stack);
    }

    @Override
    public void sendChangeNotification(int slot) {
        delegate.sendChangeNotification(slot);
    }
    @Override
    protected Storage<ItemVariant> createStorage() {
        // Pass transactions through to the delegate if it supports index-based access
        if (delegate.toStorage() instanceof IndexedStorage<ItemVariant> indexed) {
            return new FilteringStorage(indexed);
        }
        return super.createStorage();
    }

    private class FilteringStorage implements IndexedStorage<ItemVariant> {
        private final IndexedStorage<ItemVariant> delegateStorage;

        FilteringStorage(IndexedStorage<ItemVariant> delegateStorage) {
            this.delegateStorage = delegateStorage;
        }

        @Override
        public int size() {
            return delegateStorage.size();
        }

        @Override
        public ItemVariant getResource(int index) {
            return delegateStorage.getResource(index);
        }

        @Override
        public long getAmountAsLong(int index) {
            return delegateStorage.getAmountAsLong(index);
        }

        @Override
        public long getCapacityAsLong(int index, ItemVariant resource) {
            return delegateStorage.getCapacityAsLong(index, resource);
        }

        @Override
        public boolean isValid(int index, ItemVariant resource) {
            return delegateStorage.isValid(index, resource) && filter.allowInsert(delegate, index, resource.toStack());
        }

        @Override
        public long insert(int index, ItemVariant resource, long amount, TransactionContext transaction) {
            if (!filter.allowInsert(delegate, index, resource.toStack())) {
                return 0;
            }
            return delegateStorage.insert(index, resource, amount, transaction);
        }

        @Override
        public long extract(int index, ItemVariant resource, long amount, TransactionContext transaction) {
            if (!filter.allowExtract(delegate, index, Ints.saturatedCast(amount))) {
                return 0;
            }
            return delegateStorage.extract(index, resource, amount, transaction);
        }

        @Override
        public long insert(ItemVariant resource, long amount, TransactionContext transaction) {
            return insertStacking(resource, amount, transaction);
        }
    }
}
